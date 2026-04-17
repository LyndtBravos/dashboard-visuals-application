package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.exception.QueryExecutionException;
import com.mediahost.dashboard.model.enums.StatusColor;
import com.mediahost.dashboard.service.QueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryServiceImpl implements QueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Object executeQuery(String query) {
        try {
            String cleanedQuery = sanitizeQuery(query);

            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(cleanedQuery);

            if (rowSet.next()) {
                Object value = rowSet.getObject(1);
                return convertValue(value);
            }

            return null;
        } catch (Exception e) {
            throw new QueryExecutionException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> executeQueryWithMetadata(String query) {
        Map<String, Object> result = new HashMap<>();

        try {
            String cleanedQuery = sanitizeQuery(query);
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(cleanedQuery);

            List<Map<String, Object>> dataList = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            int rowCount = 0;

            int columnCount = rowSet.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++)
                columns.add(rowSet.getMetaData().getColumnName(i));

            while (rowSet.next()) {
                rowCount++;
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rowSet.getMetaData().getColumnName(i);
                    row.put(columnName, convertValue(rowSet.getObject(i)));
                }
                dataList.add(row);
            }

            result.put("success", true);
            result.put("data", dataList);
            result.put("columns", columns);
            result.put("rowCount", rowCount);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new QueryExecutionException("Failed to execute query: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public StatusColor evaluateThreshold(Object value, Double warningThreshold, Double dangerThreshold) {
        if (warningThreshold == null && dangerThreshold == null)
            return StatusColor.green;

        Double numericValue = convertToDouble(value);

        if (numericValue == null) return StatusColor.green;
        if (dangerThreshold != null && numericValue >= dangerThreshold) return StatusColor.red;
        if (warningThreshold != null && numericValue >= warningThreshold) return StatusColor.yellow;

        return StatusColor.green;
    }

    private String sanitizeQuery(String query) {
        if (query == null || query.trim().isEmpty())
            throw new QueryExecutionException("Query cannot be empty");

        String trimmedQuery = query.trim().toUpperCase();

        if (!trimmedQuery.startsWith("SELECT"))
            throw new QueryExecutionException("Only SELECT queries are allowed");

        // "INSERT", "UPDATE"
        String[] blockedKeywords = {"DROP", "DELETE", "ALTER", "CREATE", "TRUNCATE", "GRANT", "REVOKE"};

        for (String keyword : blockedKeywords)
            if (trimmedQuery.contains(keyword))
                throw new QueryExecutionException("Query contains blocked keyword: " + keyword);

        return query;
    }

    private Object convertValue(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return ((BigDecimal) value).doubleValue();
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime();

        return value;
    }

    private Double convertToDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();

        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}