package com.mediahost.dashboard.repository;

import com.mediahost.dashboard.model.entity.DashboardConfig;
import com.mediahost.dashboard.model.enums.ServiceType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<DashboardConfig, Integer> {

    @Query("SELECT d FROM DashboardConfig d WHERE d.serviceType = :serviceType AND d.isActive = true ORDER BY CASE WHEN d.flowOrder IS NULL THEN 1 ELSE 0 END, d.flowOrder ASC, d.name ASC")
    List<DashboardConfig> findByServiceTypeOrdered(@Param("serviceType") ServiceType serviceType);

    List<DashboardConfig> findByServiceTypeAndIsActiveTrue(ServiceType serviceType);

    List<DashboardConfig> findByIsActiveTrue();

    @Query("SELECT d FROM DashboardConfig d WHERE d.isActive = true AND " +
            "(:serviceType IS NULL OR d.serviceType = :serviceType)")
    List<DashboardConfig> findAllActive(@Param("serviceType") ServiceType serviceType);

    @Query("SELECT d.serviceType, COUNT(d) FROM DashboardConfig d WHERE d.isActive = true GROUP BY d.serviceType")
    List<Object[]> countByServiceType();
}