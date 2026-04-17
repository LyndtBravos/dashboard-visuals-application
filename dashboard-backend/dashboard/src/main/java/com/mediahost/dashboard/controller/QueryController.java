package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.request.ExecuteQueryRequest;
import com.mediahost.dashboard.model.dto.response.QueryDataSetResponse;
import com.mediahost.dashboard.model.dto.response.QueryResultResponse;
import com.mediahost.dashboard.model.enums.StatusColor;
import com.mediahost.dashboard.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/execute")
    public ResponseEntity<QueryResultResponse> executeQuery(@Valid @RequestBody ExecuteQueryRequest request) {
        Object value = queryService.executeQuery(request.getQuery());
        StatusColor status = queryService.evaluateThreshold(
                value,
                request.getWarningThreshold(),
                request.getDangerThreshold()
        );

        QueryResultResponse response = QueryResultResponse.builder()
                .value(value)
                .status(status)
                .warningThreshold(request.getWarningThreshold())
                .dangerThreshold(request.getDangerThreshold())
                .query(request.getQuery())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<QueryResultResponse>> executeBatch(@Valid @RequestBody List<ExecuteQueryRequest> requests) {
        List<QueryResultResponse> results = new ArrayList<>();

        for (ExecuteQueryRequest request : requests) {
            try {
                Object value = queryService.executeQuery(request.getQuery());
                StatusColor status = queryService.evaluateThreshold(
                        value,
                        request.getWarningThreshold(),
                        request.getDangerThreshold()
                );

                results.add(QueryResultResponse.builder()
                        .value(value)
                        .status(status)
                        .warningThreshold(request.getWarningThreshold())
                        .dangerThreshold(request.getDangerThreshold())
                        .query(request.getQuery())
                        .timestamp(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                results.add(QueryResultResponse.builder()
                        .value(null)
                        .status(StatusColor.red)
                        .query(request.getQuery())
                        .timestamp(LocalDateTime.now())
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return ResponseEntity.ok(results);
    }

    @PostMapping("/execute-dataset")
    public ResponseEntity<QueryDataSetResponse> executeQueryDataset(@Valid @RequestBody ExecuteQueryRequest request) {

        Map<String, Object> result = queryService.executeQueryWithMetadata(request.getQuery());

        QueryDataSetResponse response = QueryDataSetResponse.builder()
                .success((Boolean) result.get("success"))
                .data((List<Map<String, Object>>) result.get("data"))
                .columns((List<String>) result.get("columns"))
                .rowCount((Integer) result.get("rowCount"))
                .query(request.getQuery())
                .timestamp(LocalDateTime.now())
                .build();

        if (result.containsKey("error"))
            response.setErrorMessage((String) result.get("error"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recheck")
    public ResponseEntity<QueryDataSetResponse> executeQueryDataset(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        Map<String, Object> result = queryService.executeQueryWithMetadata(query);

        QueryDataSetResponse response = QueryDataSetResponse.builder()
                .success((Boolean) result.get("success"))
                .data((List<Map<String, Object>>) result.get("data"))
                .columns((List<String>) result.get("columns"))
                .rowCount((Integer) result.get("rowCount"))
                .query(query)
                .timestamp(LocalDateTime.now())
                .build();

        if (result.containsKey("error"))
            response.setErrorMessage((String) result.get("error"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute-with-metadata")
    public ResponseEntity<Map<String, Object>> executeWithMetadata(@Valid @RequestBody ExecuteQueryRequest request) {
        Map<String, Object> result = queryService.executeQueryWithMetadata(request.getQuery());
        return ResponseEntity.ok(result);
    }
}