package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.entity.MonitorHistory;
import com.mediahost.dashboard.model.enums.MonitorType;
import com.mediahost.dashboard.repository.MonitorHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/monitors/history")
public class MonitorHistoryController {

    @Autowired
    private MonitorHistoryRepository historyRepository;

    @GetMapping("/{type}/{monitorId}")
    public ResponseEntity<List<MonitorHistory>> getHistory(
            @PathVariable MonitorType type,
            @PathVariable Integer monitorId,
            @RequestParam(defaultValue = "50") int limit) {

        List<MonitorHistory> history = historyRepository
                .findByMonitorTypeAndMonitorIdOrderByCheckTimeDesc(
                        type, monitorId, PageRequest.of(0, limit, Sort.by("checkTime").descending())
                );
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldHistory(
            @RequestParam(defaultValue = "90") int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        historyRepository.deleteByCheckTimeBefore(cutoff);
        return ResponseEntity.ok().build();
    }
}