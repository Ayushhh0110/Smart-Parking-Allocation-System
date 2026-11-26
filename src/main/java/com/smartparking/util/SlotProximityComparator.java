package com.smartparking.util;

import com.smartparking.model.ParkingSlot;
import java.util.Comparator;

/**
 * Comparator implementing nearest-available-slot allocation logic.
 * 
 * VIVA / LIVE MODIFICATION POINT:
 * Can be easily modified during evaluation to change sorting behavior
 * (e.g. prioritize EV charging proximity, upper floors first, or custom distances).
 */
public class SlotProximityComparator implements Comparator<ParkingSlot> {

    @Override
    public int compare(ParkingSlot s1, ParkingSlot s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;

        // Primary criteria: Lower floor is closer to entry (Floor 1 < Floor 2 < Floor 3)
        int floorCmp = Integer.compare(s1.getFloorNumber(), s2.getFloorNumber());
        if (floorCmp != 0) {
            return floorCmp;
        }

        // Secondary criteria: Lower slot number / proximity score on the same floor
        int distCmp = Integer.compare(s1.getProximityScore(), s2.getProximityScore());
        if (distCmp != 0) {
            return distCmp;
        }

        // Tie-breaker: Alphabetical slotId
        return s1.getSlotId().compareTo(s2.getSlotId());
    }
}
