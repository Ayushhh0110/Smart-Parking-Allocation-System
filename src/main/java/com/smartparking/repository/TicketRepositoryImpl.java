package com.smartparking.repository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Payment;
import com.smartparking.model.SlotType;
import com.smartparking.model.TicketStatus;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.util.DatabaseUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of TicketRepository using PreparedStatement and Transaction management.
 */
@Repository("ticketRepository")
public class TicketRepositoryImpl implements TicketRepository {

    @Override
    public ParkingTicket save(ParkingTicket ticket) {
        String sql = "INSERT INTO parking_tickets " +
                "(ticket_number, license_plate, slot_id, entry_gate, entry_time, exit_time, status, total_amount, billing_plan) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, ticket.getTicketNumber());
            ps.setString(2, ticket.getVehicle() != null ? ticket.getVehicle().getLicensePlate() : null);
            ps.setString(3, ticket.getSlot() != null ? ticket.getSlot().getSlotId() : null);
            ps.setString(4, ticket.getEntryGate());
            ps.setTimestamp(5, Timestamp.valueOf(ticket.getEntryTime()));
            ps.setTimestamp(6, ticket.getExitTime() != null ? Timestamp.valueOf(ticket.getExitTime()) : null);
            ps.setString(7, ticket.getStatus().name());
            ps.setDouble(8, ticket.getTotalAmount());
            ps.setString(9, ticket.getBillingPlan());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        ticket.setTicketId(rs.getLong(1));
                    }
                }
            }
            return ticket;
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Error saving ticket: " + e.getMessage());
            throw new RuntimeException("Database error saving parking ticket", e);
        }
    }

    @Override
    public Optional<ParkingTicket> findByTicketNumber(String ticketNumber) {
        String sql = "SELECT t.*, v.vehicle_type, v.owner_name, v.contact_number, " +
                "s.floor_number, s.slot_number, s.slot_type, s.is_available, s.proximity_score " +
                "FROM parking_tickets t " +
                "JOIN vehicles v ON t.license_plate = v.license_plate " +
                "JOIN parking_slots s ON t.slot_id = s.slot_id " +
                "WHERE t.ticket_number = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ticketNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Error finding ticket by number: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<ParkingTicket> findActiveByLicensePlate(String licensePlate) {
        String sql = "SELECT t.*, v.vehicle_type, v.owner_name, v.contact_number, " +
                "s.floor_number, s.slot_number, s.slot_type, s.is_available, s.proximity_score " +
                "FROM parking_tickets t " +
                "JOIN vehicles v ON t.license_plate = v.license_plate " +
                "JOIN parking_slots s ON t.slot_id = s.slot_id " +
                "WHERE t.license_plate = ? AND t.status = 'ACTIVE' ORDER BY t.entry_time DESC LIMIT 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, licensePlate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTicket(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Error finding active ticket for vehicle: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<ParkingTicket> findAllActiveTickets() {
        List<ParkingTicket> list = new ArrayList<>();
        String sql = "SELECT t.*, v.vehicle_type, v.owner_name, v.contact_number, " +
                "s.floor_number, s.slot_number, s.slot_type, s.is_available, s.proximity_score " +
                "FROM parking_tickets t " +
                "JOIN vehicles v ON t.license_plate = v.license_plate " +
                "JOIN parking_slots s ON t.slot_id = s.slot_id " +
                "WHERE t.status = 'ACTIVE' ORDER BY t.entry_time ASC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToTicket(rs));
            }
        } catch (SQLException e) {
            System.err.println("[TicketRepository] Error fetching active tickets: " + e.getMessage());
        }
        return list;
    }

    /**
     * ATOMIC DATABASE TRANSACTION:
     * 1. Update parking_tickets (status='COMPLETED', exit_time, total_amount)
     * 2. Insert payments record (ticket_id, amount, payment_time, mode, status)
     * 3. Update parking_slots (is_available = TRUE)
     * If any operation fails, the transaction is rolled back completely.
     */
    @Override
    public boolean completeTicketAndRecordPaymentTransactional(ParkingTicket ticket, Payment payment) {
        String updateTicketSql = "UPDATE parking_tickets SET exit_time = ?, status = ?, total_amount = ? WHERE ticket_id = ?";
        String insertPaymentSql = "INSERT INTO payments (ticket_id, amount, payment_time, payment_mode, payment_status) VALUES (?, ?, ?, ?, ?)";
        String releaseSlotSql = "UPDATE parking_slots SET is_available = TRUE WHERE slot_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Update ticket
            try (PreparedStatement psTicket = conn.prepareStatement(updateTicketSql)) {
                psTicket.setTimestamp(1, Timestamp.valueOf(ticket.getExitTime()));
                psTicket.setString(2, TicketStatus.COMPLETED.name());
                psTicket.setDouble(3, ticket.getTotalAmount());
                psTicket.setLong(4, ticket.getTicketId());
                int rows = psTicket.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Failed to update ticket during checkout.");
                }
            }

            // 2. Insert Payment
            try (PreparedStatement psPayment = conn.prepareStatement(insertPaymentSql, Statement.RETURN_GENERATED_KEYS)) {
                psPayment.setLong(1, ticket.getTicketId());
                psPayment.setDouble(2, payment.getAmount());
                psPayment.setTimestamp(3, Timestamp.valueOf(payment.getPaymentTime()));
                psPayment.setString(4, payment.getPaymentMode());
                psPayment.setString(5, payment.getPaymentStatus());
                psPayment.executeUpdate();

                try (ResultSet rs = psPayment.getGeneratedKeys()) {
                    if (rs.next()) {
                        payment.setPaymentId(rs.getLong(1));
                    }
                }
            }

            // 3. Release Slot in DB
            try (PreparedStatement psSlot = conn.prepareStatement(releaseSlotSql)) {
                psSlot.setString(1, ticket.getSlot().getSlotId());
                psSlot.executeUpdate();
            }

            conn.commit(); // Commit Transaction
            System.out.println("[TicketRepository] Transaction Committed: Ticket #" + ticket.getTicketNumber() +
                    " settled with Payment ID " + payment.getPaymentId() + " (Slot " + ticket.getSlot().getSlotId() + " released).");
            return true;

        } catch (SQLException e) {
            System.err.println("[TicketRepository] Transaction Failed! Rolling back changes: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on failure
                } catch (SQLException ex) {
                    System.err.println("[TicketRepository] Error during rollback: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    private ParkingTicket mapResultSetToTicket(ResultSet rs) throws SQLException {
        ParkingTicket t = new ParkingTicket();
        t.setTicketId(rs.getLong("ticket_id"));
        t.setTicketNumber(rs.getString("ticket_number"));
        t.setEntryGate(rs.getString("entry_gate"));
        t.setEntryTime(rs.getTimestamp("entry_time").toLocalDateTime());
        Timestamp exitTs = rs.getTimestamp("exit_time");
        if (exitTs != null) {
            t.setExitTime(exitTs.toLocalDateTime());
        }
        t.setStatus(TicketStatus.valueOf(rs.getString("status")));
        t.setTotalAmount(rs.getDouble("total_amount"));
        t.setBillingPlan(rs.getString("billing_plan"));

        // Vehicle mapping
        Vehicle v = new Vehicle();
        v.setLicensePlate(rs.getString("license_plate"));
        v.setVehicleType(VehicleType.valueOf(rs.getString("vehicle_type")));
        v.setOwnerName(rs.getString("owner_name"));
        v.setContactNumber(rs.getString("contact_number"));
        t.setVehicle(v);

        // Slot mapping
        ParkingSlot s = new ParkingSlot();
        s.setSlotId(rs.getString("slot_id"));
        s.setFloorNumber(rs.getInt("floor_number"));
        s.setSlotNumber(rs.getInt("slot_number"));
        s.setSlotType(SlotType.valueOf(rs.getString("slot_type")));
        s.setAvailable(rs.getBoolean("is_available"));
        s.setProximityScore(rs.getInt("proximity_score"));
        t.setSlot(s);

        return t;
    }
}
