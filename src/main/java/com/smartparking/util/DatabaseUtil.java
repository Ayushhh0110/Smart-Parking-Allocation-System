package com.smartparking.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Utility class managing JDBC Database connections, drivers,
 * and automated schema initialization (H2 / MySQL).
 */
public class DatabaseUtil {
    private static Properties props = new Properties();
    private static String dbMode = "h2";
    private static String driverClassName;
    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static boolean isInitialized = false;

    static {
        loadConfiguration();
    }

    private static synchronized void loadConfiguration() {
        try (InputStream in = DatabaseUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                // Fallback default properties
                props.setProperty("db.mode", "h2");
                props.setProperty("h2.driver", "org.h2.Driver");
                props.setProperty("h2.url", "jdbc:h2:mem:smart_parking_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
                props.setProperty("h2.user", "sa");
                props.setProperty("h2.password", "");
            }

            dbMode = props.getProperty("db.mode", "h2").trim().toLowerCase();

            if ("mysql".equals(dbMode)) {
                driverClassName = props.getProperty("mysql.driver", "com.mysql.cj.jdbc.Driver");
                jdbcUrl = props.getProperty("mysql.url", "jdbc:mysql://localhost:3306/smart_parking_db");
                username = props.getProperty("mysql.user", "root");
                password = props.getProperty("mysql.password", "");
            } else {
                driverClassName = props.getProperty("h2.driver", "org.h2.Driver");
                jdbcUrl = props.getProperty("h2.url", "jdbc:h2:mem:smart_parking_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
                username = props.getProperty("h2.user", "sa");
                password = props.getProperty("h2.password", "");
            }

            Class.forName(driverClassName);
        } catch (Exception e) {
            System.err.println("[DatabaseUtil] Warning during driver loading: " + e.getMessage());
        }
    }

    /**
     * Obtains a fresh JDBC connection.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * Initializes database tables and seed data if not already done.
     */
    public static synchronized void initializeDatabase() {
        if (isInitialized) return;

        System.out.println("[DatabaseUtil] Initializing Database (Mode: " + dbMode.toUpperCase() + ")...");
        try (Connection conn = getConnection()) {
            executeSqlScript(conn, "sql/schema.sql");
            executeSqlScript(conn, "sql/data.sql");
            isInitialized = true;
            System.out.println("[DatabaseUtil] Database schema & sample data loaded successfully.");
        } catch (Exception e) {
            System.err.println("[DatabaseUtil] Failed to initialize database: " + e.getMessage());
            // Try fallback from classpath
            fallbackClasspathInit();
        }
    }

    private static void fallbackClasspathInit() {
        // Fallback for minimal bootstrap if external file not found
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_floors (floor_number INT PRIMARY KEY, floor_name VARCHAR(50), total_slots INT);");
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_slots (slot_id VARCHAR(20) PRIMARY KEY, floor_number INT, slot_number INT, slot_type VARCHAR(10), is_available BOOLEAN, proximity_score INT);");
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicles (license_plate VARCHAR(20) PRIMARY KEY, vehicle_type VARCHAR(10), owner_name VARCHAR(100), contact_number VARCHAR(20), registered_at TIMESTAMP);");
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_tickets (ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY, ticket_number VARCHAR(50) UNIQUE, license_plate VARCHAR(20), slot_id VARCHAR(20), entry_gate VARCHAR(20), entry_time TIMESTAMP, exit_time TIMESTAMP, status VARCHAR(20), total_amount DECIMAL(10,2), billing_plan VARCHAR(30));");
            stmt.execute("CREATE TABLE IF NOT EXISTS payments (payment_id BIGINT AUTO_INCREMENT PRIMARY KEY, ticket_id BIGINT, amount DECIMAL(10,2), payment_time TIMESTAMP, payment_mode VARCHAR(20), payment_status VARCHAR(20));");
            isInitialized = true;
        } catch (Exception ex) {
            System.err.println("[DatabaseUtil] Fallback init error: " + ex.getMessage());
        }
    }

    private static void executeSqlScript(Connection conn, String relativePath) {
        Path p = Paths.get(relativePath);
        if (!Files.exists(p)) {
            // Try with project root
            p = Paths.get("SmartParkingSystem", relativePath);
        }
        if (!Files.exists(p)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8);
             Statement stmt = conn.createStatement()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Strip SQL comments
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                if (line.contains("--")) {
                    line = line.substring(0, line.indexOf("--")).trim();
                }
                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().replace(";", "").trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                        } catch (SQLException sqle) {
                            // Suppress drop table warnings
                            if (!sql.toUpperCase().startsWith("DROP")) {
                                System.err.println("[DatabaseUtil] Warning on SQL: " + sql + " -> " + sqle.getMessage());
                            }
                        }
                    }
                    sb.setLength(0);
                }
            }
        } catch (Exception e) {
            System.err.println("[DatabaseUtil] Error executing script " + relativePath + ": " + e.getMessage());
        }
    }

    public static String getDbMode() {
        return dbMode;
    }

    public static void setDbMode(String mode) {
        dbMode = mode;
        loadConfiguration();
    }
}
