package com.smartparking.repository;

import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Payment;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Parking Ticket and Checkout persistence.
 * Problem Statement 9: Injected into ParkingService using Spring DI.
 */
public interface TicketRepository {

    ParkingTicket save(ParkingTicket ticket);

    Optional<ParkingTicket> findByTicketNumber(String ticketNumber);

    Optional<ParkingTicket> findActiveByLicensePlate(String licensePlate);

    List<ParkingTicket> findAllActiveTickets();

    /**
     * Executes vehicle exit, payment creation, and slot release within a single
     * atomic JDBC database transaction.
     * 
     * Mandatory Requirement: Use transactions wherever multiple related database
     * operations must succeed or fail together.
     */
    boolean completeTicketAndRecordPaymentTransactional(ParkingTicket ticket, Payment payment);
}
