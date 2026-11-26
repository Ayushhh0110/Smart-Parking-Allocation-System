package com.smartparking.model;

import java.util.Objects;

/**
 * Model representing an individual parking bay/slot.
 * Implements Comparable to allow natural sorting by proximity (nearest to entry).
 */
public class ParkingSlot implements Comparable<ParkingSlot> {
    private String slotId;
    private int floorNumber;
    private int slotNumber;
    private SlotType slotType;
    private volatile boolean isAvailable;
    private int proximityScore; // Calculated as: (floorNumber * 100) + slotNumber

    public ParkingSlot() {
        this.isAvailable = true;
    }

    public ParkingSlot(String slotId, int floorNumber, int slotNumber, SlotType slotType) {
        this.slotId = slotId;
        this.floorNumber = floorNumber;
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.isAvailable = true;
        this.proximityScore = (floorNumber * 100) + slotNumber;
    }

    public ParkingSlot(String slotId, int floorNumber, int slotNumber, SlotType slotType, boolean isAvailable, int proximityScore) {
        this.slotId = slotId;
        this.floorNumber = floorNumber;
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.isAvailable = isAvailable;
        this.proximityScore = proximityScore;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public synchronized void occupy() {
        this.isAvailable = false;
    }

    public synchronized void vacate() {
        this.isAvailable = true;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    public int getProximityScore() {
        return proximityScore;
    }

    public void setProximityScore(int proximityScore) {
        this.proximityScore = proximityScore;
    }

    /**
     * Natural ordering: by proximity score ascending (Level 1 closer than Level 2,
     * slot 1 closer than slot 10).
     */
    @Override
    public int compareTo(ParkingSlot other) {
        if (other == null) return -1;
        int cmp = Integer.compare(this.proximityScore, other.proximityScore);
        if (cmp != 0) return cmp;
        return this.slotId.compareTo(other.slotId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSlot that = (ParkingSlot) o;
        return Objects.equals(slotId, that.slotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId);
    }

    @Override
    public String toString() {
        return String.format("[Slot %s | L%d | %s | %s | Dist:%d]",
                slotId, floorNumber, slotType, isAvailable ? "FREE" : "OCCUPIED", proximityScore);
    }
}
