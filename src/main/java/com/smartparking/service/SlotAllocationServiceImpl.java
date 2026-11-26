package com.smartparking.service;

import com.smartparking.exception.InvalidVehicleException;
import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.model.ParkingFloor;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.util.SlotProximityComparator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-Safe Implementation of SlotAllocationService.
 * 
 * CORE COLLECTIONS USED & JUSTIFICATION:
 * 1. EnumMap<SlotType, TreeSet<ParkingSlot>>:
 *    - Maintained per slot type. Each TreeSet is ordered via SlotProximityComparator.
 *    - Allows O(log N) nearest slot retrieval (pollFirst) and sorted insertion upon exit.
 * 2. ConcurrentHashMap<String, ParkingSlot> (vehicleToSlotMap):
 *    - Maps licensePlate -> occupied ParkingSlot.
 *    - Provides O(1) concurrent lookup to verify whether a vehicle is currently parked.
 * 3. ConcurrentHashMap<String, ParkingSlot> (allSlotsById):
 *    - Registry of all physical slots in the building for O(1) indexed access.
 * 4. ConcurrentHashMap<Integer, ParkingFloor> (floorMap):
 *    - Multi-level floor representations for floor-wise capacity and reporting.
 * 
 * MULTITHREADING SAFETY:
 * - Uses a fair ReentrantLock (allocationLock) to synchronize slot allocation across
 *   multiple entry gates.
 * - Guarantees Requirement 3: "The same slot must never be allocated to two vehicles".
 */
@Service("slotAllocationService")
public class SlotAllocationServiceImpl implements SlotAllocationService, InitializingBean {

    private final ParkingSlotRepository slotRepository;

    // Collection 1: Available slots pool grouped by SlotType, sorted by proximity
    private final Map<SlotType, TreeSet<ParkingSlot>> availableSlotsMap = new EnumMap<>(SlotType.class);

    // Collection 2: Currently occupied slots set for tracking active bay occupancy
    private final Set<ParkingSlot> occupiedSlots = ConcurrentHashMap.newKeySet();

    // Collection 3: O(1) mapping for currently parked vehicle license plate -> assigned slot
    private final Map<String, ParkingSlot> vehicleToSlotMap = new ConcurrentHashMap<>();

    // Collection 3: O(1) lookup of all slots by slotId
    private final Map<String, ParkingSlot> allSlotsById = new ConcurrentHashMap<>();

    // Collection 4: Floor-wise structural map
    private final Map<Integer, ParkingFloor> floorMap = new ConcurrentHashMap<>();

    // Fair lock to ensure FIFO allocation fairness across entry gate threads
    private final ReentrantLock allocationLock = new ReentrantLock(true);

    @Autowired
    public SlotAllocationServiceImpl(ParkingSlotRepository slotRepository) {
        this.slotRepository = slotRepository;
        // Initialize sets for each slot type with proximity comparator
        for (SlotType st : SlotType.values()) {
            availableSlotsMap.put(st, new TreeSet<>(new SlotProximityComparator()));
        }
    }

    @Override
    public void afterPropertiesSet() {
        initializeFromRepository();
    }

    @Override
    public void initializeFromRepository() {
        allocationLock.lock();
        try {
            // Clear in-memory collections
            for (TreeSet<ParkingSlot> set : availableSlotsMap.values()) {
                set.clear();
            }
            occupiedSlots.clear();
            vehicleToSlotMap.clear();
            allSlotsById.clear();
            floorMap.clear();

            List<ParkingSlot> dbSlots = slotRepository.findAllSlots();
            for (ParkingSlot s : dbSlots) {
                allSlotsById.put(s.getSlotId(), s);

                // Add to floor map
                floorMap.computeIfAbsent(s.getFloorNumber(), fn -> new ParkingFloor(fn, "Level " + (fn - 1)))
                        .addSlot(s);

                // Partition into available vs occupied collections
                if (s.isAvailable()) {
                    availableSlotsMap.get(s.getSlotType()).add(s);
                } else {
                    occupiedSlots.add(s);
                }
            }
            System.out.println("[SlotAllocationService] Loaded " + dbSlots.size() + " parking slots across " + floorMap.size() + " floors.");
        } finally {
            allocationLock.unlock();
        }
    }

    /**
     * Requirement 2 & 3:
     * Support BIKE, CAR and EV slot compatibility.
     * Allocate the nearest available compatible slot.
     * Ensure the same slot is never allocated twice across concurrent threads.
     */
    @Override
    public ParkingSlot allocateNearestSlot(Vehicle vehicle) {
        if (vehicle == null || vehicle.getLicensePlate() == null) {
            throw new InvalidVehicleException("Vehicle information and license plate cannot be null.");
        }

        String plate = vehicle.getLicensePlate().toUpperCase();
        VehicleType vType = vehicle.getVehicleType();

        allocationLock.lock(); // CRITICAL SECTION START
        try {
            // Guard: Prevent same vehicle from entering twice simultaneously
            if (vehicleToSlotMap.containsKey(plate)) {
                throw new InvalidVehicleException("Vehicle " + plate + " is already recorded as parked in the facility!");
            }

            // Find compatible slot type
            SlotType targetSlotType = mapVehicleToSlotType(vType);
            TreeSet<ParkingSlot> pool = availableSlotsMap.get(targetSlotType);

            if (pool == null || pool.isEmpty()) {
                // Exceptional case: No compatible slot available
                throw new SlotNotAvailableException(vType);
            }

            // Retrieve and remove the nearest slot in O(log N)
            ParkingSlot allocatedSlot = pool.pollFirst();

            if (allocatedSlot == null || !allocatedSlot.isAvailable()) {
                throw new SlotNotAvailableException(vType);
            }

            // Mark occupied and track in occupiedSlots set
            allocatedSlot.occupy();
            occupiedSlots.add(allocatedSlot);

            // Record vehicle-to-slot mapping
            vehicleToSlotMap.put(plate, allocatedSlot);

            // Persist availability update in database
            slotRepository.updateSlotAvailability(allocatedSlot.getSlotId(), false);

            return allocatedSlot;

        } finally {
            allocationLock.unlock(); // CRITICAL SECTION END
        }
    }

    @Override
    public void releaseSlot(ParkingSlot slot, Vehicle vehicle) {
        if (slot == null) return;

        allocationLock.lock();
        try {
            slot.vacate();
            occupiedSlots.remove(slot);
            if (vehicle != null && vehicle.getLicensePlate() != null) {
                vehicleToSlotMap.remove(vehicle.getLicensePlate().toUpperCase());
            }

            // Return to available pool
            TreeSet<ParkingSlot> pool = availableSlotsMap.get(slot.getSlotType());
            if (pool != null) {
                pool.add(slot);
            }

            // Update in DB
            slotRepository.updateSlotAvailability(slot.getSlotId(), true);

        } finally {
            allocationLock.unlock();
        }
    }

    @Override
    public boolean isVehicleParked(String licensePlate) {
        if (licensePlate == null) return false;
        return vehicleToSlotMap.containsKey(licensePlate.trim().toUpperCase());
    }

    @Override
    public ParkingSlot getSlotForVehicle(String licensePlate) {
        if (licensePlate == null) return null;
        return vehicleToSlotMap.get(licensePlate.trim().toUpperCase());
    }

    @Override
    public Map<VehicleType, Integer> getAvailableSlotCounts() {
        allocationLock.lock();
        try {
            Map<VehicleType, Integer> map = new EnumMap<>(VehicleType.class);
            map.put(VehicleType.BIKE, availableSlotsMap.get(SlotType.BIKE).size());
            map.put(VehicleType.CAR, availableSlotsMap.get(SlotType.CAR).size());
            map.put(VehicleType.EV, availableSlotsMap.get(SlotType.EV).size());
            return map;
        } finally {
            allocationLock.unlock();
        }
    }

    @Override
    public Map<Integer, ParkingFloor> getFloors() {
        return Collections.unmodifiableMap(floorMap);
    }

    @Override
    public Set<ParkingSlot> getOccupiedSlots() {
        return Collections.unmodifiableSet(occupiedSlots);
    }

    private SlotType mapVehicleToSlotType(VehicleType vehicleType) {
        switch (vehicleType) {
            case BIKE: return SlotType.BIKE;
            case CAR:  return SlotType.CAR;
            case EV:   return SlotType.EV;
            default:   return SlotType.CAR;
        }
    }
}
