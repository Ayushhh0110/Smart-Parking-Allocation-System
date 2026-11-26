package com.smartparking.model;

import java.time.LocalDateTime;

/**
 * Model representing a financial payment made upon exiting the parking facility.
 */
public class Payment {
    private long paymentId;
    private long ticketId;
    private double amount;
    private LocalDateTime paymentTime;
    private String paymentMode;   // 'CASH', 'CARD', 'UPI'
    private String paymentStatus; // 'SUCCESS', 'FAILED'

    public Payment() {
        this.paymentTime = LocalDateTime.now();
        this.paymentStatus = "SUCCESS";
    }

    public Payment(long ticketId, double amount, String paymentMode, String paymentStatus) {
        this.ticketId = ticketId;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.paymentStatus = paymentStatus;
        this.paymentTime = LocalDateTime.now();
    }

    public long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(long paymentId) {
        this.paymentId = paymentId;
    }

    public long getTicketId() {
        return ticketId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String toString() {
        return String.format("[Payment ID: %d | Ticket ID: %d | Amount: ₹%.2f | Mode: %s | Status: %s]",
                paymentId, ticketId, amount, paymentMode, paymentStatus);
    }
}
