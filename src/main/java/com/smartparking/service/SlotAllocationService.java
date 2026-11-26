package com.smartparking.service;

import com.smartparking.model.ParkingFloor;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Set;

/**
 * Service managing in-memory collections and concurrent slot allocation.
 * Problem Statement 9 - Requirements 1, 2, 3.
 */
public interface SlotAllocationService {

    /**
     * Atomically allocates the nearest compatible parking slot for an arriving vehicle.
     * Thread-safe against concurrent gate arrivals.
     */
    ParkingSlot allocateNearestSlot(Vehicle vehicle);

    /**
     * Releases a slot when a vehicle exits, returning it to the available pool.
     */
    void releaseSlot(ParkingSlot slot, Vehicle vehicle);

    /**
     * Checks if a vehicle is currently parked.
     */
    boolean isVehicleParked(String licensePlate);

    /**
     * Gets the slot where a vehicle is parked.
     */
    ParkingSlot getSlotForVehicle(String licensePlate);

    /**
     * Returns count of free slots per vehicle type.
     */
    Map<VehicleType, Integer> getAvailableSlotCounts();

    /**
     * Returns all floors in the facility.
     */
    Map<Integer, ParkingFloor> getFloors();

    /**
     * Returns an unmodifiable view of currently occupied parking slots.
     */
    Set<ParkingSlot> getOccupiedSlots();

    /**
     * Initializes slots from the database.
     */
    void initializeFromRepository();
}
