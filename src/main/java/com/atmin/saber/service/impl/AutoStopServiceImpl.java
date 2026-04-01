package com.atmin.saber.service.impl;

import com.atmin.saber.dao.BookingDao;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.enums.BookingStatus;
import com.atmin.saber.service.AutoStopService;
import com.atmin.saber.service.SessionBillingService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Policy B: Do not allow the session to exceed the minutes the wallet can pay.
 * The worker checks every minute and stops sessions that ran out of balance.
 */
public class AutoStopServiceImpl implements AutoStopService {

    private final BookingDao bookingDao;
    private final SessionBillingService sessionBillingService;
    private final ScheduledExecutorService scheduler;

    public AutoStopServiceImpl(BookingDao bookingDao, SessionBillingService sessionBillingService) {
        this.bookingDao = Objects.requireNonNull(bookingDao, "bookingDao must not be null");
        this.sessionBillingService = Objects.requireNonNull(sessionBillingService, "sessionBillingService must not be null");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auto-stop-worker");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void start() {
        // initial delay 60s, then every 60s
        scheduler.scheduleAtFixedRate(this::tickSafe, 60, 60, TimeUnit.SECONDS);
    }



    private void tickSafe() {
        try {
            tick();
        } catch (RuntimeException ex) {
            // swallow to keep worker alive
            System.err.println("[AutoStop] Error: " + ex.getMessage());
        }
    }

    private void tick() {
        // naive scan: read all bookings and filter ACTIVE.
        // Works for console project size. If needed, add a DAO method findAllActive().
        List<Booking> activeBookings = bookingDao.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.ACTIVE)
                .toList();

        for (Booking b : activeBookings) {
            // Try to auto-stop by customer id; service will compute billable minutes and stop at payable limit.
            sessionBillingService.autoStopIfOutOfBalance(b.getCustomerId());
        }
    }
}

