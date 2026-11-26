package com.smartparking.model;

/**
 * Enum representing physical parking slot types and compatibility rules.
 */
public enum SlotType {
    BIKE,
    CAR,
    EV;

    /**
     * Checks if this slot type is physically compatible with arriving vehicle type.
     * Compatibility Rules:
     * - BIKE slots only accommodate BIKE.
     * - CAR slots accommodate standard CARs.
     * - EV slots accommodate Electric Vehicles (equipped with EV charger).
     */
    public boolean isCompatibleWith(VehicleType vehicleType) {
        if (vehicleType == null) return false;
        switch (this) {
            case BIKE:
                return vehicleType == VehicleType.BIKE;
            case CAR:
                return vehicleType == VehicleType.CAR;
            case EV:
                return vehicleType == VehicleType.EV;
            default:
                return false;
        }
    }
}
