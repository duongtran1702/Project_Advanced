package com.atmin.saber.dao;

import com.atmin.saber.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingDao {
    List<Booking> findAll();

    Optional<Booking> findById(String customerId, int pcId, LocalDateTime startTime);

    /**
     * Find customer's current ACTIVE booking (if any).
     */
    Optional<Booking> findActiveByCustomerId(String customerId);

    void insert(Booking booking);

    void update(Booking booking);

    void delete(String customerId, int pcId, LocalDateTime startTime);

}
