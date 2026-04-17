package com.mediahost.dashboard.service.checker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.enums.MonitorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
public class ApiChecker implements HttpChecker {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate createRestTemplate(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return new RestTemplate(factory);
    }

    private HttpHeaders parseHeaders(String headersJson) {
        HttpHeaders headers = new HttpHeaders();
        if (headersJson != null && !headersJson.isEmpty()) {
            try {
                JsonNode headersNode = objectMapper.readTree(headersJson);

                Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    headers.add(entry.getKey(), entry.getValue().asText());
                }
            } catch (Exception e) {
                log.error("Failed to parse headers JSON: {}", e.getMessage());
            }
        }
        return headers;
    }

    @Override
    public CheckResult check(String url, int timeoutSeconds, boolean followRedirects) {
        return CheckResult.builder()
                .status(MonitorStatus.error)
                .errorMessage("Direct check not supported for APIs. Use monitor-specific check.")
                .build();
    }

    public CheckResult check(ApiMonitor monitor) {
        long startTime = System.currentTimeMillis();
        RestTemplate restTemplate = createRestTemplate(monitor.getTimeoutSeconds());

        try {
            HttpHeaders headers = parseHeaders(monitor.getRequestHeadersJson());
            if (monitor.getRequestContentType() != null) {
                headers.setContentType(MediaType.parseMediaType(monitor.getRequestContentType()));
            }

            HttpEntity<String> entity = new HttpEntity<>(monitor.getRequestBody(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    monitor.getUrl(),
                    HttpMethod.valueOf(monitor.getMethod().name()),
                    entity,
                    String.class
            );

            long responseTime = System.currentTimeMillis() - startTime;

            return CheckResult.builder()
                    .status(MonitorStatus.success)
                    .responseTimeMs((int) responseTime)
                    .statusCode(response.getStatusCode().value())
                    .responsePreview(response.getBody() != null ?
                            response.getBody().substring(0, Math.min(response.getBody().length(), 500)) : null)
                    .responseBody(response.getBody())
                    .responseSizeBytes(response.getBody() != null ? response.getBody().length() : 0)
                    .build();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return handleHttpError(e, startTime);
        } catch (ResourceAccessException e) {
            return handleResourceError(e, startTime, monitor.getTimeoutSeconds(), monitor.getUrl());
        } catch (Exception e) {
            return handleUnexpectedError(e, startTime);
        }
    }

    private CheckResult handleHttpError(Exception e, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        int statusCode = 0;
        String responseBody = null;

        if (e instanceof HttpClientErrorException) {
            statusCode = ((HttpClientErrorException) e).getStatusCode().value();
            responseBody = ((HttpClientErrorException) e).getResponseBodyAsString();
        } else if (e instanceof HttpServerErrorException) {
            statusCode = ((HttpServerErrorException) e).getStatusCode().value();
            responseBody = ((HttpServerErrorException) e).getResponseBodyAsString();
        }

        return CheckResult.builder()
                .status(MonitorStatus.failed)
                .responseTimeMs((int) responseTime)
                .statusCode(statusCode)
                .responsePreview(responseBody != null ?
                        responseBody.substring(0, Math.min(responseBody.length(), 200)) : null)
                .errorMessage("HTTP " + statusCode + ": " + e.getMessage())
                .build();
    }

    private CheckResult handleResourceError(ResourceAccessException e, long startTime,
                                            int timeoutSeconds, String url) {
        long responseTime = System.currentTimeMillis() - startTime;
        String errorMsg = e.getCause() instanceof SocketTimeoutException ?
                "Connection timeout after " + timeoutSeconds + " seconds" :
                e.getCause() instanceof UnknownHostException ?
                        "Unknown host: " + url :
                        "Connection error: " + e.getMessage();

        return CheckResult.builder()
                .status(MonitorStatus.error)
                .responseTimeMs((int) responseTime)
                .errorMessage(errorMsg)
                .build();
    }

    private CheckResult handleUnexpectedError(Exception e, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        return CheckResult.builder()
                .status(MonitorStatus.error)
                .responseTimeMs((int) responseTime)
                .errorMessage("Unexpected error: " + e.getMessage())
                .build();
    }

    public boolean validateResponse(CheckResult result, ApiMonitor monitor) {
        // Check status code
        if (!monitor.getExpectedStatusCode().equals(result.getStatusCode())) {
            result.setErrorMessage(String.format("Expected status %d but got %d",
                    monitor.getExpectedStatusCode(), result.getStatusCode()));
            return false;
        }

        // Check response time
        if (monitor.getExpectedResponseTimeMs() != null &&
                result.getResponseTimeMs() > monitor.getExpectedResponseTimeMs()) {
            result.setErrorMessage(String.format("Response time %dms exceeds threshold %dms",
                    result.getResponseTimeMs(), monitor.getExpectedResponseTimeMs()));
            return false;
        }

        if (monitor.getExpectedResponseSizeBytes() != null &&
                result.getResponseSizeBytes() > monitor.getExpectedResponseSizeBytes()) {
            result.setErrorMessage(String.format("Response size %d bytes exceeds threshold %d bytes",
                    result.getResponseSizeBytes(), monitor.getExpectedResponseSizeBytes()));
            return false;
        }

        if (monitor.getExpectedResponseContains() != null &&
                !monitor.getExpectedResponseContains().isEmpty() &&
                result.getResponseBody() != null &&
                !result.getResponseBody().contains(monitor.getExpectedResponseContains())) {
            result.setErrorMessage("Response does not contain expected text");
            return false;
        }

        if (monitor.getExpectedJsonPath() != null &&
                !monitor.getExpectedJsonPath().isEmpty() &&
                result.getResponseBody() != null) {
            try {
                Object value = JsonPath.read(result.getResponseBody(), monitor.getExpectedJsonPath());
                if (monitor.getExpectedValue() != null &&
                        !monitor.getExpectedValue().isEmpty() &&
                        !monitor.getExpectedValue().equals(value.toString())) {
                    result.setErrorMessage(String.format("JSON path '%s' returned '%s' but expected '%s'",
                            monitor.getExpectedJsonPath(), value, monitor.getExpectedValue()));
                    return false;
                }
            } catch (PathNotFoundException e) {
                result.setErrorMessage("JSON path not found: " + monitor.getExpectedJsonPath());
                return false;
            }
        }

        return true;
    }
}