# Smart Parking Allocation System - Viva & Live Modification Guide
**Course:** Enterprise Web Applications (CSL273) | **Minor Project** | **Problem Statement 9**  
**Student Name:** Animesh Pasrija | **Roll No:** 25CSU028  

This guide prepares you to answer every conceptual question the faculty might ask, and perform any of the 6 live modifications in under 2 minutes.

---

## Part 1: Core Concepts & Viva Q&A

### Q1: Why did you not use Spring Boot or Hibernate?
**Answer:** The guidelines explicitly state: *"Spring Boot, Spring MVC, Hibernate/JPA and frontend frameworks are not required. Develop a console-based Java application that integrates the Java Collections Framework, Multithreading, JDBC and Spring Core using a loosely coupled design."* We used pure **Spring Core** with `AnnotationConfigApplicationContext` for Dependency Injection, and raw **JDBC with `PreparedStatement`** for transparent database persistence and transaction management.

### Q2: What Java Collections did you use and why?
**Answer:**
1. **`TreeSet<ParkingSlot>`** (inside `availableSlotsMap`): Used for storing available slots per vehicle type. It is ordered by our custom `SlotProximityComparator`. Calling `pollFirst()` extracts the physically nearest slot in $O(\log N)$ time.
2. **`ConcurrentHashMap<String, ParkingSlot>`** (`vehicleToSlotMap`): Maps vehicle license plates to active slots for $O(1)$ fast concurrent lookup, verifying whether a vehicle is already parked inside.
3. **`ConcurrentLinkedQueue<Vehicle>`**: Lock-free FIFO queue used for gate arrivals ensuring thread-safe task processing across gates.
4. **`ConcurrentHashMap<Integer, ParkingFloor>`**: Floor-wise hierarchy lookup.
5. **`ConcurrentHashMap.newKeySet()`**: Thread-safe set used during concurrency audit to detect race conditions.

### Q3: How do you guarantee that two gates never allocate the same slot simultaneously?
**Answer:** In `SlotAllocationServiceImpl.java`, the critical allocation block is guarded by a fair `ReentrantLock` (`allocationLock = new ReentrantLock(true)`). When Gate 1 and Gate 2 arrive at the same millisecond, only one thread can acquire the lock. It removes the nearest slot from the `TreeSet` (`pollFirst()`), marks it `isAvailable = false`, registers it in `vehicleToSlotMap`, and updates the database before releasing the lock. When the second thread enters, that slot is already gone.

### Q4: Where did you implement Database Transactions?
**Answer:** In `TicketRepositoryImpl.java` inside `completeTicketAndRecordPaymentTransactional()`. During vehicle exit:
1. `conn.setAutoCommit(false)` begins the transaction.
2. We execute 3 related updates:
   - Update `parking_tickets` (exit time, fare, status = 'COMPLETED')
   - Insert into `payments` (ticket ID, amount, payment mode)
   - Update `parking_slots` (is_available = TRUE)
3. `conn.commit()` commits the atomic transaction.
4. If any error occurs, `conn.rollback()` restores the previous database state.

### Q5: How is Spring Core Dependency Injection utilized?
**Answer:**
- `@Configuration` and `@ComponentScan("com.smartparking")` in `AppConfig.java`.
- `@Service` on `ParkingServiceImpl`, `SlotAllocationServiceImpl`, `ReportService`.
- `@Repository` on `TicketRepositoryImpl`, `ParkingSlotRepositoryImpl`, `VehicleRepositoryImpl`, `ReportRepositoryImpl`.
- The interface `BillingService` has two implementations: `RegularBillingService` and `WeekendBillingService`.
- `ParkingServiceImpl` receives `BillingService` injected via `@Qualifier("regularBillingService")`.

---

## Part 2: Step-by-Step Live Modifications

During the evaluation, the faculty will ask you to perform **one live change**. Here are the 6 official examples with exact code changes:

---

### Modification 1: Change a Sorting Rule or Add a Secondary Comparator Condition
**Faculty prompt:** *"Change the slot allocation rule so that upper floors (Level 3 first) are allocated before lower floors, OR sort by slot ID in reverse."*

**Where to edit:** `src/main/java/com/smartparking/util/SlotProximityComparator.java`

**Code change:**
In `SlotProximityComparator.java`:
```java
// Original: Lower floor first (ascending)
int floorCmp = Integer.compare(s1.getFloorNumber(), s2.getFloorNumber());

// LIVE MODIFICATION: Upper floor first (descending)
int floorCmp = Integer.compare(s2.getFloorNumber(), s1.getFloorNumber());
```
Or to sort slot numbers in descending order:
```java
int distCmp = Integer.compare(s2.getProximityScore(), s1.getProximityScore());
```
Run `scripts\compile.bat` and `scripts\run.bat --demo`. Level 3 slots will be allocated first!

---

### Modification 2: Replace One Collection and Explain the Impact
**Faculty prompt:** *"Replace `TreeSet` with `PriorityQueue` or `LinkedList` for available slots, and explain the difference."*

**Where to edit:** `src/main/java/com/smartparking/service/SlotAllocationServiceImpl.java`

**Code change:**
In `SlotAllocationServiceImpl.java`, change:
```java
// Original:
private final Map<SlotType, TreeSet<ParkingSlot>> availableSlotsMap = new EnumMap<>(SlotType.class);
// In constructor:
availableSlotsMap.put(st, new TreeSet<>(new SlotProximityComparator()));
// In allocateNearestSlot:
ParkingSlot allocatedSlot = pool.pollFirst();
```
To:
```java
// Replacement with PriorityQueue:
import java.util.PriorityQueue;

private final Map<SlotType, PriorityQueue<ParkingSlot>> availableSlotsMap = new EnumMap<>(SlotType.class);
// In constructor:
availableSlotsMap.put(st, new PriorityQueue<>(new SlotProximityComparator()));
// In allocateNearestSlot:
ParkingSlot allocatedSlot = pool.poll(); // O(log N) heap removal
```
**Impact explanation for teacher:**
- `TreeSet` is backed by a Red-Black balanced binary search tree ($O(\log N)$ time, no duplicate elements allowed).
- `PriorityQueue` is backed by a binary heap array ($O(\log N)$ enqueue/dequeue, $O(1)$ peek). Both achieve identical nearest-slot ordering!

---

### Modification 3: Change the Number of Worker Threads or Identify a Race-Condition Point
**Faculty prompt:** *"Change the number of gate threads from 3 to 5, OR show where a race condition would occur if synchronization is removed."*

**1. To increase threads to 5 gates:**
In `src/main/java/com/smartparking/concurrency/SimulationRunner.java`:
```java
int gateCount = 5; // changed from 3 to 5
ExecutorService executor = Executors.newFixedThreadPool(gateCount);
CountDownLatch doneSignal = new CountDownLatch(gateCount);

// Add Gate-4 and Gate-5
executor.submit(new EntryGateWorker("Gate-4 (West)", parkingService, gate4Queue, startSignal, doneSignal, globallyAllocatedSlots, duplicateCounter, ticketsIssued));
executor.submit(new EntryGateWorker("Gate-5 (VIP)",  parkingService, gate5Queue, startSignal, doneSignal, globallyAllocatedSlots, duplicateCounter, ticketsIssued));
```

**2. To demonstrate the race-condition point:**
Show the faculty `SlotAllocationServiceImpl.java` at lines:
```java
allocationLock.lock(); // <-- RACE CONDITION POINT
try {
    // If this lock is commented out, two threads will poll the same TreeSet slot simultaneously!
```

---

### Modification 4: Add / Modify One PreparedStatement Query or Database Field
**Faculty prompt:** *"Add a `remarks` column or add a query to find all tickets issued by a specific gate."*

**Code change in `TicketRepositoryImpl.java`:**
Add this method:
```java
public List<ParkingTicket> findTicketsByGate(String gateName) {
    List<ParkingTicket> list = new ArrayList<>();
    String sql = "SELECT t.*, v.vehicle_type, v.owner_name, v.contact_number, " +
                 "s.floor_number, s.slot_number, s.slot_type, s.is_available, s.proximity_score " +
                 "FROM parking_tickets t " +
                 "JOIN vehicles v ON t.license_plate = v.license_plate " +
                 "JOIN parking_slots s ON t.slot_id = s.slot_id " +
                 "WHERE t.entry_gate = ?";

    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, gateName);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTicket(rs));
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return list;
}
```

---

### Modification 5: Switch the Injected Spring Implementation Using `@Qualifier`
**Faculty prompt:** *"Switch the default injected billing strategy from `regularBillingService` to `weekendBillingService` using Spring annotations."*

**Where to edit:** `src/main/java/com/smartparking/service/ParkingServiceImpl.java`

**Code change:**
In `ParkingServiceImpl.java`:
```java
// Original:
@Autowired
public ParkingServiceImpl(SlotAllocationService slotAllocationService,
                          TicketRepository ticketRepository,
                          VehicleRepository vehicleRepository,
                          @Qualifier("regularBillingService") BillingService billingService) {

// LIVE MODIFICATION: Change @Qualifier to weekendBillingService
@Autowired
public ParkingServiceImpl(SlotAllocationService slotAllocationService,
                          TicketRepository ticketRepository,
                          VehicleRepository vehicleRepository,
                          @Qualifier("weekendBillingService") BillingService billingService) {
```
Recompile (`scripts\compile.bat`). Spring Core will now automatically wire `WeekendBillingService` on application startup!

---

### Modification 6: Add One New Exceptional Case and Handle It Correctly
**Faculty prompt:** *"Add an exception when someone tries to park a vehicle that is already parked inside."*

**Answer:** We already have this implemented!
Show the faculty:
1. `src/main/java/com/smartparking/exception/InvalidVehicleException.java`
2. In `SlotAllocationServiceImpl.java`:
```java
if (vehicleToSlotMap.containsKey(plate)) {
    throw new InvalidVehicleException("Vehicle " + plate + " is already recorded as parked in the facility!");
}
```
3. In `MainApp.java`:
```java
catch (InvalidVehicleException ive) {
    System.err.println("Admission Refused: " + ive.getMessage());
}
```
Explain: *"This prevents the physical impossibility of a car entering a parking garage twice without ever leaving."*
