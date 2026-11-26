package com.smartparking.service;

import com.smartparking.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service generating formatted console reports for management & evaluation.
 * Problem Statement 9 - Requirement 6.
 */
@Service("reportService")
public class ReportService {

    private final ReportRepository reportRepository;

    @Autowired
    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public void printOccupancyReport() {
        Map<String, Object> occ = reportRepository.getOccupancySummary();
        int total = occ.get("totalSlots") != null ? (int) occ.get("totalSlots") : 0;
        int occupied = occ.get("occupiedSlots") != null ? (int) occ.get("occupiedSlots") : 0;
        int free = occ.get("freeSlots") != null ? (int) occ.get("freeSlots") : 0;
        double pct = occ.get("occupancyPercentage") != null ? (double) occ.get("occupancyPercentage") : 0.0;

        System.out.println("\n=======================================================");
        System.out.println("             SMART PARKING OCCUPANCY REPORT            ");
        System.out.println("=======================================================");
        System.out.printf("  Total Parking Capacity : %d slots\n", total);
        System.out.printf("  Currently Occupied     : %d slots\n", occupied);
        System.out.printf("  Available / Free Slots : %d slots\n", free);
        System.out.printf("  Occupancy Rate         : %.2f%%\n", pct);
        System.out.print("  Visual Utilization     : [");
        int bars = (int) (pct / 5);
        for (int i = 0; i < 20; i++) {
            System.out.print(i < bars ? "#" : ".");
        }
        System.out.println("]");
        System.out.println("=======================================================");
    }

    public void printFreeSlotsReport() {
        List<Map<String, Object>> rows = reportRepository.getFreeSlotsByFloorAndType();
        System.out.println("\n=======================================================================");
        System.out.println("             FREE SLOTS BREAKDOWN (BY FLOOR & SLOT TYPE)               ");
        System.out.println("=======================================================================");
        System.out.printf("%-10s | %-24s | %-8s | %-6s | %-6s | %-8s\n",
                "Floor", "Floor Name", "Type", "Total", "Free", "Occupied");
        System.out.println("-----------------------------------------------------------------------");

        for (Map<String, Object> r : rows) {
            System.out.printf("Level %-4d | %-24s | %-8s | %-6d | %-6d | %-8d\n",
                    r.get("floorNumber"),
                    r.get("floorName"),
                    r.get("slotType"),
                    r.get("totalSlots"),
                    r.get("availableSlots"),
                    r.get("occupiedSlots"));
        }
        System.out.println("=======================================================================");
    }

    public void printVehicleTypeUtilizationReport() {
        List<Map<String, Object>> rows = reportRepository.getVehicleTypeUtilization();
        System.out.println("\n===============================================================================");
        System.out.println("                  VEHICLE-TYPE-WISE UTILIZATION REPORT                         ");
        System.out.println("===============================================================================");
        System.out.printf("%-12s | %-12s | %-14s | %-14s | %-12s\n",
                "Vehicle Type", "Total Trips", "Currently Parked", "Completed Trips", "Revenue (INR)");
        System.out.println("-------------------------------------------------------------------------------");

        for (Map<String, Object> r : rows) {
            System.out.printf("%-12s | %-12d | %-16d | %-15d | Rs %-10.2f\n",
                    r.get("vehicleType"),
                    r.get("totalTrips"),
                    r.get("currentlyParked"),
                    r.get("completedParkings"),
                    r.get("totalRevenue"));
        }
        System.out.println("===============================================================================");
    }

    public void printRevenueReport() {
        Map<String, Object> rev = reportRepository.getRevenueAnalytics();
        List<Map<String, Object>> modes = reportRepository.getPaymentModeBreakdown();

        int totalTx = rev.get("totalTransactions") != null ? (int) rev.get("totalTransactions") : 0;
        double totalRev = rev.get("totalRevenue") != null ? (double) rev.get("totalRevenue") : 0.0;
        double avgFare = rev.get("averageFare") != null ? (double) rev.get("averageFare") : 0.0;
        double maxFare = rev.get("highestFare") != null ? (double) rev.get("highestFare") : 0.0;

        System.out.println("\n=======================================================");
        System.out.println("              REVENUE & FINANCIAL REPORT               ");
        System.out.println("=======================================================");
        System.out.printf("  Total Successful Payments : %d\n", totalTx);
        System.out.printf("  Total Revenue Collected   : Rs %.2f\n", totalRev);
        System.out.printf("  Average Ticket Fare       : Rs %.2f\n", avgFare);
        System.out.printf("  Highest Recorded Ticket   : Rs %.2f\n", maxFare);
        System.out.println("-------------------------------------------------------");
        System.out.println("  Breakdown by Payment Mode:");
        for (Map<String, Object> m : modes) {
            System.out.printf("    - %-6s : %3d transactions | Total: Rs %.2f\n",
                    m.get("paymentMode"), m.get("count"), m.get("totalRevenue"));
        }
        System.out.println("=======================================================");
    }

    public void printAllReports() {
        printOccupancyReport();
        printFreeSlotsReport();
        printVehicleTypeUtilizationReport();
        printRevenueReport();
    }
}
