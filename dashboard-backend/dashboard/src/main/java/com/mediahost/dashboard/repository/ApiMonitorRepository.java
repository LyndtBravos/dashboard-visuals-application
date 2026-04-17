package com.mediahost.dashboard.repository;

import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiMonitorRepository extends JpaRepository<ApiMonitor, Integer> {
    List<ApiMonitor> findByServiceTypeAndIsActiveTrue(ServiceType serviceType);
    List<ApiMonitor> findByIsActiveTrue();

    @Query("SELECT a FROM ApiMonitor a WHERE a.isActive = true AND " +
            "(a.lastCheckTime IS NULL OR a.lastCheckTime <= :nextCheckTime)")
    List<ApiMonitor> findDueForCheck(@Param("nextCheckTime") LocalDateTime nextCheckTime);

    @Query("SELECT a FROM ApiMonitor a WHERE a.isActive = true AND a.currentFailureCount > 0")
    List<ApiMonitor> findFailingMonitors();

    @Query("SELECT a FROM ApiMonitor a WHERE a.isActive = true AND a.currentFailureCount >= a.retryCount")
    List<ApiMonitor> findAlertingMonitors();
}