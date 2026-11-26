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
 * Repository interface and JDBC implementation for parking slot persistence.
 */
public interface ParkingSlotRepository {
    List<ParkingSlot> findAllSlots();
    void updateSlotAvailability(String slotId, boolean isAvailable);
}
