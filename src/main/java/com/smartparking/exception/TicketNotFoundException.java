package com.smartparking.exception;

/**
 * Thrown when an invalid or non-existent ticket number is presented for checkout.
 */
public class TicketNotFoundException extends ParkingException {
    public TicketNotFoundException(String ticketNumber) {
        super(String.format("Ticket '%s' not found or has already been completed.", ticketNumber));
    }
}
