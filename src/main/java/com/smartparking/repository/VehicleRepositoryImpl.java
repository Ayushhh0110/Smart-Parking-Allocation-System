package com.smartparking.repository;

import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.util.DatabaseUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * JDBC implementation for Vehicle persistence using PreparedStatement.
 */
@Repository("vehicleRepository")
public class VehicleRepositoryImpl implements VehicleRepository {

    @Override
    public void saveOrUpdate(Vehicle vehicle) {
        String checkSql = "SELECT COUNT(*) FROM vehicles WHERE license_plate = ?";
        String insertSql = "INSERT INTO vehicles (license_plate, vehicle_type, owner_name, contact_number, registered_at) VALUES (?, ?, ?, ?, ?)";
        String updateSql = "UPDATE vehicles SET vehicle_type = ?, owner_name = ?, contact_number = ? WHERE license_plate = ?";

        try (Connection conn = DatabaseUtil.getConnection()) {
            boolean exists = false;
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setString(1, vehicle.getLicensePlate());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        exists = true;
                    }
                }
            }

            if (!exists) {
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setString(1, vehicle.getLicensePlate());
                    psInsert.setString(2, vehicle.getVehicleType().name());
                    psInsert.setString(3, vehicle.getOwnerName());
                    psInsert.setString(4, vehicle.getContactNumber());
                    psInsert.setTimestamp(5, Timestamp.valueOf(vehicle.getRegisteredAt()));
                    psInsert.executeUpdate();
                }
            } else {
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, vehicle.getVehicleType().name());
                    psUpdate.setString(2, vehicle.getOwnerName());
                    psUpdate.setString(3, vehicle.getContactNumber());
                    psUpdate.setString(4, vehicle.getLicensePlate());
                    psUpdate.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[VehicleRepository] Error saving vehicle: " + e.getMessage());
        }
    }

    @Override
    public Optional<Vehicle> findByLicensePlate(String licensePlate) {
        String sql = "SELECT license_plate, vehicle_type, owner_name, contact_number, registered_at FROM vehicles WHERE license_plate = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, licensePlate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Vehicle v = new Vehicle();
                    v.setLicensePlate(rs.getString("license_plate"));
                    v.setVehicleType(VehicleType.valueOf(rs.getString("vehicle_type")));
                    v.setOwnerName(rs.getString("owner_name"));
                    v.setContactNumber(rs.getString("contact_number"));
                    v.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
                    return Optional.of(v);
                }
            }
        } catch (SQLException e) {
            System.err.println("[VehicleRepository] Error finding vehicle: " + e.getMessage());
        }
        return Optional.empty();
    }
}
