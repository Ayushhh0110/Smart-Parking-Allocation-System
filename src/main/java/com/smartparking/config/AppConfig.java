package com.smartparking.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring Core Configuration class.
 * Uses Annotation-Based Configuration and Component Scanning.
 * 
 * Problem Statement 9 - Requirement 5:
 * "Use Spring Core with annotation-based configuration and Dependency Injection.
 * At least one interface must have two implementations, with one implementation injected through Spring."
 */
@Configuration
@ComponentScan(basePackages = "com.smartparking")
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
public class AppConfig {
    // Component scanning will automatically discover and wire:
    // - ParkingServiceImpl -> @Service("parkingService")
    // - SlotAllocationServiceImpl -> @Service("slotAllocationService")
    // - RegularBillingService -> @Service("regularBillingService")
    // - WeekendBillingService -> @Service("weekendBillingService")
    // - TicketRepositoryImpl -> @Repository("ticketRepository")
    // - ParkingSlotRepositoryImpl -> @Repository("parkingSlotRepository")
    // - VehicleRepositoryImpl -> @Repository("vehicleRepository")
    // - ReportRepositoryImpl -> @Repository("reportRepository")
    // - ReportService -> @Service("reportService")
    // - SimulationRunner -> @Component
}
