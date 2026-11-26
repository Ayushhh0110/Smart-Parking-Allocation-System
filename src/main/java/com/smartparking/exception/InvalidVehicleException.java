package com.smartparking.exception;

/**
 * Thrown when a vehicle record has invalid data or attempts double entry.
 */
public class InvalidVehicleException extends ParkingException {
    public InvalidVehicleException(String message) {
        super(message);
    }
}
