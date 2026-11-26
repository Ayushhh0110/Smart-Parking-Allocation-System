package com.smartparking.model;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model representing a parking floor/level in a multi-level car park.
 */
public class ParkingFloor {
    private int floorNumber;
    private String floorName;
    private final Map<String, ParkingSlot> slots; // slotId -> ParkingSlot

    public ParkingFloor() {
        this.slots = new ConcurrentHashMap<>();
    }

    public ParkingFloor(int floorNumber, String floorName) {
        this.floorNumber = floorNumber;
        this.floorName = floorName;
        this.slots = new ConcurrentHashMap<>();
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public void addSlot(ParkingSlot slot) {
        if (slot != null) {
            slots.put(slot.getSlotId(), slot);
        }
    }

    public ParkingSlot getSlot(String slotId) {
        return slots.get(slotId);
    }

    public Map<String, ParkingSlot> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public int getTotalCapacity() {
        return slots.size();
    }

    public long getAvailableCount() {
        return slots.values().stream().filter(ParkingSlot::isAvailable).count();
    }

    public long getAvailableCountByType(SlotType type) {
        return slots.values().stream()
                .filter(s -> s.isAvailable() && s.getSlotType() == type)
                .count();
    }

    @Override
    public String toString() {
        return String.format("Floor %d (%s) - Capacity: %d, Available: %d",
                floorNumber, floorName, getTotalCapacity(), getAvailableCount());
    }
}
