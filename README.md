# Smart Multi-Level Parking Allocation System
**Course:** Enterprise Web Applications (CSL273) | **Minor Project** | **Problem Statement 9**  
**Student Name:** Animesh Pasrija | **Roll No:** 25CSU028  
**Student Name:** Ayush Bhat | **Roll No:** 25CSU044

---

## 1. Project Overview & Scenario
This project is an enterprise-grade, console-based Java application implementing a **Smart Multi-Level Parking Allocation System**. The facility consists of multiple floors (Ground, Level 1, Level 2) with dedicated slot categories for **BIKE**, **CAR**, and **EV** (Electric Vehicles). 

Arriving vehicles from **three concurrent entry gates** (North, South, East) request slots simultaneously. The system guarantees that:
- Arriving vehicles are always allocated the **nearest available compatible parking slot** based on proximity scores.
- Under high concurrency, **no two vehicles are ever allocated the same slot** (Strict thread-safety and race-condition prevention).
- Vehicle checkouts, payments, and slot releases execute inside an **atomic JDBC database transaction** (`commit` / `rollback`).
- Loosely coupled architecture powered by **Spring Core annotation-based Dependency Injection** (`@Service`, `@Repository`, `@Qualifier`, `@Autowired`).
- Two billing strategies are provided: `RegularBillingService` and `WeekendBillingService`.
- Comprehensive reporting queries generate analytical reports for **occupancy**, **free-slot breakdown**, **vehicle-type utilization**, and **revenue**.

---

## 2. Technology Stack & Key Highlights
- **Programming Language:** Java 17+ (SE Runtime Environment)
- **Framework:** Spring Core 6.x (IoC Container, Annotation-based DI)
- **Database:** 
  - **Embedded In-Memory H2 Database** (Zero configuration, runs immediately on any machine)
  - **MySQL 8.x** (Configurable via `application.properties` with standard relational tables)
- **Persistence:** Pure JDBC with `PreparedStatement` & manual Transaction Management (`setAutoCommit(false)`, `commit()`, `rollback()`)
- **Concurrency Utilities:** `ReentrantLock`, `CountDownLatch`, `ExecutorService`, `AtomicInteger`, `ConcurrentHashMap`, `ConcurrentLinkedQueue`
- **Build & Execution:** Standalone Batch Scripts (`compile.bat`, `run.bat`) + standard Apache Maven (`pom.xml`)

---

## 3. Package & Directory Structure
```
SmartParkingSystem/
├── pom.xml                                     # Maven Project Configuration
├── README.md                                   # Complete Setup & Execution Guide
├── DESIGN_NOTE.md                              # 3-Page Architecture & Design Document
├── VIVA_PREPARATION.md                         # Viva Q&A & Live Modification Guide
├── EXECUTION_EVIDENCE.md                       # Verifiable Console Execution Outputs
│
├── sql/
│   ├── schema.sql                              # DDL: Tables for floors, slots, vehicles, tickets, payments
│   ├── data.sql                                # DML: Multi-floor layout & sample records
│   └── reporting_queries.sql                   # SQL reporting queries for occupancy & revenue
│
├── lib/                                        # Standalone Dependencies (Spring Core, H2, MySQL Connector)
│
├── scripts/
│   ├── compile.bat                             # One-click Java 17+ compilation script
│   └── run.bat                                 # Interactive & Demo execution script
│
└── src/
    └── main/
        ├── java/com/smartparking/
        │   ├── MainApp.java                    # Console CLI & Demonstration Entrypoint
        │   ├── config/
        │   │   └── AppConfig.java              # Spring @Configuration & Component Scanning
        │   ├── model/
        │   │   ├── Vehicle.java                # Vehicle entity
        │   │   ├── VehicleType.java            # Enum: BIKE, CAR, EV
        │   │   ├── ParkingSlot.java            # Slot entity (implements Comparable)
        │   │   ├── SlotType.java               # Enum: BIKE, CAR, EV
        │   │   ├── ParkingFloor.java           # Multi-level floor model
        │   │   ├── ParkingTicket.java          # Ticket model
        │   │   ├── TicketStatus.java           # Enum: ACTIVE, COMPLETED, CANCELLED
        │   │   └── Payment.java                # Payment transaction entity
        │   ├── repository/
        │   │   ├── TicketRepository.java       # Interface
        │   │   ├── TicketRepositoryImpl.java   # JDBC PreparedStatement & Transactional checkout
        │   │   ├── ParkingSlotRepository.java  # Slot persistence interface
        │   │   ├── ParkingSlotRepositoryImpl.java # Slot JDBC implementation
        │   │   ├── VehicleRepository.java      # Vehicle persistence interface
        │   │   ├── VehicleRepositoryImpl.java  # Vehicle JDBC implementation
        │   │   ├── ReportRepository.java       # Reporting queries interface
        │   │   └── ReportRepositoryImpl.java   # JDBC SQL aggregation reports
        │   ├── service/
        │   │   ├── BillingService.java         # Billing strategy interface (2 implementations)
        │   │   ├── RegularBillingService.java  # Weekday pricing implementation
        │   │   ├── WeekendBillingService.java  # Weekend peak surge pricing implementation
        │   │   ├── SlotAllocationService.java  # Thread-safe slot manager interface
        │   │   ├── SlotAllocationServiceImpl.java # TreeSet & ConcurrentHashMap allocation logic
        │   │   ├── ParkingService.java         # Main business service interface
        │   │   ├── ParkingServiceImpl.java     # Orchestration of vehicle admission & checkout
        │   │   └── ReportService.java          # Console reports formatter
        │   ├── concurrency/
        │   │   ├── EntryGateWorker.java        # Runnable entry gate thread
        │   │   └── SimulationRunner.java       # 3-Gate concurrent rush-hour simulator
        │   ├── exception/
        │   │   ├── ParkingException.java       # Base runtime domain exception
        │   │   ├── SlotNotAvailableException.java # Failed / exceptional test scenario
        │   │   ├── InvalidVehicleException.java# Duplicate / invalid vehicle exception
        │   │   └── TicketNotFoundException.java# Checkout ticket lookup failure
        │   └── util/
        │       ├── SlotProximityComparator.java# Proximity comparator for nearest slot
        │       └── DatabaseUtil.java           # JDBC connection provider & schema auto-loader
        └── resources/
            └── application.properties          # Database & system configuration
```

---

## 4. Setup & Running Instructions

### Method A: One-Click Windows Batch Scripts (Recommended)
1. Open PowerShell or Command Prompt.
2. Navigate to the project root directory:
   ```cmd
   cd SmartParkingSystem
   ```
3. Compile all source files:
   ```cmd
   scripts\compile.bat
   ```
4. **Run Live Web Dashboard (Browser UI on `http://localhost:8080`):**
   ```cmd
   scripts\run-web.bat
   ```
   *(This launches the server and automatically opens `http://localhost:8080` in your browser).*
5. Run the interactive console application:
   ```cmd
   scripts\run.bat
   ```
6. Run the automated end-to-end evaluation demo:
   ```cmd
   scripts\run.bat --demo
   ```

### Method B: Using Apache Maven (IDE / CLI)
1. Open terminal in the project directory:
   ```bash
   mvn clean compile
   ```
2. Run the application:
   ```bash
   mvn exec:java
   ```

---

## 5. Database Configuration (H2 vs MySQL)
Configuration is managed in `src/main/resources/application.properties`:

### Default Mode: Embedded H2 (Zero Setup Required)
Out-of-the-box, the application is pre-configured to run with an in-memory H2 database with MySQL compatibility mode:
```properties
db.mode=h2
h2.driver=org.h2.Driver
h2.url=jdbc:h2:mem:smart_parking_db;DB_CLOSE_DELAY=-1;MODE=MySQL
h2.user=sa
h2.password=
```
The application **automatically executes `sql/schema.sql` and `sql/data.sql` upon startup**, creating all tables, floor plans, and initial slots.

### Switching to MySQL Server
If you want to run against a local MySQL server:
1. Open `src/main/resources/application.properties`.
2. Change `db.mode=mysql`:
```properties
db.mode=mysql
mysql.driver=com.mysql.cj.jdbc.Driver
mysql.url=jdbc:mysql://localhost:3306/smart_parking_db?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
mysql.user=root
mysql.password=your_password
```
3. Run the application. The system will connect and populate `smart_parking_db` automatically.

---

## 6. Verification Against Project Guidelines Checklist
| Requirement | Status | Implementation Details |
|---|---|---|
| **Console-based (No Spring Boot/MVC/Hibernate)** | ✅ COMPLIANT | Pure Java SE Console + Spring Core IoC Container + Pure JDBC |
| **At least 3 Java Collection types with justification** | ✅ COMPLIANT | `TreeSet<ParkingSlot>`, `ConcurrentHashMap`, `EnumMap`, `ArrayList`, `ConcurrentLinkedQueue` |
| **Genuine Multithreading use case** | ✅ COMPLIANT | 3 concurrent gate threads (`Gate-1`, `Gate-2`, `Gate-3`) competing for shared slots via `ReentrantLock` |
| **Zero Double-Allocation Guarantee** | ✅ COMPLIANT | Concurrent verification audit proves 0 duplicate slot allocations |
| **JDBC with PreparedStatement** | ✅ COMPLIANT | All SQL queries in Repositories use parameterized `PreparedStatement` |
| **Database Transaction Handling** | ✅ COMPLIANT | Vehicle checkout & payment uses `setAutoCommit(false)`, `commit()`, and `rollback()` |
| **Spring Core Annotation-based DI** | ✅ COMPLIANT | `@Configuration`, `@ComponentScan`, `@Service`, `@Repository`, `@Autowired`, `@Qualifier` |
| **Interface with Two Implementations** | ✅ COMPLIANT | `BillingService` has `RegularBillingService` and `WeekendBillingService` |
| **Four Analytical Reports** | ✅ COMPLIANT | Occupancy Report, Free Slots Breakdown, Vehicle Utilization, and Revenue Report |
| **Failed / Exceptional Test Scenario** | ✅ COMPLIANT | `SlotNotAvailableException` thrown & handled gracefully when capacity is exhausted |
| **Viva & Live Modification Ready** | ✅ COMPLIANT | Step-by-step modification guide provided in `VIVA_PREPARATION.md` |
