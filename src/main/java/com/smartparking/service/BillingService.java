package com.smartparking.service;

import com.smartparking.model.VehicleType;
import java.time.LocalDateTime;

/**
 * Interface defining the billing and parking fee calculation strategy.
 * 
 * Problem Statement 9 - Requirement 5:
 * Spring DI must be used, with at least two implementations:
 * 1. RegularBillingService (Standard weekday pricing)
 * 2. WeekendBillingService (Weekend surge pricing)
 */
public interface BillingService {

    /**
     * Calculates the fee based on vehicle type and duration.
     */
    double calculateFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime);

    /**
     * Returns the name of the billing scheme.
     */
    String getSchemeName();

    /**
     * Returns base hourly rate for a vehicle type.
     */
    double getBaseHourlyRate(VehicleType vehicleType);
}
