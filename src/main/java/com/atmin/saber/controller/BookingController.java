package com.atmin.saber.controller;

import com.atmin.saber.dao.impl.BookingDaoImpl;
import com.atmin.saber.dao.impl.PcDaoImpl;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.PC;
import com.atmin.saber.service.BookingService;
import com.atmin.saber.service.SessionBillingService;
import com.atmin.saber.service.impl.BookingServiceImpl;
import com.atmin.saber.service.impl.SessionBillingServiceImpl;
import com.atmin.saber.service.impl.WalletServiceImpl;
import com.atmin.saber.util.DBConnection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BookingController {
    private final BookingService bookingService;
    private final SessionBillingService sessionBillingService;

    public BookingController(BookingService bookingService, SessionBillingService sessionBillingService) {
        this.bookingService = bookingService;
        this.sessionBillingService = sessionBillingService;
    }
    
    public static BookingController createDefault() {
        DBConnection db = DBConnection.getInstance();
        BookingService bookingService = new BookingServiceImpl( new PcDaoImpl(db));

        com.atmin.saber.dao.UserDao userDao = new com.atmin.saber.dao.impl.UserDaoImpl(db);
        com.atmin.saber.dao.TransactionDao txDao = new com.atmin.saber.dao.impl.TransactionDaoImpl(db);
        com.atmin.saber.service.WalletService walletService = new WalletServiceImpl(userDao, txDao, db);
        SessionBillingService sessionBillingService = new SessionBillingServiceImpl(new BookingDaoImpl(db), new PcDaoImpl(db), walletService);

        return new BookingController(bookingService, sessionBillingService);
    }
    
    public void showAvailablePCsByZone() {
        System.out.println("\n=== AVAILABLE PCS BY ZONE ===");
        
        Map<String, List<PC>> pcsByZone = bookingService.getAvailablePCsGroupedByZone();
        
        if (pcsByZone.isEmpty()) {
            System.out.println("No PCs available currently.");
            return;
        }
        
        for (Map.Entry<String, List<PC>> entry : pcsByZone.entrySet()) {
            System.out.println("\n" + entry.getKey().toUpperCase() + ":");
            System.out.println("+------+----------------------+--------------+");
            System.out.printf("| %-4s | %-20s | %-12s |%n", "ID", "NAME", "STATUS");
            System.out.println("+------+----------------------+--------------+");
            
            for (PC pc : entry.getValue()) {
                System.out.printf("| %-4d | %-20s | %-12s |%n",
                    pc.getPcId(), pc.getPcName(), "Available");
            }
            System.out.println("+------+----------------------+--------------+");
        }
    }
    


    


    public void startBooking(String customerId, int pcId, LocalDateTime startTime) {
        sessionBillingService.startBooking(customerId, pcId, startTime);
    }

    public java.util.Optional<java.math.BigDecimal> stopActiveBookingAndCharge(String customerId) {
        return sessionBillingService.stopActiveBookingAndCharge(customerId);
    }

    public java.util.Optional<Booking> getActiveBooking(String customerId) {
        return sessionBillingService.getActiveBookingOfCustomer(customerId);
    }
    
    public java.math.BigDecimal calculateCurrentDebt(String customerId) {
        return sessionBillingService.calculateCurrentDebt(customerId);
    }
}
