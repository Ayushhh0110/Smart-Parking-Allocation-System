-- ========================================================
-- Smart Parking Allocation System - Sample Seed Data (DML)
-- Compatible with MySQL 8.x and H2 (MODE=MySQL)
-- Enterprise Web Applications (CSL273) - Problem Statement 9
-- ========================================================

-- 1. Insert Multi-Level Floors (Ground, First, Second)
INSERT INTO parking_floors (floor_number, floor_name, total_slots) VALUES
(1, 'Ground Floor (Level 0)', 10),
(2, 'First Floor (Level 1)', 10),
(3, 'Second Floor (Level 2)', 10);

-- 2. Insert Slots for Floor 1 (Nearest to Entry Gates: Level 1)
-- Floor 1: 3 BIKE, 4 CAR, 3 EV
INSERT INTO parking_slots (slot_id, floor_number, slot_number, slot_type, is_available, proximity_score) VALUES
('F1-B01', 1, 1, 'BIKE', TRUE, 101),
('F1-B02', 1, 2, 'BIKE', TRUE, 102),
('F1-B03', 1, 3, 'BIKE', TRUE, 103),
('F1-C01', 1, 4, 'CAR',  TRUE, 104),
('F1-C02', 1, 5, 'CAR',  TRUE, 105),
('F1-C03', 1, 6, 'CAR',  TRUE, 106),
('F1-C04', 1, 7, 'CAR',  TRUE, 107),
('F1-E01', 1, 8, 'EV',   TRUE, 108),
('F1-E02', 1, 9, 'EV',   TRUE, 109),
('F1-E03', 1, 10, 'EV',  TRUE, 110);

-- Floor 2: 3 BIKE, 5 CAR, 2 EV
INSERT INTO parking_slots (slot_id, floor_number, slot_number, slot_type, is_available, proximity_score) VALUES
('F2-B01', 2, 1, 'BIKE', TRUE, 201),
('F2-B02', 2, 2, 'BIKE', TRUE, 202),
('F2-B03', 2, 3, 'BIKE', TRUE, 203),
('F2-C01', 2, 4, 'CAR',  TRUE, 204),
('F2-C02', 2, 5, 'CAR',  TRUE, 205),
('F2-C03', 2, 6, 'CAR',  TRUE, 206),
('F2-C04', 2, 7, 'CAR',  TRUE, 207),
('F2-C05', 2, 8, 'CAR',  TRUE, 208),
('F2-E01', 2, 9, 'EV',   TRUE, 209),
('F2-E02', 2, 10, 'EV',  TRUE, 210);

-- Floor 3: 4 BIKE, 4 CAR, 2 EV
INSERT INTO parking_slots (slot_id, floor_number, slot_number, slot_type, is_available, proximity_score) VALUES
('F3-B01', 3, 1, 'BIKE', TRUE, 301),
('F3-B02', 3, 2, 'BIKE', TRUE, 302),
('F3-B03', 3, 3, 'BIKE', TRUE, 303),
('F3-B04', 3, 4, 'BIKE', TRUE, 304),
('F3-C01', 3, 5, 'CAR',  TRUE, 305),
('F3-C02', 3, 6, 'CAR',  TRUE, 306),
('F3-C03', 3, 7, 'CAR',  TRUE, 307),
('F3-C04', 3, 8, 'CAR',  TRUE, 308),
('F3-E01', 3, 9, 'EV',   TRUE, 309),
('F3-E02', 3, 10, 'EV',  TRUE, 310);

-- 3. Insert Historical Sample Vehicles
INSERT INTO vehicles (license_plate, vehicle_type, owner_name, contact_number, registered_at) VALUES
('DL-01-AB-1234', 'CAR',  'Rahul Sharma', '9811122233', CURRENT_TIMESTAMP),
('HR-26-CD-5678', 'BIKE', 'Animesh Pasrija', '9876543210', CURRENT_TIMESTAMP),
('UP-16-EF-9012', 'EV',   'Priya Verma',  '9822334455', CURRENT_TIMESTAMP),
('DL-03-GH-3456', 'CAR',  'Amit Kumar',   '9833445566', CURRENT_TIMESTAMP);

-- 4. Insert Past Completed Tickets & Payments (for Reporting Queries demonstration)
INSERT INTO parking_tickets (ticket_id, ticket_number, license_plate, slot_id, entry_gate, entry_time, exit_time, status, total_amount, billing_plan) VALUES
(1001, 'TCK-20261120-001', 'DL-01-AB-1234', 'F1-C01', 'Gate-1', '2026-11-20 09:00:00', '2026-11-20 12:30:00', 'COMPLETED', 140.00, 'Regular'),
(1002, 'TCK-20261120-002', 'HR-26-CD-5678', 'F1-B01', 'Gate-2', '2026-11-20 10:15:00', '2026-11-20 11:45:00', 'COMPLETED', 30.00,  'Regular'),
(1003, 'TCK-20261120-003', 'UP-16-EF-9012', 'F1-E01', 'Gate-3', '2026-11-20 11:00:00', '2026-11-20 14:00:00', 'COMPLETED', 105.00, 'Weekend');

INSERT INTO payments (payment_id, ticket_id, amount, payment_time, payment_mode, payment_status) VALUES
(501, 1001, 140.00, '2026-11-20 12:30:05', 'UPI',  'SUCCESS'),
(502, 1002, 30.00,  '2026-11-20 11:45:10', 'CASH', 'SUCCESS'),
(503, 1003, 105.00, '2026-11-20 14:00:15', 'CARD', 'SUCCESS');
