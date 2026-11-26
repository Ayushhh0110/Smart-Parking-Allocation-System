package com.smartparking.web;

import com.smartparking.concurrency.SimulationRunner;
import com.smartparking.exception.ParkingException;
import com.smartparking.model.ParkingFloor;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingTicket;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ReportRepository;
import com.smartparking.service.BillingService;
import com.smartparking.service.ParkingService;
import com.smartparking.service.SlotAllocationService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Lightweight, zero-dependency embedded HTTP Server & REST API.
 * Uses built-in Java SE HttpServer (jdk.httpserver).
 * Serves the modern Single Page Web Dashboard on http://localhost:8080
 */
@Component
public class ParkingWebServer {

    private final ParkingService parkingService;
    private final SlotAllocationService slotService;
    private final ReportRepository reportRepository;
    private final SimulationRunner simulationRunner;
    private final BillingService regularBilling;
    private final BillingService weekendBilling;

    private HttpServer server;
    private int port = 8080;

    @Autowired
    public ParkingWebServer(ParkingService parkingService,
                            SlotAllocationService slotService,
                            ReportRepository reportRepository,
                            SimulationRunner simulationRunner,
                            @org.springframework.beans.factory.annotation.Qualifier("regularBillingService") BillingService regularBillingService,
                            @org.springframework.beans.factory.annotation.Qualifier("weekendBillingService") BillingService weekendBillingService) {
        this.parkingService = parkingService;
        this.slotService = slotService;
        this.reportRepository = reportRepository;
        this.simulationRunner = simulationRunner;
        this.regularBilling = regularBillingService;
        this.weekendBilling = weekendBillingService;
    }

    public synchronized void start(int port) {
        this.port = port;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.setExecutor(Executors.newFixedThreadPool(10));

            // Static Dashboard
            server.createContext("/", new StaticFileHandler());

            // REST APIs
            server.createContext("/api/status", new StatusHandler());
            server.createContext("/api/reports", new ReportsHandler());
            server.createContext("/api/tickets/active", new ActiveTicketsHandler());
            server.createContext("/api/entry", new EntryHandler());
            server.createContext("/api/exit", new ExitHandler());
            server.createContext("/api/simulate", new SimulateHandler());
            server.createContext("/api/tariff/toggle", new TariffToggleHandler());
            server.createContext("/api/simulate-overflow", new OverflowHandler());

            server.start();
            System.out.println("========================================================================");
            System.out.println(" 🌐 SMART PARKING WEB DASHBOARD RUNNING AT: http://localhost:" + port);
            System.out.println("========================================================================");

        } catch (IOException e) {
            System.err.println("[WebServer] Failed to start HTTP server on port " + port + ": " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[WebServer] Server stopped.");
        }
    }

    // -------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Look in resources/web or filesystem
            byte[] bytes = loadWebResource(path);
            if (bytes != null) {
                String contentType = "text/html; charset=utf-8";
                if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".js")) contentType = "application/javascript";
                else if (path.endsWith(".json")) contentType = "application/json";

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            Map<String, Object> occ = reportRepository.getOccupancySummary();
            BillingService billing = parkingService.getActiveBillingService();

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"totalSlots\":").append(occ.getOrDefault("totalSlots", 30)).append(",");
            json.append("\"occupiedSlots\":").append(occ.getOrDefault("occupiedSlots", 0)).append(",");
            json.append("\"freeSlots\":").append(occ.getOrDefault("freeSlots", 30)).append(",");
            json.append("\"occupancyPercentage\":").append(occ.getOrDefault("occupancyPercentage", 0.0)).append(",");
            json.append("\"activeTariff\":{")
                    .append("\"name\":").append(quote(billing.getSchemeName())).append(",")
                    .append("\"bikeRate\":").append(billing.getBaseHourlyRate(VehicleType.BIKE)).append(",")
                    .append("\"carRate\":").append(billing.getBaseHourlyRate(VehicleType.CAR)).append(",")
                    .append("\"evRate\":").append(billing.getBaseHourlyRate(VehicleType.EV))
                    .append("},");

            // Floors & Slots
            json.append("\"floors\":[");
            Map<Integer, ParkingFloor> floors = slotService.getFloors();
            boolean firstFloor = true;
            for (int fn = 1; fn <= floors.size(); fn++) {
                ParkingFloor floor = floors.get(fn);
                if (floor == null) continue;
                if (!firstFloor) json.append(",");
                firstFloor = false;

                json.append("{");
                json.append("\"floorNumber\":").append(floor.getFloorNumber()).append(",");
                json.append("\"floorName\":").append(quote(floor.getFloorName())).append(",");
                json.append("\"slots\":[");

                boolean firstSlot = true;
                for (ParkingSlot s : floor.getSlots().values()) {
                    if (!firstSlot) json.append(",");
                    firstSlot = false;

                    json.append("{");
                    json.append("\"slotId\":").append(quote(s.getSlotId())).append(",");
                    json.append("\"slotNumber\":").append(s.getSlotNumber()).append(",");
                    json.append("\"slotType\":").append(quote(s.getSlotType().name())).append(",");
                    json.append("\"isAvailable\":").append(s.isAvailable()).append(",");
                    json.append("\"proximityScore\":").append(s.getProximityScore());
                    json.append("}");
                }
                json.append("]}");
            }
            json.append("]}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    private class ReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            Map<String, Object> occ = reportRepository.getOccupancySummary();
            List<Map<String, Object>> freeSlots = reportRepository.getFreeSlotsByFloorAndType();
            List<Map<String, Object>> util = reportRepository.getVehicleTypeUtilization();
            Map<String, Object> rev = reportRepository.getRevenueAnalytics();
            List<Map<String, Object>> paymentModes = reportRepository.getPaymentModeBreakdown();

            StringBuilder json = new StringBuilder();
            json.append("{");

            // Occupancy
            json.append("\"occupancy\":{")
                    .append("\"total\":").append(occ.getOrDefault("totalSlots", 0)).append(",")
                    .append("\"occupied\":").append(occ.getOrDefault("occupiedSlots", 0)).append(",")
                    .append("\"free\":").append(occ.getOrDefault("freeSlots", 0)).append(",")
                    .append("\"percentage\":").append(occ.getOrDefault("occupancyPercentage", 0.0))
                    .append("},");

            // Free Slots
            json.append("\"freeSlots\":[");
            for (int i = 0; i < freeSlots.size(); i++) {
                if (i > 0) json.append(",");
                Map<String, Object> r = freeSlots.get(i);
                json.append("{")
                        .append("\"floorNumber\":").append(r.get("floorNumber")).append(",")
                        .append("\"floorName\":").append(quote(String.valueOf(r.get("floorName")))).append(",")
                        .append("\"slotType\":").append(quote(String.valueOf(r.get("slotType")))).append(",")
                        .append("\"totalSlots\":").append(r.get("totalSlots")).append(",")
                        .append("\"availableSlots\":").append(r.get("availableSlots")).append(",")
                        .append("\"occupiedSlots\":").append(r.get("occupiedSlots"))
                        .append("}");
            }
            json.append("],");

            // Vehicle utilization
            json.append("\"utilization\":[");
            for (int i = 0; i < util.size(); i++) {
                if (i > 0) json.append(",");
                Map<String, Object> u = util.get(i);
                json.append("{")
                        .append("\"vehicleType\":").append(quote(String.valueOf(u.get("vehicleType")))).append(",")
                        .append("\"totalTrips\":").append(u.get("totalTrips")).append(",")
                        .append("\"currentlyParked\":").append(u.get("currentlyParked")).append(",")
                        .append("\"completedParkings\":").append(u.get("completedParkings")).append(",")
                        .append("\"totalRevenue\":").append(u.get("totalRevenue"))
                        .append("}");
            }
            json.append("],");

            // Revenue
            json.append("\"revenue\":{")
                    .append("\"totalTransactions\":").append(rev.getOrDefault("totalTransactions", 0)).append(",")
                    .append("\"totalRevenue\":").append(rev.getOrDefault("totalRevenue", 0.0)).append(",")
                    .append("\"averageFare\":").append(rev.getOrDefault("averageFare", 0.0)).append(",")
                    .append("\"highestFare\":").append(rev.getOrDefault("highestFare", 0.0))
                    .append("},");

            // Modes
            json.append("\"paymentModes\":[");
            for (int i = 0; i < paymentModes.size(); i++) {
                if (i > 0) json.append(",");
                Map<String, Object> m = paymentModes.get(i);
                json.append("{")
                        .append("\"mode\":").append(quote(String.valueOf(m.get("paymentMode")))).append(",")
                        .append("\"count\":").append(m.get("count")).append(",")
                        .append("\"totalRevenue\":").append(m.get("totalRevenue"))
                        .append("}");
            }
            json.append("]");

            json.append("}");
            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    private class ActiveTicketsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            List<ParkingTicket> tickets = parkingService.listActiveTickets();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < tickets.size(); i++) {
                if (i > 0) json.append(",");
                ParkingTicket t = tickets.get(i);
                json.append("{")
                        .append("\"ticketNumber\":").append(quote(t.getTicketNumber())).append(",")
                        .append("\"licensePlate\":").append(quote(t.getVehicle().getLicensePlate())).append(",")
                        .append("\"vehicleType\":").append(quote(t.getVehicle().getVehicleType().name())).append(",")
                        .append("\"ownerName\":").append(quote(t.getVehicle().getOwnerName())).append(",")
                        .append("\"slotId\":").append(quote(t.getSlot().getSlotId())).append(",")
                        .append("\"floorNumber\":").append(t.getSlot().getFloorNumber()).append(",")
                        .append("\"entryGate\":").append(quote(t.getEntryGate())).append(",")
                        .append("\"entryTime\":").append(quote(t.getEntryTime().format(fmt))).append(",")
                        .append("\"billingPlan\":").append(quote(t.getBillingPlan()))
                        .append("}");
            }
            json.append("]");
            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    private class EntryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readBody(exchange);
            Map<String, String> params = parseJsonSimple(body);

            String plate = params.get("licensePlate");
            String typeStr = params.getOrDefault("vehicleType", "CAR");
            String owner = params.getOrDefault("ownerName", "Walk-in Customer");
            String phone = params.getOrDefault("contactNumber", "");
            String gate = params.getOrDefault("entryGate", "Gate-1 (Main)");

            if (plate == null || plate.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"License plate is required.\"}");
                return;
            }

            try {
                VehicleType vType = VehicleType.valueOf(typeStr.toUpperCase());
                Vehicle vehicle = new Vehicle(plate, vType, owner, phone);
                ParkingTicket ticket = parkingService.processVehicleEntry(gate, vehicle);

                StringBuilder json = new StringBuilder();
                json.append("{\"success\":true,")
                        .append("\"ticketNumber\":").append(quote(ticket.getTicketNumber())).append(",")
                        .append("\"licensePlate\":").append(quote(ticket.getVehicle().getLicensePlate())).append(",")
                        .append("\"vehicleType\":").append(quote(ticket.getVehicle().getVehicleType().name())).append(",")
                        .append("\"slotId\":").append(quote(ticket.getSlot().getSlotId())).append(",")
                        .append("\"floorNumber\":").append(ticket.getSlot().getFloorNumber()).append(",")
                        .append("\"billingPlan\":").append(quote(ticket.getBillingPlan()))
                        .append("}");
                sendJsonResponse(exchange, 200, json.toString());

            } catch (ParkingException pe) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":" + quote(pe.getMessage()) + "}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":" + quote(e.getMessage()) + "}");
            }
        }
    }

    private class ExitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readBody(exchange);
            Map<String, String> params = parseJsonSimple(body);

            String ticketNum = params.get("ticketNumber");
            String mode = params.getOrDefault("paymentMode", "UPI");

            if (ticketNum == null || ticketNum.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Ticket number is required.\"}");
                return;
            }

            try {
                ParkingTicket ticket = parkingService.processVehicleExit(ticketNum.trim(), mode);
                StringBuilder json = new StringBuilder();
                json.append("{\"success\":true,")
                        .append("\"ticketNumber\":").append(quote(ticket.getTicketNumber())).append(",")
                        .append("\"licensePlate\":").append(quote(ticket.getVehicle().getLicensePlate())).append(",")
                        .append("\"freedSlotId\":").append(quote(ticket.getSlot().getSlotId())).append(",")
                        .append("\"totalAmount\":").append(ticket.getTotalAmount()).append(",")
                        .append("\"paymentMode\":").append(quote(mode))
                        .append("}");
                sendJsonResponse(exchange, 200, json.toString());

            } catch (ParkingException pe) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":" + quote(pe.getMessage()) + "}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":" + quote(e.getMessage()) + "}");
            }
        }
    }

    private class SimulateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            try {
                List<ParkingTicket> tickets = simulationRunner.runThreeGateSimulation();
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Simulation finished successfully!\",\"ticketsIssued\":" + tickets.size() + "}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":" + quote(e.getMessage()) + "}");
            }
        }
    }

    private class TariffToggleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            BillingService current = parkingService.getActiveBillingService();
            if (current == regularBilling) {
                parkingService.setActiveBillingService(weekendBilling);
            } else {
                parkingService.setActiveBillingService(regularBilling);
            }

            BillingService updated = parkingService.getActiveBillingService();
            sendJsonResponse(exchange, 200, "{\"success\":true,\"schemeName\":" + quote(updated.getSchemeName()) + "}");
        }
    }

    private class OverflowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }

            int count = 1;
            String caughtMsg = null;
            try {
                while (count < 20) {
                    Vehicle ev = new Vehicle("WEB-EV-" + count, VehicleType.EV, "Web Stress Tester " + count, "9999999999");
                    parkingService.processVehicleEntry("Gate-StressTest", ev);
                    count++;
                }
            } catch (ParkingException pe) {
                caughtMsg = pe.getMessage();
            }

            sendJsonResponse(exchange, 200, "{\"success\":true,\"exceptionCaught\":" + quote(caughtMsg) + "}");
        }
    }

    // -------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------

    private byte[] loadWebResource(String path) {
        if (path.startsWith("/")) path = path.substring(1);

        // 1. Check local filesystem: src/main/resources/web/
        Path p = Paths.get("src/main/resources/web", path);
        if (!Files.exists(p)) {
            p = Paths.get("SmartParkingSystem/src/main/resources/web", path);
        }
        if (Files.exists(p)) {
            try {
                return Files.readAllBytes(p);
            } catch (IOException ignored) {}
        }

        // 2. Classpath resource
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("web/" + path)) {
            if (in != null) {
                return in.readAllBytes();
            }
        } catch (IOException ignored) {}

        return null;
    }

    private void sendCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private Map<String, String> parseJsonSimple(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        String clean = body.trim();
        if (clean.startsWith("{")) clean = clean.substring(1);
        if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);

        String[] pairs = clean.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim().replace("\"", "");
                map.put(k, v);
            }
        }
        return map;
    }

    private String quote(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }
}
