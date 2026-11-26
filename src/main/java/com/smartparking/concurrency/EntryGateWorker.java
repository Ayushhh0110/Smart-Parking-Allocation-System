package com.smartparking.concurrency;

import com.smartparking.exception.ParkingException;
import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Vehicle;
import com.smartparking.service.ParkingService;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker thread simulating an active physical Entry Gate.
 * Problem Statement 9 - Requirement 3.
 */
public class EntryGateWorker implements Runnable {

    private final String gateName;
    private final ParkingService parkingService;
    private final Queue<Vehicle> vehicleArrivalQueue;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;
    private final Set<String> globalAllocatedSlots;
    private final AtomicInteger duplicateAllocationsCounter;
    private final List<ParkingTicket> issuedTickets;

    public EntryGateWorker(String gateName,
                           ParkingService parkingService,
                           Queue<Vehicle> vehicleArrivalQueue,
                           CountDownLatch startSignal,
                           CountDownLatch doneSignal,
                           Set<String> globalAllocatedSlots,
                           AtomicInteger duplicateAllocationsCounter,
                           List<ParkingTicket> issuedTickets) {
        this.gateName = gateName;
        this.parkingService = parkingService;
        this.vehicleArrivalQueue = vehicleArrivalQueue;
        this.startSignal = startSignal;
        this.doneSignal = doneSignal;
        this.globalAllocatedSlots = globalAllocatedSlots;
        this.duplicateAllocationsCounter = duplicateAllocationsCounter;
        this.issuedTickets = issuedTickets;
    }

    @Override
    public void run() {
        try {
            // Await synchronization latch so all gates start concurrently
            startSignal.await();
            System.out.printf("[%s] Gate opened on Thread '%s'. Starting vehicle admissions...\n",
                    gateName, Thread.currentThread().getName());

            while (true) {
                Vehicle vehicle = vehicleArrivalQueue.poll();
                if (vehicle == null) {
                    break; // No more vehicles waiting in this gate's line
                }

                try {
                    // Critical concurrent call to parking service
                    ParkingTicket ticket = parkingService.processVehicleEntry(gateName, vehicle);
                    issuedTickets.add(ticket);

                    String slotId = ticket.getSlot().getSlotId();

                    // Verification of mutual exclusion: check if another thread also claimed this slot
                    boolean isUnique = globalAllocatedSlots.add(slotId);
                    if (!isUnique) {
                        System.err.printf("!!! RACE CONDITION DETECTED !!! Duplicate slot %s assigned to %s at %s!\n",
                                slotId, vehicle.getLicensePlate(), gateName);
                        duplicateAllocationsCounter.incrementAndGet();
                    }

                    // Small simulated inter-arrival gap (5-20ms)
                    Thread.sleep(10);

                } catch (ParkingException pe) {
                    System.out.printf("  [!] [%s] Admission Failed for %s: %s\n",
                            gateName, vehicle.getLicensePlate(), pe.getMessage());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.printf("  [!] [%s] Unexpected error processing %s: %s\n",
                            gateName, vehicle.getLicensePlate(), e.getMessage());
                }
            }

            System.out.printf("[%s] All vehicles in queue processed. Gate idling.\n", gateName);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            doneSignal.countDown();
        }
    }
}
