# Smart Parking Allocation System - Design Note
**Course:** Enterprise Web Applications (CSL273) | **Minor Project**  
**Student Name:** Animesh Pasrija | **Roll No:** 25CSU028  
**Assigned Problem:** Problem Statement 9 (Smart Parking Allocation System)  

---

## 1. System Architecture & Class Diagram

### High-Level Architecture Overview
The system follows an enterprise **layered architecture** emphasizing separation of concerns and loose coupling via Spring Core Dependency Injection:
1. **Presentation / Entry Layer:** `MainApp` provides an interactive console CLI interface and an automated verification harness.
2. **Concurrency / Worker Layer:** `EntryGateWorker` threads running in an `ExecutorService` pool simulating simultaneous vehicle arrivals at multiple physical gates.
3. **Business Logic Layer:** 
   - `ParkingService` orchestrates vehicle admissions, ticketing, and exit settlements.
   - `SlotAllocationService` manages in-memory thread-safe collections and nearest-compatible slot allocation.
   - `BillingService` defines polymorphic pricing schemes (`RegularBillingService` vs `WeekendBillingService`).
   - `ReportService` computes analytical reporting statistics.
4. **Persistence Layer:** Repositories (`TicketRepository`, `ParkingSlotRepository`, `VehicleRepository`, `ReportRepository`) interacting with the database via pure JDBC `PreparedStatement` and manual transaction boundaries.
5. **Database Storage Layer:** Relational tables running in embedded H2 or external MySQL.

```
+-----------------------------------------------------------------------------------+
|                              Console Entry (MainApp)                              |
+-----------------------------------------------------------------------------------+
                                          |
                +-------------------------+-------------------------+
                |                                                   |
                v                                                   v
+-------------------------------+                   +-------------------------------+
|     EntryGateWorker (Gate 1)  |                   |   SimulationRunner (Multi)    |
|     EntryGateWorker (Gate 2)  |                   +-------------------------------+
|     EntryGateWorker (Gate 3)  |                                   |
+-------------------------------+                                   |
                |                                                   |
                +-------------------------+-------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                        ParkingService / ParkingServiceImpl                        |
+-----------------------------------------------------------------------------------+
             |                                     |                         |
             v                                     v                         v
+--------------------------+           +-----------------------+   +--------------------+
|  SlotAllocationService   |           |    BillingService     |   |  TicketRepository  |
|  (In-memory Collections  |           |  <<interface>>        |   |  (PreparedStatement|
|   & ReentrantLock)       |           +-----------------------+   |   & Transactions)  |
+--------------------------+                   /       \           +--------------------+
                                              /         \                    |
                                             v           v                   v
                            +--------------------+  +-------------------+  Database
                            |RegularBillingServ. |  |WeekendBillingServ.|  (H2 / MySQL)
                            +--------------------+  +-------------------+
```

---

## 2. Database Schema Design (Relational DDL)

The system persists multi-level facility structures, vehicles, audit tickets, and checkout payments:

```
+--------------------+       1:N       +----------------------+
|   PARKING_FLOORS   |----------------<|    PARKING_SLOTS     |
+--------------------+                 +----------------------+
| floor_number (PK)  |                 | slot_id (PK)         |
| floor_name         |                 | floor_number (FK)    |
| total_slots        |                 | slot_number          |
+--------------------+                 | slot_type            |
                                       | is_available         |
                                       | proximity_score      |
                                       +----------------------+
                                                  |
                                                  | 1:N
                                                  v
+--------------------+       1:N       +----------------------+       1:1       +--------------------+
|      VEHICLES      |----------------<|   PARKING_TICKETS    |----------------<|      PAYMENTS      |
+--------------------+                 +----------------------+                 +--------------------+
| license_plate (PK) |                 | ticket_id (PK, Auto) |                 | payment_id (PK,Auto|
| vehicle_type       |                 | ticket_number (UQ)   |                 | ticket_id (FK)     |
| owner_name         |                 | license_plate (FK)   |                 | amount             |
| contact_number     |                 | slot_id (FK)         |                 | payment_time       |
| registered_at      |                 | entry_gate           |                 | payment_mode       |
+--------------------+                 | entry_time           |                 | payment_status     |
                                       | exit_time            |                 +--------------------+
                                       | status               |
                                       | total_amount         |
                                       | billing_plan         |
                                       +----------------------+
```

### Transaction Boundary Justification:
When a vehicle departs, three interrelated operations must happen atomically:
1. `parking_tickets` status is updated to `COMPLETED` with timestamp and fare.
2. A new record is inserted into `payments` with the paid amount and mode.
3. The assigned slot in `parking_slots` is marked `is_available = TRUE`.

If server power is lost or an error occurs after charging the customer, the slot must not be marked occupied nor the payment orphaned. Thus, `conn.setAutoCommit(false)` is initiated, all statements executed, and committed via `conn.commit()`. If an exception occurs, `conn.rollback()` restores the prior state cleanly.

---

## 3. Java Collection Choices & Justification

Per project requirements, at least three meaningful collection types must be chosen and justified:

| Collection Type | Specific Class Used | Role in Smart Parking System | Justification & Complexity |
|---|---|---|---|
| **1. Sorted Set / Priority Pool** | `TreeSet<ParkingSlot>` (wrapped inside `EnumMap<SlotType, ...>`) | Holds all currently available slots per vehicle type, ordered by `SlotProximityComparator`. | **O(log N)** extraction: Calling `pollFirst()` immediately yields the physically nearest slot (lowest floor number, followed by lowest bay index). Maintains order upon slot re-insertion. |
| **2. Concurrent Hash Map** | `ConcurrentHashMap<String, ParkingSlot>` | `vehicleToSlotMap`: Maps `licensePlate -> ParkingSlot`. | **O(1)** average-time lookup. Allows lock-free concurrent reads while verifying whether a vehicle is already parked inside the facility, preventing double-entry abuse. |
| **3. Concurrent Queue** | `ConcurrentLinkedQueue<Vehicle>` | Task queue for each entry gate representing arriving vehicles. | **Lock-free, thread-safe FIFO queue** using Michael-Scott non-blocking queue algorithm. Guarantees that vehicles arriving at the same gate are admitted in order of arrival. |
| **4. Thread-Safe Hash Set** | `ConcurrentHashMap.newKeySet()` | Global tracking set during concurrent simulations. | **O(1)** atomic set membership test to verify that no two threads claimed the same `slotId`. |
| **5. Indexed List** | `ArrayList<ParkingTicket>` & `CopyOnWriteArrayList` | Audit logs of all issued tickets and historical queries. | Fast random-access indexing and sequential report iteration. |

---

## 4. Multithreading Design & Concurrency Control

### Concurrency Problem & Shared Resource
- **Shared Resource:** The pool of vacant `ParkingSlot` objects across all floors.
- **Race Condition Threat:** Two distinct entry gate threads (e.g. `Gate-1` and `Gate-2`) process arriving cars at the exact same millisecond. If both inspect the available pool and read slot `F1-C01` as free before either marks it occupied, **both cars would be allocated the identical parking bay**. This violates the physical law of parking and corrupts system state.

### Concurrency Control Mechanism
```
  [ Gate-1 Thread ]             [ Gate-2 Thread ]             [ Gate-3 Thread ]
         |                             |                             |
         v                             v                             v
  +------------------------------------------------------------------------+
  |               ReentrantLock (allocationLock.lock())                     |
  |               CRITICAL SECTION (Atomic Mutual Exclusion)              |
  |                                                                        |
  |  1. Check vehicle double-entry in vehicleToSlotMap                     |
  |  2. Retrieve target TreeSet for vehicle type (BIKE/CAR/EV)             |
  |  3. Poll nearest slot (pool.pollFirst())                               |
  |  4. Mark slot.occupy() (isAvailable = false)                           |
  |  5. Register vehicleToSlotMap.put(plate, slot)                         |
  |  6. Persist slot status in DB via PreparedStatement                    |
  |                                                                        |
  +------------------------------------------------------------------------+
         |                             |                             |
         v                             v                             v
  +------------------------------------------------------------------------+
  |              ReentrantLock (allocationLock.unlock())                   |
  +------------------------------------------------------------------------+
```

1. **Fair ReentrantLock:** `ReentrantLock(true)` guarantees FIFO fairness among gate threads, preventing thread starvation.
2. **Volatile Slot State:** `ParkingSlot.isAvailable` is marked `volatile` to guarantee immediate memory visibility across CPU caches.
3. **CountDownLatch Synchronization:** In the test harness, `CountDownLatch startSignal = new CountDownLatch(1)` releases all 3 gate worker threads simultaneously at the exact same millisecond to rigorously stress-test the concurrency boundary.
4. **Verification Proof:** The concurrency audit tracks `duplicateAllocationsCounter.get() == 0`, confirming that even under heavy multi-gate traffic, zero collisions occur.

---

## 5. Spring Core Dependency Injection Diagram

The project leverages Spring Core IoC with annotation-driven configuration:

```
                              @Configuration
                              (AppConfig.java)
                                     |
                                     v
                        @ComponentScan("com.smartparking")
                                     |
               +---------------------+---------------------+
               |                     |                     |
               v                     v                     v
       @Service              @Service              @Repository
(ParkingServiceImpl)  (SlotAllocationServiceImpl) (TicketRepositoryImpl)
       |       |                     |                     |
       |       |                     v                     |
       |       |            @Repository                    |
       |       |       (ParkingSlotRepoImpl)               |
       |       |                                           |
       |       +-------------------------------------------+
       |
       v
  @Autowired @Qualifier("regularBillingService")
  BillingService billingService
       ^
       |-- RegularBillingService (@Service("regularBillingService"))
       |-- WeekendBillingService (@Service("weekendBillingService"))
```

### Loose Coupling Demonstration:
- `ParkingServiceImpl` depends strictly on the interface `BillingService`, rather than any concrete class.
- Switching between `RegularBillingService` and `WeekendBillingService` requires zero changes to business logic; it can be achieved by changing the `@Qualifier` annotation value or invoking `setActiveBillingService()`.

---

## 6. Failed / Exceptional Test Case Analysis

### Scenario: Capacity Saturation & Slot Allocation Denial
- **Objective:** Verify that when the parking capacity for a specific vehicle category is fully exhausted, the system refuses admission gracefully without corrupting database integrity or assigning incompatible bays.
- **Trigger Condition:** The facility contains exactly 7 dedicated EV slots across 3 levels (3 on Level 1, 2 on Level 2, 2 on Level 3). Eight consecutive EV vehicles arrive.
- **Expected Behavior:** 
  - The first 7 EVs are successfully allocated slots in ascending order of proximity: `F1-E01`, `F1-E02`, `F1-E03`, `F2-E01`, `F2-E02`, `F3-E01`, `F3-E02`.
  - The 8th EV arrival triggers `SlotNotAvailableException`:
    ```
    com.smartparking.exception.SlotNotAvailableException: 
    PARKING FULL ERROR: No compatible free slot available for vehicle type: EV
    ```
  - The gate worker catches this exception, logs an alert to the console, and leaves all existing active tickets, slots, and database transactions intact.
- **Verification Result:** The test passed with 100% fidelity during automated execution.
