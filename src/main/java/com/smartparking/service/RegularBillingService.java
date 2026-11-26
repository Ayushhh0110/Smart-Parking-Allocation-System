package com.smartparking.service;

import com.smartparking.model.VehicleType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Standard Weekday Billing Implementation.
 * Injected through Spring DI.
 */
@Service("regularBillingService")
public class RegularBillingService implements BillingService {

    private static final double BIKE_RATE_PER_HOUR = 15.0;
    private static final double CAR_RATE_PER_HOUR  = 40.0;
    private static final double EV_RATE_PER_HOUR   = 30.0; // ₹10 green discount

    @Override
    public double calculateFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime == null || exitTime == null) {
            return 0.0;
        }

        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        if (minutes <= 0) {
            minutes = 1; // Minimum 1 minute charge
        }

        // Bill by hourly blocks (ceil to next hour, or minimum 1 hour)
        double hours = Math.ceil(minutes / 60.0);
        if (hours < 1.0) {
            hours = 1.0;
        }

        double rate = getBaseHourlyRate(vehicleType);
        return Math.round((hours * rate) * 100.0) / 100.0;
    }

    @Override
    public String getSchemeName() {
        return "Regular Weekday Tariff";
    }

    @Override
    public double getBaseHourlyRate(VehicleType vehicleType) {
        if (vehicleType == null) return CAR_RATE_PER_HOUR;
        switch (vehicleType) {
            case BIKE: return BIKE_RATE_PER_HOUR;
            case CAR:  return CAR_RATE_PER_HOUR;
            case EV:   return EV_RATE_PER_HOUR;
            default:   return CAR_RATE_PER_HOUR;
        }
    }
}
