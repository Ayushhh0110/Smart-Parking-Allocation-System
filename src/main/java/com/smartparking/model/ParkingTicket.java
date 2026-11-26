package com.smartparking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model representing an issued parking ticket.
 */
public class ParkingTicket {
    private long ticketId;
    private String ticketNumber;
    private Vehicle vehicle;
    private ParkingSlot slot;
    private String entryGate;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status;
    private double totalAmount;
    private String billingPlan;

    public ParkingTicket() {
        this.status = TicketStatus.ACTIVE;
        this.entryTime = LocalDateTime.now();
    }

    public ParkingTicket(String ticketNumber, Vehicle vehicle, ParkingSlot slot, String entryGate, String billingPlan) {
        this.ticketNumber = ticketNumber;
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryGate = entryGate;
        this.billingPlan = billingPlan;
        this.entryTime = LocalDateTime.now();
        this.status = TicketStatus.ACTIVE;
        this.totalAmount = 0.0;
    }

    public long getTicketId() {
        return ticketId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ParkingSlot getSlot() {
        return slot;
    }

    public void setSlot(ParkingSlot slot) {
        this.slot = slot;
    }

    public String getEntryGate() {
        return entryGate;
    }

    public void setEntryGate(String entryGate) {
        this.entryGate = entryGate;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBillingPlan() {
        return billingPlan;
    }

    public void setBillingPlan(String billingPlan) {
        this.billingPlan = billingPlan;
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(
            "Ticket #%s [%s] | Gate: %s | Vehicle: %s (%s) | Slot: %s | In: %s | Out: %s | Fare: ₹%.2f",
            ticketNumber, status, entryGate,
            vehicle != null ? vehicle.getLicensePlate() : "N/A",
            vehicle != null ? vehicle.getVehicleType() : "N/A",
            slot != null ? slot.getSlotId() : "N/A",
            entryTime != null ? entryTime.format(fmt) : "N/A",
            exitTime != null ? exitTime.format(fmt) : "ACTIVE",
            totalAmount
        );
    }
}
