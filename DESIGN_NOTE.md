# Smart Parking Allocation System - Design Note
**Course:** Enterprise Web Applications (CSL273) | **Problem Statement 9**  
**Student Name:** Animesh Pasrija | **Roll No:** 25CSU028  
**Student Name:** Ayush Bhat | **Roll No:** 25CSU044

## 1. Architecture / Class Diagram
```
MainApp (console menu, --demo) ---> SimulationRunner ---> EntryGateWorker x 3
      |                                                        |
      +----------------> ParkingService <----------------------+
                         (ParkingServiceImpl)
        +--------------------+---------------------+-------------------+
        v                    v                     v                   v
SlotAllocationService   <<interface>>       TicketRepository    VehicleRepository
        |               BillingService             |                   |
        v                 /       \                +---------+---------+
ParkingSlotRepository  Regular   Weekend                     v
                                                      H2 / MySQL (JDBC)
MainApp ---> ReportService ---> ReportRepository ---> H2 / MySQL
```
Layers: console and worker threads, services, repositories (JDBC `PreparedStatement`), database.
Models: `Vehicle`, `ParkingSlot`, `ParkingFloor`, `ParkingTicket`, `Payment`.

## 2. Database Schema

| Table | Primary key | Main columns | Foreign keys |
|---|---|---|---|
| `parking_floors` | floor_number | floor_name, total_slots | - |
| `parking_slots` | slot_id | floor_number, slot_number, slot_type, is_available, proximity_score | floor_number |
| `vehicles` | license_plate | vehicle_type, owner_name, contact_number, registered_at | - |
| `parking_tickets` | ticket_id | ticket_number (unique), license_plate, slot_id, entry_gate, entry_time, exit_time, status, total_amount, billing_plan | license_plate, slot_id |
| `payments` | payment_id | ticket_id, amount, payment_time, payment_mode, payment_status | ticket_id |

**Checkout transaction:** updating the ticket, inserting the payment and freeing the slot run on one
connection with `setAutoCommit(false)`. All three are committed together; on an `SQLException` they are
rolled back, so a payment is never saved without the ticket being closed.

## 3. Collection Choices

| Collection | Use | Reason |
|---|---|---|
| `EnumMap<SlotType, TreeSet<ParkingSlot>>` | Free slots per type | `TreeSet` with `SlotProximityComparator` keeps slots sorted; `pollFirst()` returns the nearest in O(log N). |
| `ConcurrentHashMap<String, ParkingSlot>` | Vehicle-to-slot mapping, slot lookup by id | O(1) average lookup; used to reject double entry of a vehicle. |
| `ConcurrentHashMap.newKeySet()` | Occupied slots; duplicate check in simulation | Thread-safe set; `add()` returns false for a repeated slot. |
| `ConcurrentLinkedQueue<Vehicle>` | Arrival queue of each gate | Thread-safe, non-blocking FIFO order. |
| Synchronized `ArrayList<ParkingTicket>` | Tickets issued in the simulation | Safe concurrent adds and indexed access. |

## 4. Multithreading Design
**Shared resource:** the pool of free slots. Without synchronization, concurrent access to the pool
could corrupt it or give two vehicles the same slot.

```
 Gate-1 thread        Gate-2 thread        Gate-3 thread      (ExecutorService, 3 threads)
       \                    |                    /
        +------- allocationLock.lock()  (fair ReentrantLock) -------+
        | 1. reject if vehicle already parked                       |
        | 2. pool.pollFirst() for the compatible slot type          |
        | 3. slot.occupy(), update occupied set and vehicle map     |
        | 4. update slot availability in the database               |
        +------------------ allocationLock.unlock() ----------------+
```
- The same lock protects `releaseSlot()`, so allocation and release cannot interleave.
- `CountDownLatch startSignal` starts all gates together; `doneSignal` waits for them to finish.
- `ParkingSlot.isAvailable` is `volatile` for visibility between threads.
- Result of the demo run: 12 vehicles, 12 unique slots, 0 duplicate allocations.

## 5. Spring Dependency Diagram
```
AppConfig (@Configuration, @ComponentScan("com.smartparking"))

SimulationRunner ----> ParkingService (ParkingServiceImpl)
ParkingServiceImpl --> SlotAllocationService (SlotAllocationServiceImpl)
                   --> TicketRepository      (TicketRepositoryImpl)
                   --> VehicleRepository     (VehicleRepositoryImpl)
                   --> @Qualifier("regularBillingService") BillingService
                           |-- RegularBillingService  (injected)
                           |-- WeekendBillingService  (alternative)
SlotAllocationServiceImpl --> ParkingSlotRepository (ParkingSlotRepositoryImpl)
ReportService             --> ReportRepository      (ReportRepositoryImpl)
```
Constructor injection is used and services depend on interfaces. The tariff can be changed through the
`@Qualifier` value, or at runtime with `setActiveBillingService()` (menu option 7).

## 6. Failed / Exceptional Test Case
- **Objective:** a vehicle must be refused when no compatible slot is free.
- **Trigger:** the facility has 7 EV slots. In the demo 2 are already occupied, so EVs keep arriving
  until the remaining 5 are filled (`F1-E01`, `F2-E01`, `F2-E02`, `F3-E01`, `F3-E02`).
- **Expected and observed:** the 6th arrival throws `SlotNotAvailableException`:
```
PARKING FULL ERROR: No compatible free slot available for vehicle type: EV
```
- `MainApp.testSlotFullExceptionScenario()` catches it and prints it. No ticket is issued and no
  incompatible slot is assigned; existing tickets remain unchanged.
