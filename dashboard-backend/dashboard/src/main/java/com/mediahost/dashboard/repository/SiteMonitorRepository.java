package com.mediahost.dashboard.repository;

import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SiteMonitorRepository extends JpaRepository<SiteMonitor, Integer> {

    List<SiteMonitor> findByServiceTypeAndIsActiveTrue(ServiceType serviceType);
    List<SiteMonitor> findByIsActiveTrue();

    @Query("SELECT s FROM SiteMonitor s WHERE s.isActive = true AND " +
            "(s.lastCheckTime IS NULL OR s.lastCheckTime <= :nextCheckTime)")
    List<SiteMonitor> findDueForCheck(@Param("nextCheckTime") LocalDateTime nextCheckTime);

    @Query("SELECT s FROM SiteMonitor s WHERE s.isActive = true AND s.currentFailureCount > 0")
    List<SiteMonitor> findFailingMonitors();

    @Query("SELECT s FROM SiteMonitor s WHERE s.isActive = true AND s.currentFailureCount >= s.retryCount")
    List<SiteMonitor> findAlertingMonitors();

    @Query("SELECT COUNT(s) FROM SiteMonitor s WHERE s.isActive = true AND s.currentFailureCount = 0")
    long countHealthy();

    @Query("SELECT COUNT(s) FROM SiteMonitor s WHERE s.isActive = true AND s.currentFailureCount > 0")
    long countFailing();
}