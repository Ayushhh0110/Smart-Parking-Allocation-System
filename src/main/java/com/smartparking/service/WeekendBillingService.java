package com.smartparking.service;

import com.smartparking.model.VehicleType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Weekend / Peak Holiday Surcharge Billing Implementation.
 * Injected through Spring DI (e.g. via @Qualifier or profile).
 * 
 * Demonstrates Spring polymorphism and loose coupling:
 * In viva, students can switch the billing strategy with a single @Qualifier change!
 */
@Service("weekendBillingService")
public class WeekendBillingService implements BillingService {

    private static final double BIKE_RATE_PER_HOUR = 25.0; // Weekend rate
    private static final double CAR_RATE_PER_HOUR  = 60.0; // Weekend rate
    private static final double EV_RATE_PER_HOUR   = 45.0; // Weekend rate with EV incentive
    private static final double WEEKEND_SURCHARGE  = 10.0; // Flat facility maintenance fee

    @Override
    public double calculateFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime == null || exitTime == null) {
            return 0.0;
        }

        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        if (minutes <= 0) {
            minutes = 1;
        }

        double hours = Math.ceil(minutes / 60.0);
        if (hours < 1.0) {
            hours = 1.0;
        }

        double rate = getBaseHourlyRate(vehicleType);
        double total = (hours * rate) + WEEKEND_SURCHARGE;
        return Math.round(total * 100.0) / 100.0;
    }

    @Override
    public String getSchemeName() {
        return "Weekend Peak Surcharge (+Rs 10 Base Fee & Rush Hourly Rates)";
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
