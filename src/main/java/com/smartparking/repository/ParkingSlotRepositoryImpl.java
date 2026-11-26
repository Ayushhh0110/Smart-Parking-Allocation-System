package com.smartparking.repository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotType;
import com.smartparking.util.DatabaseUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation for ParkingSlotRepository using PreparedStatement.
 */
@Repository("parkingSlotRepository")
public class ParkingSlotRepositoryImpl implements ParkingSlotRepository {

    @Override
    public List<ParkingSlot> findAllSlots() {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT slot_id, floor_number, slot_number, slot_type, is_available, proximity_score " +
                     "FROM parking_slots ORDER BY floor_number ASC, slot_number ASC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ParkingSlot slot = new ParkingSlot();
                slot.setSlotId(rs.getString("slot_id"));
                slot.setFloorNumber(rs.getInt("floor_number"));
                slot.setSlotNumber(rs.getInt("slot_number"));
                slot.setSlotType(SlotType.valueOf(rs.getString("slot_type")));
                slot.setAvailable(rs.getBoolean("is_available"));
                slot.setProximityScore(rs.getInt("proximity_score"));
                list.add(slot);
            }
        } catch (SQLException e) {
            System.err.println("[ParkingSlotRepository] Error loading slots: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void updateSlotAvailability(String slotId, boolean isAvailable) {
        String sql = "UPDATE parking_slots SET is_available = ? WHERE slot_id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isAvailable);
            ps.setString(2, slotId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ParkingSlotRepository] Error updating slot availability: " + e.getMessage());
        }
    }
}
