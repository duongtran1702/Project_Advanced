package com.atmin.saber.service;

import com.atmin.saber.model.Booking;
import com.atmin.saber.model.PC;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface BookingService {
    List<PC> getAvailablePCsByZone();
    
    List<PC> getAvailablePCsInZone(String roomName);
    
    boolean isPcAvailable(int pcId, LocalDateTime startTime, LocalDateTime endTime);
    
    Booking createBooking(String customerId, int pcId, LocalDateTime startTime, LocalDateTime expectedEndTime);
    
    List<Booking> getCustomerBookings(String customerId);

    /**
     * Staff: view all pending booking requests.
     */
    List<Booking> getPendingBookings();
    
    Booking getBookingDetails(String customerId, int pcId, LocalDateTime startTime);
    
    void cancelBooking(String customerId, int pcId, LocalDateTime startTime);
    
    Map<String, List<PC>> getAvailablePCsGroupedByZone();
}
