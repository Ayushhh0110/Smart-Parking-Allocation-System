package com.smartparking.repository;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for analytical and reporting queries via JDBC.
 * Problem Statement 9 - Requirement 6.
 */
public interface ReportRepository {

    Map<String, Object> getOccupancySummary();

    List<Map<String, Object>> getFreeSlotsByFloorAndType();

    List<Map<String, Object>> getVehicleTypeUtilization();

    Map<String, Object> getRevenueAnalytics();

    List<Map<String, Object>> getPaymentModeBreakdown();
}
