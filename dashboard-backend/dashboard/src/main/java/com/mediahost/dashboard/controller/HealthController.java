package com.mediahost.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Dashboard API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> dbHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());

        try{
            Integer result =  jdbcTemplate.queryForObject("select 1;", Integer.class);

            if(result != null && result == 1){
                response.put("status", "OK");
                response.put("database", "Connected to database");
                response.put("query_result", "SELECT 1 returned " + result);
            }else {
                response.put("status", "DEGRADED");
                response.put("database", "Responding but unexpected result");
            }
        }catch (Exception e){
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("error_class", e.getClass().getSimpleName());
            response.put("database", "Disconnected");
        }

        return ResponseEntity.ok(response);
    }
}