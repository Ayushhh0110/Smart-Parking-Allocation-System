package com.smartparking.exception;

/**
 * Base custom runtime exception for Smart Parking System domain errors.
 */
public class ParkingException extends RuntimeException {
    public ParkingException(String message) {
        super(message);
    }

    public ParkingException(String message, Throwable cause) {
        super(message, cause);
    }
}
