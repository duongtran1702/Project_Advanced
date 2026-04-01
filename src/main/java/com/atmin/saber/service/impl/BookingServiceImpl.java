package com.atmin.saber.service.impl;

import com.atmin.saber.dao.BookingDao;
import com.atmin.saber.dao.PcDao;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.enums.BookingStatus;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.service.BookingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookingServiceImpl implements BookingService {
    
    private final BookingDao bookingDao;
    private final PcDao pcDao;
    
    public BookingServiceImpl(BookingDao bookingDao, PcDao pcDao) {
        this.bookingDao = bookingDao;
        this.pcDao = pcDao;
    }
    
    @Override
    public List<PC> getAvailablePCsByZone() {
        return pcDao.findAllActive().stream()
                .filter(pc -> pc.getStatus() == PCStatus.AVAILABLE)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Booking> getPendingBookings() {
        return bookingDao.findPendingBookings();
    }
    
    @Override
    public List<PC> getAvailablePCsInZone(String roomName) {
        return pcDao.findAllActive().stream()
                .filter(pc -> pc.getStatus() == PCStatus.AVAILABLE && 
                             (roomName == null || roomName.isEmpty() || pc.getRoomName().equalsIgnoreCase(roomName)))
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public boolean isPcAvailable(int pcId, LocalDateTime startTime, LocalDateTime endTime) {
        PC pc = pcDao.findById(pcId).orElse(null);
        if (pc == null || pc.getStatus() != PCStatus.AVAILABLE) {
            return false;
        }
        
        return bookingDao.isPcAvailable(pcId, startTime, endTime);
    }
    
    @Override
    public Booking createBooking(String customerId, int pcId, LocalDateTime startTime, LocalDateTime expectedEndTime) {
        if (!isPcAvailable(pcId, startTime, expectedEndTime)) {
            throw new RuntimeException("PC is not available for the selected time slot");
        }
        
        PC pc = pcDao.findById(pcId).orElse(null);
        if (pc == null) {
            throw new RuntimeException("PC not found");
        }
        
        long minutes = ChronoUnit.MINUTES.between(startTime, expectedEndTime);
        if (minutes <= 0) {
            throw new RuntimeException("End time must be after start time");
        }

        BigDecimal hourlyRate = getHourlyRateForZone(pc.getRoomName());
        BigDecimal perMinute = hourlyRate.divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalFee = perMinute.multiply(BigDecimal.valueOf(minutes));
        
        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setPcId(pcId);
        booking.setStartTime(startTime);
        booking.setExpectedEndTime(expectedEndTime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalFee(totalFee);
        
        bookingDao.insert(booking);
        
        return booking;
    }
    
    @Override
    public List<Booking> getCustomerBookings(String customerId) {
        return bookingDao.findByCustomerId(customerId);
    }
    
    @Override
    public Booking getBookingDetails(String customerId, int pcId, LocalDateTime startTime) {
        return bookingDao.findById(customerId, pcId, startTime)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }
    
    @Override
    public void cancelBooking(String customerId, int pcId, LocalDateTime startTime) {
        Booking booking = bookingDao.findById(customerId, pcId, startTime)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() == BookingStatus.ACTIVE) {
            throw new RuntimeException("Cannot cancel active booking");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        bookingDao.update(booking);
    }
    
    @Override
    public Map<String, List<PC>> getAvailablePCsGroupedByZone() {
        List<PC> availablePCs = getAvailablePCsByZone();
        Map<String, List<PC>> groupedPCs = new LinkedHashMap<>();
        
        for (PC pc : availablePCs) {
            groupedPCs.computeIfAbsent(pc.getRoomName(), k -> new java.util.ArrayList<>()).add(pc);
        }
        
        return groupedPCs;
    }
    
    private BigDecimal getHourlyRateForZone(String roomName) {
        return switch (roomName.toLowerCase()) {
            case "atmin2", "atmin5" -> BigDecimal.valueOf(20000);
            case "atmin3", "atmin6" -> BigDecimal.valueOf(30000);
            default -> BigDecimal.valueOf(10000);
        };
    }
}
