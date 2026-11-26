-- ========================================================
-- Smart Parking Allocation System - Database Schema (DDL)
-- Compatible with MySQL 8.x and H2 (MODE=MySQL)
-- Enterprise Web Applications (CSL273) - Problem Statement 9
-- ========================================================

-- Drop tables in reverse order of foreign keys
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS parking_tickets;
DROP TABLE IF EXISTS parking_slots;
DROP TABLE IF EXISTS parking_floors;
DROP TABLE IF EXISTS vehicles;

-- 1. Parking Floors
CREATE TABLE parking_floors (
    floor_number INT PRIMARY KEY,
    floor_name VARCHAR(50) NOT NULL,
    total_slots INT NOT NULL
);

-- 2. Parking Slots
CREATE TABLE parking_slots (
    slot_id VARCHAR(20) PRIMARY KEY,
    floor_number INT NOT NULL,
    slot_number INT NOT NULL,
    slot_type VARCHAR(10) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    proximity_score INT NOT NULL,
    CONSTRAINT fk_slot_floor FOREIGN KEY (floor_number) REFERENCES parking_floors(floor_number)
);

-- 3. Vehicles
CREATE TABLE vehicles (
    license_plate VARCHAR(20) PRIMARY KEY,
    vehicle_type VARCHAR(10) NOT NULL,
    owner_name VARCHAR(100),
    contact_number VARCHAR(20),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Parking Tickets
CREATE TABLE parking_tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(50) UNIQUE NOT NULL,
    license_plate VARCHAR(20) NOT NULL,
    slot_id VARCHAR(20) NOT NULL,
    entry_gate VARCHAR(20) NOT NULL,
    entry_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    billing_plan VARCHAR(30) DEFAULT 'Regular',
    CONSTRAINT fk_ticket_vehicle FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate),
    CONSTRAINT fk_ticket_slot FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id)
);

-- 5. Payments (Populated on transactional checkout)
CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_time TIMESTAMP NOT NULL,
    payment_mode VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_payment_ticket FOREIGN KEY (ticket_id) REFERENCES parking_tickets(ticket_id)
);

-- Indexes for performance
CREATE INDEX idx_slot_type_available ON parking_slots(slot_type, is_available);
CREATE INDEX idx_ticket_status ON parking_tickets(status);
CREATE INDEX idx_ticket_vehicle ON parking_tickets(license_plate);
