-- ========================================================
-- Smart Parking Allocation System - Reporting Queries
-- Enterprise Web Applications (CSL273) - Problem Statement 9
-- Requirement 6: Generate occupancy, free-slot, vehicle-type-wise utilization and revenue reports.
-- ========================================================

-- 1. Overall System Occupancy Report
SELECT 
    COUNT(*) AS total_slots,
    SUM(CASE WHEN is_available = FALSE THEN 1 ELSE 0 END) AS occupied_slots,
    SUM(CASE WHEN is_available = TRUE THEN 1 ELSE 0 END) AS free_slots,
    ROUND((SUM(CASE WHEN is_available = FALSE THEN 1.0 ELSE 0.0 END) / COUNT(*)) * 100, 2) AS occupancy_percentage
FROM parking_slots;

-- 2. Free Slots Report (Grouped by Floor and Slot Type)
SELECT 
    f.floor_number,
    f.floor_name,
    s.slot_type,
    COUNT(s.slot_id) AS total_slots_in_group,
    SUM(CASE WHEN s.is_available = TRUE THEN 1 ELSE 0 END) AS available_slots,
    SUM(CASE WHEN s.is_available = FALSE THEN 1 ELSE 0 END) AS occupied_slots
FROM parking_floors f
JOIN parking_slots s ON f.floor_number = s.floor_number
GROUP BY f.floor_number, f.floor_name, s.slot_type
ORDER BY f.floor_number ASC, s.slot_type ASC;

-- 3. Vehicle-Type-Wise Utilization Report
SELECT 
    v.vehicle_type,
    COUNT(t.ticket_id) AS total_trips,
    SUM(CASE WHEN t.status = 'ACTIVE' THEN 1 ELSE 0 END) AS currently_parked,
    SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_parkings,
    COALESCE(SUM(t.total_amount), 0.00) AS total_revenue_by_type
FROM vehicles v
LEFT JOIN parking_tickets t ON v.license_plate = t.license_plate
GROUP BY v.vehicle_type
ORDER BY total_trips DESC;

-- 4. Revenue and Financial Summary Report
SELECT 
    COUNT(p.payment_id) AS successful_transactions,
    COALESCE(SUM(p.amount), 0.00) AS total_revenue,
    COALESCE(AVG(p.amount), 0.00) AS average_ticket_fare,
    COALESCE(MAX(p.amount), 0.00) AS highest_fee_collected,
    p.payment_mode,
    COUNT(p.payment_id) AS count_by_mode
FROM payments p
WHERE p.payment_status = 'SUCCESS'
GROUP BY p.payment_mode;
