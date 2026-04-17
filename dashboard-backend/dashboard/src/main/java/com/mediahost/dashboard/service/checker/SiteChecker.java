package com.mediahost.dashboard.service.checker;

import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.MonitorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.*;
import java.io.IOException;

@Service
@Slf4j
public class SiteChecker implements HttpChecker {

    private RestTemplate createRestTemplate(int timeoutSeconds, boolean followRedirects) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);

            HostnameVerifier verifier = (hostname, session) -> {
                log.debug("Verifying hostname: {}", hostname);
                return true;
            };

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(verifier);

        } catch (Exception e) {
            log.error("Failed to configure SSL", e);
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(followRedirects);
            }
        };
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return new RestTemplate(factory);
    }

    @Override
    public CheckResult check(String url, int timeoutSeconds, boolean followRedirects) {
        long startTime = System.currentTimeMillis();
        RestTemplate restTemplate = createRestTemplate(timeoutSeconds, followRedirects);

        try {
            // Execute HEAD request first to check availability
            restTemplate.headForHeaders(url);

            long responseTime = System.currentTimeMillis() - startTime;

            String responseBody = restTemplate.getForObject(url, String.class);

            return CheckResult.builder()
                    .status(MonitorStatus.success)
                    .responseTimeMs((int) responseTime)
                    .statusCode(200)
                    .responsePreview(responseBody != null ?
                            responseBody.substring(0, Math.min(responseBody.length(), 500)) : null)
                    .responseBody(responseBody)
                    .build();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return handleHttpError(e, startTime);
        } catch (ResourceAccessException e) {
            return handleResourceError(e, startTime, timeoutSeconds, url);
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

    public boolean validateContent(CheckResult result, SiteMonitor monitor) {
        if (monitor.getExpectedPhrase() == null || monitor.getExpectedPhrase().isEmpty())
            return true;


        String responseBody = result.getResponseBody();
        if (responseBody == null) {
            result.setErrorMessage("No response body received");
            return false;
        }

        boolean phraseFound = responseBody.contains(monitor.getExpectedPhrase());
        boolean shouldBeMissing = Boolean.TRUE.equals(monitor.getExpectedPhraseMissing());

        if (shouldBeMissing && phraseFound) {
            result.setErrorMessage("Found unexpected phrase: '" + monitor.getExpectedPhrase() + "'");
            return false;
        } else if (!shouldBeMissing && !phraseFound) {
            result.setErrorMessage("Expected phrase not found: '" + monitor.getExpectedPhrase() + "'");
            return false;
        }

        return true;
    }
}