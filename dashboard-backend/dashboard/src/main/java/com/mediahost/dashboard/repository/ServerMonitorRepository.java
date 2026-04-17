package com.mediahost.dashboard.repository;

import com.mediahost.dashboard.model.entity.ServerMonitor;
import com.mediahost.dashboard.model.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServerMonitorRepository extends JpaRepository<ServerMonitor, Integer> {

    List<ServerMonitor> findByServiceTypeAndIsActiveTrue(ServiceType serviceType);

    List<ServerMonitor> findByIsActiveTrue();

    @Query("SELECT s FROM ServerMonitor s WHERE s.isActive = true AND " +
            "(s.lastCheckTime IS NULL OR s.lastCheckTime <= :nextCheckTime)")
    List<ServerMonitor> findDueForCheck(@Param("nextCheckTime") LocalDateTime nextCheckTime);

    @Query("SELECT s FROM ServerMonitor s WHERE s.isActive = true AND s.currentFailureCount > 0")
    List<ServerMonitor> findFailingMonitors();

    @Query("SELECT s FROM ServerMonitor s WHERE s.isActive = true AND s.currentFailureCount >= s.retryCount")
    List<ServerMonitor> findAlertingMonitors();

    @Query("SELECT COUNT(s) FROM ServerMonitor s WHERE s.isActive = true AND s.currentFailureCount = 0")
    long countHealthy();

    @Query("SELECT COUNT(s) FROM ServerMonitor s WHERE s.isActive = true AND s.currentFailureCount > 0")
    long countFailing();
}