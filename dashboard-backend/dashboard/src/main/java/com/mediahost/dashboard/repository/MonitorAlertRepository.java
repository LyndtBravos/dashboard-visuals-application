package com.mediahost.dashboard.repository;

import com.mediahost.dashboard.model.entity.MonitorAlert;
import com.mediahost.dashboard.model.enums.AlertStatus;
import com.mediahost.dashboard.model.enums.MonitorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorAlertRepository extends JpaRepository<MonitorAlert, Integer> {
    List<MonitorAlert> findByCurrentStatusAndAcknowledgedFalse(AlertStatus status);
    Optional<MonitorAlert> findByMonitorTypeAndMonitorIdAndCurrentStatus(
            MonitorType monitorType, Integer monitorId, AlertStatus status);

    List<MonitorAlert> findByMonitorTypeAndMonitorIdOrderByStartedAtDesc(
            MonitorType monitorType, Integer monitorId);

    @Query("SELECT a FROM MonitorAlert a WHERE a.id = :id")
    Optional<MonitorAlert> findByIdWithDetails(@Param("id") Integer id);

    List<MonitorAlert> findByAcknowledgedFalse();
    List<MonitorAlert> findByAcknowledged(boolean acknowledged);
    List<MonitorAlert> findByCurrentStatus(AlertStatus status);
}