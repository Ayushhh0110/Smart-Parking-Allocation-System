# Smart Parking Allocation System - Execution Evidence & Console Logs
**Course:** Enterprise Web Applications (CSL273) | **Minor Project**  
**Student Name:** Animesh Pasrija | **Roll No:** 25CSU028  
**Student Name:** Ayush Bhat | **Roll No:** 25csu044

This document records the exact, verified console execution outputs for different operational scenarios of the application.

---

## Scenario 1: Multithreaded 3-Gate Concurrent Admission Simulation
Demonstrates 3 entry gate worker threads (`Gate-1 (North)`, `Gate-2 (South)`, `Gate-3 (East)`) running on separate thread pool workers and admitting 12 arriving vehicles simultaneously.

```
=======================================================
   STARTING MULTITHREADED 3-GATE ADMISSION SIMULATION  
=======================================================
[Gate-1 (North)] Gate opened on Thread 'pool-1-thread-1'. Starting vehicle admissions...
[Gate-2 (South)] Gate opened on Thread 'pool-1-thread-2'. Starting vehicle admissions...
[Gate-3 (East)] Gate opened on Thread 'pool-1-thread-3'. Starting vehicle admissions...

>> [Gate-2 (South)] CAR entered at Gate-2 (South) -> Allocated Nearest Slot: F1-C01 (Floor 1, Dist:104)
>> [Gate-1 (North)] CAR entered at Gate-1 (North) -> Allocated Nearest Slot: F1-C02 (Floor 1, Dist:105)
>> [Gate-3 (East)] EV entered at Gate-3 (East) -> Allocated Nearest Slot: F1-E01 (Floor 1, Dist:108)
>> [Gate-1 (North)] BIKE entered at Gate-1 (North) -> Allocated Nearest Slot: F1-B01 (Floor 1, Dist:101)
>> [Gate-3 (East)] BIKE entered at Gate-3 (East) -> Allocated Nearest Slot: F1-B02 (Floor 1, Dist:102)
>> [Gate-2 (South)] EV entered at Gate-2 (South) -> Allocated Nearest Slot: F1-E02 (Floor 1, Dist:109)
>> [Gate-1 (North)] EV entered at Gate-1 (North) -> Allocated Nearest Slot: F1-E03 (Floor 1, Dist:110)
>> [Gate-3 (East)] CAR entered at Gate-3 (East) -> Allocated Nearest Slot: F1-C03 (Floor 1, Dist:106)
>> [Gate-2 (South)] BIKE entered at Gate-2 (South) -> Allocated Nearest Slot: F1-B03 (Floor 1, Dist:103)
>> [Gate-3 (East)] BIKE entered at Gate-3 (East) -> Allocated Nearest Slot: F2-B01 (Floor 2, Dist:201)
>> [Gate-1 (North)] CAR entered at Gate-1 (North) -> Allocated Nearest Slot: F1-C04 (Floor 1, Dist:107)
>> [Gate-2 (South)] CAR entered at Gate-2 (South) -> Allocated Nearest Slot: F2-C01 (Floor 2, Dist:204)

[Gate-3 (East)] All vehicles in queue processed. Gate idling.
[Gate-1 (North)] All vehicles in queue processed. Gate idling.
[Gate-2 (South)] All vehicles in queue processed. Gate idling.

=======================================================
          CONCURRENCY VERIFICATION AUDIT               
=======================================================
  Execution Time              : 78 ms
  Total Vehicles Arrived      : 12
  Successfully Parked         : 12
  Unique Slots Allocated      : 12
  Duplicate Allocations Found : 0
  >>> VERIFICATION PASSED: ZERO RACE CONDITIONS DETECTED! <<<
  Every vehicle received a distinct, compatible, nearest slot.
=======================================================
```

---

## Scenario 2: Transactional Checkout & Slot Release (JDBC Transaction)
Shows vehicle exit, fare calculation via Spring `BillingService`, payment insertion, and slot release within a single atomic database transaction.

```
--- TRANSACTIONAL CHECKOUT & PAYMENT ---
Checking out vehicle: DL-08-DD-4004 (Ticket: TCK-20261121-82385)
[TicketRepository] Transaction Committed: Ticket #TCK-20261121-82385 settled with Payment ID 504 (Slot F1-C01 released).
<< [EXIT] Vehicle DL-08-DD-4004 exited. Slot F1-C01 freed. Fare: Rs 40.00 (Paid via UPI)

Checking out vehicle: DL-10-AA-1001 (Ticket: TCK-20261121-82388)
[TicketRepository] Transaction Committed: Ticket #TCK-20261121-82388 settled with Payment ID 505 (Slot F1-C02 released).
<< [EXIT] Vehicle DL-10-AA-1001 exited. Slot F1-C02 freed. Fare: Rs 40.00 (Paid via CARD)
```

---

## Scenario 3: Spring DI Tariff Switching (Regular vs Weekend Peak)
Demonstrates switching the injected `BillingService` implementation at runtime from `RegularBillingService` to `WeekendBillingService`.

```
--- SPRING DI TARIFF SWITCHING (REGULAR -> WEEKEND) ---
[ParkingService] Billing strategy switched to: Weekend Peak Surcharge (+Rs 10 Base Fee & Rush Hourly Rates)
Checking out under Weekend Tariff: DL-12-JJ-9009
[TicketRepository] Transaction Committed: Ticket #TCK-20261121-82387 settled with Payment ID 506 (Slot F1-E01 released).
<< [EXIT] Vehicle DL-12-JJ-9009 exited. Slot F1-E01 freed. Fare: Rs 55.00 (Paid via CASH)
[ParkingService] Billing strategy switched to: Regular Weekday Tariff
```

---

## Scenario 4: Analytical Reporting Queries (Occupancy, Free-Slots, Utilization, Revenue)
Demonstrates JDBC aggregation queries formatted into console reports.

```
=======================================================
             SMART PARKING OCCUPANCY REPORT            
=======================================================
  Total Parking Capacity : 30 slots
  Currently Occupied     : 9 slots
  Available / Free Slots : 21 slots
  Occupancy Rate         : 30.00%
  Visual Utilization     : [######..............]
=======================================================

=======================================================================
             FREE SLOTS BREAKDOWN (BY FLOOR & SLOT TYPE)               
=======================================================================
Floor      | Floor Name               | Type     | Total  | Free   | Occupied
-----------------------------------------------------------------------
Level 1    | Ground Floor (Level 0)   | BIKE     | 3      | 0      | 3       
Level 1    | Ground Floor (Level 0)   | CAR      | 4      | 2      | 2       
Level 1    | Ground Floor (Level 0)   | EV       | 3      | 1      | 2       
Level 2    | First Floor (Level 1)    | BIKE     | 3      | 2      | 1       
Level 2    | First Floor (Level 1)    | CAR      | 5      | 4      | 1       
Level 2    | First Floor (Level 1)    | EV       | 2      | 2      | 0       
Level 3    | Second Floor (Level 2)   | BIKE     | 4      | 4      | 0       
Level 3    | Second Floor (Level 2)   | CAR      | 4      | 4      | 0       
Level 3    | Second Floor (Level 2)   | EV       | 2      | 2      | 0       
=======================================================================

===============================================================================
                  VEHICLE-TYPE-WISE UTILIZATION REPORT                         
===============================================================================
Vehicle Type | Total Trips  | Currently Parked | Completed Trips | Revenue (INR)
-------------------------------------------------------------------------------
CAR          | 6            | 3                | 3               | Rs 220.00    
BIKE         | 5            | 4                | 1               | Rs 30.00     
EV           | 4            | 2                | 2               | Rs 160.00    
===============================================================================

=======================================================
              REVENUE & FINANCIAL REPORT               
=======================================================
  Total Successful Payments : 6
  Total Revenue Collected   : Rs 410.00
  Average Ticket Fare       : Rs 68.33
  Highest Recorded Ticket   : Rs 140.00
-------------------------------------------------------
  Breakdown by Payment Mode:
    - UPI    :   2 transactions | Total: Rs 180.00
    - CARD   :   2 transactions | Total: Rs 145.00
    - CASH   :   2 transactions | Total: Rs 85.00
=======================================================
```

---

## Scenario 5: Exceptional / Failed Test Case (Capacity Exhaustion)
Shows graceful error handling when all compatible slots are exhausted.

```
=======================================================
  TESTING EXCEPTIONAL CASE: SLOT NOT AVAILABLE ERROR   
=======================================================
Attempting to overflow EV slots to trigger SlotNotAvailableException...
>> [Gate-Overflow] EV entered at Gate-Overflow -> Allocated Nearest Slot: F1-E01 (Floor 1, Dist:108)
>> [Gate-Overflow] EV entered at Gate-Overflow -> Allocated Nearest Slot: F2-E01 (Floor 2, Dist:209)
>> [Gate-Overflow] EV entered at Gate-Overflow -> Allocated Nearest Slot: F2-E02 (Floor 2, Dist:210)
>> [Gate-Overflow] EV entered at Gate-Overflow -> Allocated Nearest Slot: F3-E01 (Floor 3, Dist:309)
>> [Gate-Overflow] EV entered at Gate-Overflow -> Allocated Nearest Slot: F3-E02 (Floor 3, Dist:310)

>>> EXPECTED EXCEPTION CAUGHT AND HANDLED SUCCESSFULLY! <<<
Exception Class   : com.smartparking.exception.SlotNotAvailableException
Exception Message : PARKING FULL ERROR: No compatible free slot available for vehicle type: EV
Vehicle Type      : EV
System gracefully rejected admission without corrupting state.
=======================================================
```
