package com.smartparking.exception;

import com.smartparking.model.VehicleType;

/**
 * Exception thrown when an arriving vehicle cannot be allocated any compatible free slot.
 * Serves as the required failed/exceptional test scenario (Design Note requirement).
 */
public class SlotNotAvailableException extends ParkingException {
    private final VehicleType vehicleType;

    public SlotNotAvailableException(VehicleType vehicleType) {
        super(String.format("PARKING FULL ERROR: No compatible free slot available for vehicle type: %s", vehicleType));
        this.vehicleType = vehicleType;
    }

    public SlotNotAvailableException(String message, VehicleType vehicleType) {
        super(message);
        this.vehicleType = vehicleType;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }
}
