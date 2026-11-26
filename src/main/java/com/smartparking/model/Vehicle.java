package com.smartparking.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model representing an arriving vehicle.
 */
public class Vehicle {
    private String licensePlate;
    private VehicleType vehicleType;
    private String ownerName;
    private String contactNumber;
    private LocalDateTime registeredAt;

    public Vehicle() {
        this.registeredAt = LocalDateTime.now();
    }

    public Vehicle(String licensePlate, VehicleType vehicleType, String ownerName, String contactNumber) {
        this.licensePlate = (licensePlate != null) ? licensePlate.trim().toUpperCase() : null;
        this.vehicleType = vehicleType;
        this.ownerName = ownerName;
        this.contactNumber = contactNumber;
        this.registeredAt = LocalDateTime.now();
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = (licensePlate != null) ? licensePlate.trim().toUpperCase() : null;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(licensePlate, vehicle.licensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licensePlate);
    }

    @Override
    public String toString() {
        return String.format("[%s | %s | %s]", licensePlate, vehicleType, ownerName != null ? ownerName : "Visitor");
    }
}
