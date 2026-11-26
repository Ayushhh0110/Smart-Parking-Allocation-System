package com.smartparking.repository;

import com.smartparking.model.Vehicle;
import java.util.Optional;

public interface VehicleRepository {
    void saveOrUpdate(Vehicle vehicle);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
}
