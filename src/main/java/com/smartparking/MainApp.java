package com.smartparking;

import com.smartparking.concurrency.SimulationRunner;
import com.smartparking.config.AppConfig;
import com.smartparking.exception.ParkingException;
import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.service.BillingService;
import com.smartparking.service.ParkingService;
import com.smartparking.service.ReportService;
import com.smartparking.service.SlotAllocationService;
import com.smartparking.util.DatabaseUtil;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Scanner;

/**
 * Main Application Console Entry Point.
 * Enterprise Web Applications (CSL273) - Problem Statement 9:
 * Smart Parking Allocation System.
 */
public class MainApp {

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("     SMART MULTI-LEVEL PARKING ALLOCATION SYSTEM - CSL273 MINOR         ");
        System.out.println("========================================================================");

        // 1. Initialize Database Schema & Seed Data
        DatabaseUtil.initializeDatabase();

        // 2. Bootstrap Spring Core Application Context (Annotation-driven DI)
        System.out.println("[MainApp] Bootstrapping Spring Core Context (AnnotationConfigApplicationContext)...");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 3. Retrieve Spring-managed service beans
        ParkingService parkingService = context.getBean(ParkingService.class);
        SlotAllocationService slotService = context.getBean(SlotAllocationService.class);
        ReportService reportService = context.getBean(ReportService.class);
        SimulationRunner simulationRunner = context.getBean(SimulationRunner.class);
        BillingService regularBilling = (BillingService) context.getBean("regularBillingService");
        BillingService weekendBilling = (BillingService) context.getBean("weekendBillingService");

        System.out.println("[MainApp] System ready. All services wired successfully through Spring DI.\n");

        // If automated demo mode is passed via CLI argument
        if (args.length > 0 && ("--demo".equalsIgnoreCase(args[0]) || "--test".equalsIgnoreCase(args[0]))) {
            runAutomatedDemonstration(parkingService, reportService, simulationRunner, weekendBilling, regularBilling);
            context.close();
            return;
        }

        // If running in dedicated web server mode
        if (args.length > 0 && "--web".equalsIgnoreCase(args[0])) {
            com.smartparking.web.ParkingWebServer webServer = context.getBean(com.smartparking.web.ParkingWebServer.class);
            webServer.start(8080);
            System.out.println("[MainApp] Running in dedicated Web Server mode (http://localhost:8080). Press Ctrl+C to stop.");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                System.out.println("[MainApp] Web server interrupted.");
            }
            webServer.stop();
            context.close();
            return;
        }

        // Interactive Console Menu
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu(parkingService.getActiveBillingService());
            System.out.print("Enter your choice (1-10): ");
            if (!scanner.hasNextLine()) {
                System.out.println("\n[MainApp] End of input stream reached. Exiting.");
                break;
            }
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleManualEntry(scanner, parkingService);
                    break;
                case "2":
                    handleManualExit(scanner, parkingService);
                    break;
                case "3":
                    System.out.println("\n[Action] Launching 3 concurrent Gate threads (Gate-1, Gate-2, Gate-3)...");
                    simulationRunner.runThreeGateSimulation();
                    break;
                case "4":
                    reportService.printOccupancyReport();
                    reportService.printFreeSlotsReport();
                    break;
                case "5":
                    reportService.printVehicleTypeUtilizationReport();
                    break;
                case "6":
                    reportService.printRevenueReport();
                    break;
                case "7":
                    toggleBillingStrategy(parkingService, regularBilling, weekendBilling);
                    break;
                case "8":
                    testSlotFullExceptionScenario(parkingService);
                    break;
                case "9":
                    displayActiveTickets(parkingService);
                    break;
                case "10":
                    running = false;
                    System.out.println("\n[MainApp] Exiting Smart Parking System. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid selection. Please enter a number between 1 and 10.");
            }
        }

        context.close();
    }

    private static void printMenu(BillingService activeBilling) {
        System.out.println("\n------------------------------------------------------------------------");
        System.out.println("                         CONSOLE CONTROL PANEL                          ");
        System.out.println("------------------------------------------------------------------------");
        System.out.printf("  Active Tariff Plan: %s\n", activeBilling.getSchemeName());
        System.out.printf("  Database Engine   : %s\n", DatabaseUtil.getDbMode().toUpperCase());
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  1. Park Vehicle (Manual Admission at Gate)");
        System.out.println("  2. Vehicle Exit & Checkout (Transactional Payment & Slot Release)");
        System.out.println("  3. Run Multithreaded 3-Gate Concurrent Admission Simulation");
        System.out.println("  4. Display Occupancy & Free Slots Report");
        System.out.println("  5. Display Vehicle-Type-Wise Utilization Report");
        System.out.println("  6. Display Revenue & Financial Analytics Report");
        System.out.println("  7. Switch Billing Strategy (Regular <-> Weekend Peak Surcharge)");
        System.out.println("  8. Test Exceptional Case: Slot Full Rejection Scenario");
        System.out.println("  9. View Active Parked Tickets");
        System.out.println("  10. Exit Application");
        System.out.println("------------------------------------------------------------------------");
    }

    private static void handleManualEntry(Scanner scanner, ParkingService parkingService) {
        try {
            System.out.println("\n--- Vehicle Admission ---");
            System.out.print("Enter License Plate (e.g. DL-01-XY-9999): ");
            String plate = scanner.nextLine().trim();
            if (plate.isEmpty()) {
                System.out.println("License plate cannot be empty!");
                return;
            }

            System.out.print("Enter Vehicle Type (1: BIKE, 2: CAR, 3: EV): ");
            String typeChoice = scanner.nextLine().trim();
            VehicleType type = VehicleType.CAR;
            if ("1".equals(typeChoice)) type = VehicleType.BIKE;
            else if ("3".equals(typeChoice)) type = VehicleType.EV;

            System.out.print("Enter Driver / Owner Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Enter Contact Number: ");
            String phone = scanner.nextLine().trim();

            System.out.print("Select Entry Gate (1: Gate-1, 2: Gate-2, 3: Gate-3): ");
            String gateChoice = scanner.nextLine().trim();
            String gate = "Gate-" + ("1".equals(gateChoice) ? "1" : ("2".equals(gateChoice) ? "2" : "3"));

            Vehicle vehicle = new Vehicle(plate, type, name.isEmpty() ? "Walk-in" : name, phone);
            ParkingTicket ticket = parkingService.processVehicleEntry(gate, vehicle);

            System.out.println("\n>> SUCCESS: Ticket Issued Successfully!");
            System.out.println("   Ticket Number : " + ticket.getTicketNumber());
            System.out.println("   Allocated Bay : " + ticket.getSlot().getSlotId() + " (Level " + ticket.getSlot().getFloorNumber() + ")");
            System.out.println("   Tariff Scheme : " + ticket.getBillingPlan());

        } catch (ParkingException pe) {
            System.err.println("Admission Failed: " + pe.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void handleManualExit(Scanner scanner, ParkingService parkingService) {
        try {
            System.out.println("\n--- Vehicle Checkout & Settlement ---");
            System.out.print("Enter Ticket Number or License Plate: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Input cannot be empty.");
                return;
            }

            String ticketNumber = input;
            if (!input.startsWith("TCK-")) {
                // Look up active ticket by plate
                ParkingTicket active = parkingService.getActiveTicket(input);
                if (active != null) {
                    ticketNumber = active.getTicketNumber();
                }
            }

            System.out.print("Select Payment Mode (1: UPI, 2: CARD, 3: CASH): ");
            String modeChoice = scanner.nextLine().trim();
            String mode = "1".equals(modeChoice) ? "UPI" : ("2".equals(modeChoice) ? "CARD" : "CASH");

            ParkingTicket settled = parkingService.processVehicleExit(ticketNumber, mode);
            System.out.println("\n>> SUCCESS: Checkout & Payment Transaction Completed!");
            System.out.println("   Ticket Number   : " + settled.getTicketNumber());
            System.out.println("   Vehicle         : " + settled.getVehicle().getLicensePlate() + " (" + settled.getVehicle().getVehicleType() + ")");
            System.out.println("   Freed Slot      : " + settled.getSlot().getSlotId());
            System.out.printf("   Total Fare Paid : Rs %.2f (via %s)\n", settled.getTotalAmount(), mode);

        } catch (ParkingException pe) {
            System.err.println("Checkout Failed: " + pe.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void toggleBillingStrategy(ParkingService parkingService, BillingService regular, BillingService weekend) {
        BillingService current = parkingService.getActiveBillingService();
        if (current == regular) {
            parkingService.setActiveBillingService(weekend);
        } else {
            parkingService.setActiveBillingService(regular);
        }
    }

    private static void testSlotFullExceptionScenario(ParkingService parkingService) {
        System.out.println("\n=======================================================");
        System.out.println("  TESTING EXCEPTIONAL CASE: SLOT NOT AVAILABLE ERROR   ");
        System.out.println("=======================================================");
        System.out.println("Attempting to overflow EV slots to trigger SlotNotAvailableException...");

        int count = 1;
        try {
            while (true) {
                String plate = String.format("TEST-EV-%04d", count);
                Vehicle ev = new Vehicle(plate, VehicleType.EV, "Overflow Tester " + count, "9999999999");
                parkingService.processVehicleEntry("Gate-Overflow", ev);
                count++;
            }
        } catch (SlotNotAvailableException snae) {
            System.out.println("\n>>> EXPECTED EXCEPTION CAUGHT AND HANDLED SUCCESSFULLY! <<<");
            System.out.println("Exception Class   : " + snae.getClass().getName());
            System.out.println("Exception Message : " + snae.getMessage());
            System.out.println("Vehicle Type      : " + snae.getVehicleType());
            System.out.println("System gracefully rejected admission without corrupting state.");
            System.out.println("=======================================================\n");
        }
    }

    private static void displayActiveTickets(ParkingService parkingService) {
        List<ParkingTicket> tickets = parkingService.listActiveTickets();
        System.out.println("\n=======================================================");
        System.out.println("             CURRENTLY ACTIVE PARKED VEHICLES          ");
        System.out.println("=======================================================");
        if (tickets.isEmpty()) {
            System.out.println("  No vehicles currently parked.");
        } else {
            for (ParkingTicket t : tickets) {
                System.out.println("  * " + t);
            }
        }
        System.out.println("=======================================================");
    }

    /**
     * Automated demonstration that executes all required rubric flows.
     */
    public static void runAutomatedDemonstration(ParkingService parkingService,
                                                ReportService reportService,
                                                SimulationRunner simulationRunner,
                                                BillingService weekendBilling,
                                                BillingService regularBilling) {
        System.out.println("\n>>> EXECUTING AUTOMATED COMPREHENSIVE SYSTEM VERIFICATION <<<\n");

        // 1. Initial State Report
        System.out.println("--- STEP 1: INITIAL OCCUPANCY & SLOTS ---");
        reportService.printOccupancyReport();

        // 2. Multithreaded 3-Gate Concurrent Simulation
        System.out.println("\n--- STEP 2: MULTITHREADED CONCURRENT 3-GATE SIMULATION ---");
        List<ParkingTicket> tickets = simulationRunner.runThreeGateSimulation();

        // 3. Post-Admission Occupancy & Breakdown
        System.out.println("\n--- STEP 3: POST-CONCURRENCY OCCUPANCY & BREAKDOWN ---");
        reportService.printOccupancyReport();
        reportService.printFreeSlotsReport();

        // 4. Vehicle Exit & Database Transaction Checkout
        System.out.println("\n--- STEP 4: TRANSACTIONAL CHECKOUT & PAYMENT ---");
        if (!tickets.isEmpty()) {
            ParkingTicket first = tickets.get(0);
            System.out.println("Checking out vehicle: " + first.getVehicle().getLicensePlate() + " (Ticket: " + first.getTicketNumber() + ")");
            parkingService.processVehicleExit(first.getTicketNumber(), "UPI");

            if (tickets.size() > 1) {
                ParkingTicket second = tickets.get(1);
                parkingService.processVehicleExit(second.getTicketNumber(), "CARD");
            }
        }

        // 5. Switch Spring Billing Strategy
        System.out.println("\n--- STEP 5: SPRING DI TARIFF SWITCHING (REGULAR -> WEEKEND) ---");
        parkingService.setActiveBillingService(weekendBilling);
        if (tickets.size() > 2) {
            ParkingTicket third = tickets.get(2);
            System.out.println("Checking out under Weekend Tariff: " + third.getVehicle().getLicensePlate());
            parkingService.processVehicleExit(third.getTicketNumber(), "CASH");
        }
        parkingService.setActiveBillingService(regularBilling); // revert

        // 6. Final Reporting
        System.out.println("\n--- STEP 6: COMPREHENSIVE ANALYTICAL REPORTS ---");
        reportService.printAllReports();

        // 7. Exceptional / Failed Test Case
        System.out.println("\n--- STEP 7: FAILED / EXCEPTIONAL TEST CASE DEMONSTRATION ---");
        testSlotFullExceptionScenario(parkingService);

        System.out.println("\n>>> ALL DEMO & VERIFICATION STEPS COMPLETED SUCCESSFULLY! <<<\n");
    }
}
