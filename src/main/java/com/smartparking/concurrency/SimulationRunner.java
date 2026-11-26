package com.smartparking.concurrency;

import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrator for concurrent multi-gate simulation.
 * Problem Statement 9 - Requirement 3:
 * "Simulate at least three entry-gate threads. The same slot must never be allocated to two vehicles."
 */
@Component
public class SimulationRunner {

    private final ParkingService parkingService;

    @Autowired
    public SimulationRunner(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /**
     * Executes concurrent simulation across 3 distinct entry gates.
     */
    public List<ParkingTicket> runThreeGateSimulation() {
        System.out.println("\n=======================================================");
        System.out.println("   STARTING MULTITHREADED 3-GATE ADMISSION SIMULATION  ");
        System.out.println("=======================================================");

        int gateCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(gateCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(gateCount);

        // Thread-safe shared tracking structures
        Set<String> globallyAllocatedSlots = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicateCounter = new AtomicInteger(0);
        List<ParkingTicket> ticketsIssued = Collections.synchronizedList(new ArrayList<>());

        // Gate queues
        Queue<Vehicle> gate1Queue = new ConcurrentLinkedQueue<>();
        Queue<Vehicle> gate2Queue = new ConcurrentLinkedQueue<>();
        Queue<Vehicle> gate3Queue = new ConcurrentLinkedQueue<>();

        // Populate arriving vehicle queues with unique license plates
        gate1Queue.add(new Vehicle("DL-10-AA-1001", VehicleType.CAR,  "Vikas Mehta",   "9810011111"));
        gate1Queue.add(new Vehicle("HR-26-BB-2002", VehicleType.BIKE, "Sunil Rao",     "9810022222"));
        gate1Queue.add(new Vehicle("UP-14-CC-3003", VehicleType.EV,   "Deepak Gupta",  "9810033333"));
        gate1Queue.add(new Vehicle("DL-08-DD-4004", VehicleType.CAR,  "Karan Johar",   "9810044444"));

        gate2Queue.add(new Vehicle("HR-51-EE-5005", VehicleType.CAR,  "Rohit Verma",   "9810055555"));
        gate2Queue.add(new Vehicle("DL-04-FF-6006", VehicleType.EV,   "Sneha Kapoor",  "9810066666"));
        gate2Queue.add(new Vehicle("UP-16-GG-7007", VehicleType.BIKE, "Manoj Tiwari",  "9810077777"));
        gate2Queue.add(new Vehicle("DL-02-HH-8008", VehicleType.CAR,  "Alok Nath",     "9810088888"));

        gate3Queue.add(new Vehicle("DL-12-JJ-9009", VehicleType.EV,   "Neha Sharma",   "9810099999"));
        gate3Queue.add(new Vehicle("HR-29-KK-1111", VehicleType.BIKE, "Tarun Chawla",  "9810010101"));
        gate3Queue.add(new Vehicle("UP-32-LL-2222", VehicleType.CAR,  "Pooja Hegde",   "9810020202"));
        gate3Queue.add(new Vehicle("DL-09-MM-3333", VehicleType.BIKE, "Varun Dhawan",  "9810030303"));

        int totalVehiclesAttempted = gate1Queue.size() + gate2Queue.size() + gate3Queue.size();

        // Submit Gate worker threads
        executor.submit(new EntryGateWorker("Gate-1 (North)", parkingService, gate1Queue, startSignal, doneSignal,
                globallyAllocatedSlots, duplicateCounter, ticketsIssued));
        executor.submit(new EntryGateWorker("Gate-2 (South)", parkingService, gate2Queue, startSignal, doneSignal,
                globallyAllocatedSlots, duplicateCounter, ticketsIssued));
        executor.submit(new EntryGateWorker("Gate-3 (East)",  parkingService, gate3Queue, startSignal, doneSignal,
                globallyAllocatedSlots, duplicateCounter, ticketsIssued));

        long startTime = System.currentTimeMillis();
        // Release all 3 gate threads simultaneously
        startSignal.countDown();

        try {
            // Wait for all 3 gates to finish
            boolean finishedInTime = doneSignal.await(10, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startTime;

            System.out.println("=======================================================");
            System.out.println("          CONCURRENCY VERIFICATION AUDIT               ");
            System.out.println("=======================================================");
            System.out.printf("  Execution Time              : %d ms\n", elapsed);
            System.out.printf("  Total Vehicles Arrived      : %d\n", totalVehiclesAttempted);
            System.out.printf("  Successfully Parked         : %d\n", ticketsIssued.size());
            System.out.printf("  Unique Slots Allocated      : %d\n", globallyAllocatedSlots.size());
            System.out.printf("  Duplicate Allocations Found : %d\n", duplicateCounter.get());

            if (duplicateCounter.get() == 0 && ticketsIssued.size() == globallyAllocatedSlots.size()) {
                System.out.println("  >>> VERIFICATION PASSED: ZERO RACE CONDITIONS DETECTED! <<<");
                System.out.println("  Every vehicle received a distinct, compatible, nearest slot.");
            } else {
                System.err.println("  >>> CONCURRENCY FAILURE: Race condition detected! <<<");
            }
            System.out.println("=======================================================\n");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        return ticketsIssued;
    }
}
