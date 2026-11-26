package com.smartparking.model;

/**
 * Enum representing supported vehicle categories in the Smart Parking System.
 * Problem Statement 9: BIKE, CAR, EV.
 */
public enum VehicleType {
    BIKE("Two-Wheeler Motorcycle / Scooter"),
    CAR("Four-Wheeler Passenger Car / Sedan / SUV"),
    EV("Electric Vehicle (Requires Charging / Dedicated EV Slot)");

    private final String description;

    VehicleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
