package com.smartparking.service;

import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Payment;
import com.smartparking.model.Vehicle;

import java.util.List;

/**
 * Main Business Service orchestrating Parking Operations.
 * Problem Statement 9 - Requirements 1-5.
 */
public interface ParkingService {

    ParkingTicket processVehicleEntry(String entryGate, Vehicle vehicle);

    ParkingTicket processVehicleExit(String ticketNumber, String paymentMode);

    ParkingTicket getActiveTicket(String licensePlate);

    List<ParkingTicket> listActiveTickets();

    BillingService getActiveBillingService();

    void setActiveBillingService(BillingService billingService);
}
