package com.smartparking.repository;

import com.smartparking.util.DatabaseUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of ReportRepository executing reporting queries with PreparedStatement.
 */
@Repository("reportRepository")
public class ReportRepositoryImpl implements ReportRepository {

    @Override
    public Map<String, Object> getOccupancySummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        String sql = "SELECT COUNT(*) AS total_slots, " +
                "SUM(CASE WHEN is_available = FALSE THEN 1 ELSE 0 END) AS occupied_slots, " +
                "SUM(CASE WHEN is_available = TRUE THEN 1 ELSE 0 END) AS free_slots " +
                "FROM parking_slots";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int total = rs.getInt("total_slots");
                int occupied = rs.getInt("occupied_slots");
                int free = rs.getInt("free_slots");
                double pct = total > 0 ? ((double) occupied / total) * 100.0 : 0.0;

                result.put("totalSlots", total);
                result.put("occupiedSlots", occupied);
                result.put("freeSlots", free);
                result.put("occupancyPercentage", Math.round(pct * 100.0) / 100.0);
            }
        } catch (SQLException e) {
            System.err.println("[ReportRepository] Error fetching occupancy summary: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getFreeSlotsByFloorAndType() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT f.floor_number, f.floor_name, s.slot_type, " +
                "COUNT(s.slot_id) AS total_slots, " +
                "SUM(CASE WHEN s.is_available = TRUE THEN 1 ELSE 0 END) AS available_slots, " +
                "SUM(CASE WHEN s.is_available = FALSE THEN 1 ELSE 0 END) AS occupied_slots " +
                "FROM parking_floors f " +
                "JOIN parking_slots s ON f.floor_number = s.floor_number " +
                "GROUP BY f.floor_number, f.floor_name, s.slot_type " +
                "ORDER BY f.floor_number ASC, s.slot_type ASC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("floorNumber", rs.getInt("floor_number"));
                row.put("floorName", rs.getString("floor_name"));
                row.put("slotType", rs.getString("slot_type"));
                row.put("totalSlots", rs.getInt("total_slots"));
                row.put("availableSlots", rs.getInt("available_slots"));
                row.put("occupiedSlots", rs.getInt("occupied_slots"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ReportRepository] Error fetching free slots: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getVehicleTypeUtilization() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT v.vehicle_type, " +
                "COUNT(t.ticket_id) AS total_trips, " +
                "SUM(CASE WHEN t.status = 'ACTIVE' THEN 1 ELSE 0 END) AS currently_parked, " +
                "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_parkings, " +
                "COALESCE(SUM(t.total_amount), 0.00) AS total_revenue " +
                "FROM vehicles v " +
                "LEFT JOIN parking_tickets t ON v.license_plate = t.license_plate " +
                "GROUP BY v.vehicle_type " +
                "ORDER BY total_trips DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("vehicleType", rs.getString("vehicle_type"));
                row.put("totalTrips", rs.getInt("total_trips"));
                row.put("currentlyParked", rs.getInt("currently_parked"));
                row.put("completedParkings", rs.getInt("completed_parkings"));
                row.put("totalRevenue", rs.getDouble("total_revenue"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ReportRepository] Error fetching vehicle utilization: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Map<String, Object> getRevenueAnalytics() {
        Map<String, Object> result = new LinkedHashMap<>();
        String sql = "SELECT COUNT(payment_id) AS total_tx, " +
                "COALESCE(SUM(amount), 0.00) AS total_revenue, " +
                "COALESCE(AVG(amount), 0.00) AS avg_fare, " +
                "COALESCE(MAX(amount), 0.00) AS max_fare " +
                "FROM payments WHERE payment_status = 'SUCCESS'";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                result.put("totalTransactions", rs.getInt("total_tx"));
                result.put("totalRevenue", rs.getDouble("total_revenue"));
                result.put("averageFare", Math.round(rs.getDouble("avg_fare") * 100.0) / 100.0);
                result.put("highestFare", rs.getDouble("max_fare"));
            }
        } catch (SQLException e) {
            System.err.println("[ReportRepository] Error fetching revenue analytics: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPaymentModeBreakdown() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT payment_mode, COUNT(*) AS count, SUM(amount) AS total_mode_revenue " +
                "FROM payments WHERE payment_status = 'SUCCESS' " +
                "GROUP BY payment_mode ORDER BY total_mode_revenue DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("paymentMode", rs.getString("payment_mode"));
                row.put("count", rs.getInt("count"));
                row.put("totalRevenue", rs.getDouble("total_mode_revenue"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ReportRepository] Error fetching payment modes: " + e.getMessage());
        }
        return list;
    }
}
