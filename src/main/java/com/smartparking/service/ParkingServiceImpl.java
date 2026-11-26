package com.smartparking.service;

import com.smartparking.exception.InvalidVehicleException;
import com.smartparking.exception.TicketNotFoundException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Payment;
import com.smartparking.model.TicketStatus;
import com.smartparking.model.Vehicle;
import com.smartparking.repository.TicketRepository;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of ParkingService with Spring Dependency Injection.
 * 
 * VIVA / SPRING DI HIGHLIGHT:
 * Note the injection of @Qualifier("regularBillingService").
 * The active billing strategy can be toggled at runtime or modified via Spring config
 * to WeekendBillingService.
 */
@Service("parkingService")
public class ParkingServiceImpl implements ParkingService {

    private final SlotAllocationService slotAllocationService;
    private final TicketRepository ticketRepository;
    private final VehicleRepository vehicleRepository;
    private volatile BillingService billingService;

    private static final AtomicLong TICKET_SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);

    @Autowired
    public ParkingServiceImpl(SlotAllocationService slotAllocationService,
                              TicketRepository ticketRepository,
                              VehicleRepository vehicleRepository,
                              @Qualifier("regularBillingService") BillingService billingService) {
        this.slotAllocationService = slotAllocationService;
        this.ticketRepository = ticketRepository;
        this.vehicleRepository = vehicleRepository;
        this.billingService = billingService;
    }

    @Override
    public ParkingTicket processVehicleEntry(String entryGate, Vehicle vehicle) {
        if (vehicle == null) {
            throw new InvalidVehicleException("Vehicle cannot be null.");
        }

        // 1. Persist vehicle details in DB
        vehicleRepository.saveOrUpdate(vehicle);

        // 2. Allocate nearest compatible slot (Thread-safe)
        ParkingSlot allocatedSlot = slotAllocationService.allocateNearestSlot(vehicle);

        // 3. Issue & persist ticket (rollback slot allocation if ticket persistence fails)
        String ticketNumber = generateTicketNumber();
        ParkingTicket ticket = new ParkingTicket(ticketNumber, vehicle, allocatedSlot, entryGate, billingService.getSchemeName());

        ParkingTicket savedTicket;
        try {
            savedTicket = ticketRepository.save(ticket);
        } catch (RuntimeException ex) {
            slotAllocationService.releaseSlot(allocatedSlot, vehicle);
            throw ex;
        }

        System.out.println(String.format(">> [%s] %s entered at %s -> Allocated Nearest Slot: %s (Floor %d, Dist:%d)",
                entryGate, vehicle.getVehicleType(), entryGate, allocatedSlot.getSlotId(),
                allocatedSlot.getFloorNumber(), allocatedSlot.getProximityScore()));

        return savedTicket;
    }

    @Override
    public ParkingTicket processVehicleExit(String ticketNumber, String paymentMode) {
        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            throw new TicketNotFoundException("Ticket number cannot be empty.");
        }

        // 1. Fetch active ticket from repository
        ParkingTicket ticket = ticketRepository.findByTicketNumber(ticketNumber.trim())
                .orElseThrow(() -> new TicketNotFoundException(ticketNumber));

        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Ticket " + ticketNumber + " is already closed/completed!");
        }

        // 2. Compute duration and billing fee
        LocalDateTime exitTime = LocalDateTime.now();
        ticket.setExitTime(exitTime);

        double fee = billingService.calculateFee(ticket.getVehicle().getVehicleType(), ticket.getEntryTime(), exitTime);
        ticket.setTotalAmount(fee);
        ticket.setStatus(TicketStatus.COMPLETED);

        // 3. Create payment record
        String mode = (paymentMode != null && !paymentMode.trim().isEmpty()) ? paymentMode.toUpperCase() : "UPI";
        Payment payment = new Payment(ticket.getTicketId(), fee, mode, "SUCCESS");

        // 4. ATOMIC DATABASE TRANSACTION (Update ticket, Insert payment, Release slot in DB)
        boolean transactionSuccess = ticketRepository.completeTicketAndRecordPaymentTransactional(ticket, payment);

        if (!transactionSuccess) {
            throw new RuntimeException("Checkout failed due to a database transaction failure.");
        }

        // 5. Release slot in in-memory collections pool
        slotAllocationService.releaseSlot(ticket.getSlot(), ticket.getVehicle());

        System.out.println(String.format("<< [EXIT] Vehicle %s exited. Slot %s freed. Fare: Rs %.2f (Paid via %s)",
                ticket.getVehicle().getLicensePlate(), ticket.getSlot().getSlotId(), fee, mode));

        return ticket;
    }

    @Override
    public ParkingTicket getActiveTicket(String licensePlate) {
        if (licensePlate == null) return null;
        return ticketRepository.findActiveByLicensePlate(licensePlate.trim().toUpperCase()).orElse(null);
    }

    @Override
    public List<ParkingTicket> listActiveTickets() {
        return ticketRepository.findAllActiveTickets();
    }

    @Override
    public BillingService getActiveBillingService() {
        return billingService;
    }

    @Override
    public void setActiveBillingService(BillingService billingService) {
        this.billingService = billingService;
        System.out.println("[ParkingService] Billing strategy switched to: " + billingService.getSchemeName());
    }

    private String generateTicketNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("TCK-%s-%05d", datePrefix, TICKET_SEQUENCE.incrementAndGet());
    }
}
