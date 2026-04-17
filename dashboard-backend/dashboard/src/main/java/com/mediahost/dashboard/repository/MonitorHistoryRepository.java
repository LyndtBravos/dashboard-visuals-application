package com.mediahost.dashboard.repository;

import com.mediahost.dashboard.model.entity.MonitorHistory;
import com.mediahost.dashboard.model.enums.MonitorType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitorHistoryRepository extends JpaRepository<MonitorHistory, Integer> {
    List<MonitorHistory> findByMonitorTypeAndMonitorIdOrderByCheckTimeDesc(
            MonitorType monitorType, Integer monitorId);

    List<MonitorHistory> findByMonitorTypeAndMonitorIdOrderByCheckTimeDesc(
            MonitorType type, Integer monitorId, Pageable pageable);
    int deleteByCheckTimeBefore(LocalDateTime cutoff);
    List<MonitorHistory> findByAlertCreatedFalseAndCheckTimeBefore(LocalDateTime cutoff);
}