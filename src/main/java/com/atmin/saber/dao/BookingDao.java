package com.atmin.saber.dao;

import com.atmin.saber.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingDao {
    List<Booking> findAll();

    /**
     * Staff: list booking requests waiting for confirmation.
     */
    List<Booking> findPendingBookings();
    
    Optional<Booking> findById(String customerId, int pcId, LocalDateTime startTime);
    
    List<Booking> findByCustomerId(String customerId);
    
    List<Booking> findByPcId(int pcId);

    /**
     * Find customer's current ACTIVE booking (if any).
     */
    Optional<Booking> findActiveByCustomerId(String customerId);
    
    List<Booking> findActiveBookingsForPc(int pcId, LocalDateTime startTime, LocalDateTime endTime);
    
    void insert(Booking booking);
    
    void update(Booking booking);
    
    void delete(String customerId, int pcId, LocalDateTime startTime);
    
    boolean isPcAvailable(int pcId, LocalDateTime startTime, LocalDateTime endTime);
}
