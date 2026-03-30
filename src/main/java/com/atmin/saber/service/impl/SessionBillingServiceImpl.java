package com.atmin.saber.service.impl;

import com.atmin.saber.dao.BookingDao;
import com.atmin.saber.dao.PcDao;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.enums.BookingStatus;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.service.SessionBillingService;
import com.atmin.saber.service.WalletService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

public class SessionBillingServiceImpl implements SessionBillingService {

    private final BookingDao bookingDao;
    private final PcDao pcDao;
    private final WalletService walletService;

    public SessionBillingServiceImpl(BookingDao bookingDao, PcDao pcDao, WalletService walletService) {
        this.bookingDao = Objects.requireNonNull(bookingDao, "bookingDao must not be null");
        this.pcDao = Objects.requireNonNull(pcDao, "pcDao must not be null");
        this.walletService = Objects.requireNonNull(walletService, "walletService must not be null");
    }

    @Override
    public void startBooking(String customerId, int pcId, LocalDateTime startTime) {
        Booking booking = bookingDao.findById(customerId, pcId, startTime)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING booking can be started");
        }

        PC pc = pcDao.findById(pcId).orElseThrow(() -> new IllegalArgumentException("PC not found"));
        if (pc.getStatus() != PCStatus.AVAILABLE && pc.getStatus() != PCStatus.BOOKED) {
            throw new IllegalStateException("PC is not available to start: " + pc.getStatus());
        }

        // Check wallet before allowing to start
        BigDecimal balance = walletService.getBalance(customerId);
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Insufficient balance. Please top up before starting.");
        }

        // Start session
        // Record real start_time at the moment staff starts the session
        booking.setStartTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.ACTIVE);
        // reset total fee while playing
        booking.setTotalFee(BigDecimal.ZERO);
        bookingDao.update(booking);

        pc.setStatus(PCStatus.IN_USE);
        pcDao.update(pc);
    }

    @Override
    public Optional<BigDecimal> stopActiveBookingAndCharge(String customerId) {
        Optional<Booking> activeOpt = bookingDao.findActiveByCustomerId(customerId);
        if (activeOpt.isEmpty()) return Optional.empty();

        Booking booking = activeOpt.get();
        LocalDateTime end = LocalDateTime.now();

        BigDecimal fee = calculateFee(booking.getStartTime(), end, getHourlyRateForZone(resolveRoomName(booking.getPcId())));

        // Insufficient funds => refuse to stop
        boolean charged = walletService.charge(customerId, fee, "PC session fee");
        if (!charged) {
            throw new IllegalStateException("Insufficient balance. Please top up before stopping the session.");
        }

        // Mark completed + set total_fee + actual_end_time
        booking.setActualEndTime(end);
        booking.setTotalFee(fee);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingDao.update(booking);

        // Release PC
        PC pc = pcDao.findById(booking.getPcId()).orElse(null);
        if (pc != null) {
            pc.setStatus(PCStatus.AVAILABLE);
            pcDao.update(pc);
        }

        return Optional.of(fee);
    }

    @Override
    public Optional<Booking> getActiveBookingOfCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) return Optional.empty();
        return bookingDao.findActiveByCustomerId(customerId);
    }

    @Override
    public void autoStopIfOutOfBalance(String customerId) {
        if (customerId == null || customerId.isBlank()) return;
        Optional<Booking> activeOpt = bookingDao.findActiveByCustomerId(customerId);
        if (activeOpt.isEmpty()) return;

        Booking booking = activeOpt.get();
        if (booking.getStartTime() == null) return;

        // pricing
        String roomName = resolveRoomName(booking.getPcId());
        BigDecimal hourlyRate = getHourlyRateForZone(roomName);
        BigDecimal perMinute = hourlyRate.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (perMinute.compareTo(BigDecimal.ZERO) <= 0) return;

        // used minutes so far (rounded up)
        LocalDateTime now = LocalDateTime.now();
        long seconds = ChronoUnit.SECONDS.between(booking.getStartTime(), now);
        long usedMinutes = (long) Math.ceil(Math.max(1, seconds) / 60.0);

        // how many minutes can be paid by current balance
        BigDecimal balance = walletService.getBalance(customerId);
        if (balance == null) balance = BigDecimal.ZERO;

        long payableMinutes = balance.divide(perMinute, 0, RoundingMode.FLOOR).longValue();
        if (payableMinutes <= 0) {
            // cannot pay even 1 minute => stop at 0 payable: set end to start and no charge
            forceCompleteWithoutCharge(booking);
            return;
        }

        if (usedMinutes <= payableMinutes) {
            // still covered
            return;
        }

        // Stop at the last payable minute boundary
        LocalDateTime payableEnd = booking.getStartTime().plusMinutes(payableMinutes);
        BigDecimal fee = perMinute.multiply(BigDecimal.valueOf(payableMinutes));

        boolean charged = walletService.charge(customerId, fee, "PC session fee (auto-stop)");
        if (!charged) {
            // race with other charge/topup; do nothing and retry next tick
            return;
        }

        booking.setActualEndTime(payableEnd);
        booking.setTotalFee(fee);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingDao.update(booking);

        // Release PC
        PC pc = pcDao.findById(booking.getPcId()).orElse(null);
        if (pc != null) {
            pc.setStatus(PCStatus.AVAILABLE);
            pcDao.update(pc);
        }

        System.out.println("[AutoStop] Session stopped for customer=" + customerId + ", charged=" + fee);
    }

    private void forceCompleteWithoutCharge(Booking booking) {
        booking.setActualEndTime(booking.getStartTime());
        booking.setTotalFee(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingDao.update(booking);

        PC pc = pcDao.findById(booking.getPcId()).orElse(null);
        if (pc != null) {
            pc.setStatus(PCStatus.AVAILABLE);
            pcDao.update(pc);
        }
        System.out.println("[AutoStop] Session stopped due to zero balance. customer=" + booking.getCustomerId());
    }

    private String resolveRoomName(int pcId) {
        // booking fee logic currently uses roomName mapping (atmin2/3...)
        // PC already has roomName in model (loaded via PcDaoImpl).
        return pcDao.findById(pcId).map(PC::getRoomName).orElse("");
    }

    private static BigDecimal calculateFee(LocalDateTime start, LocalDateTime end, BigDecimal hourlyRate) {
        long seconds = ChronoUnit.SECONDS.between(start, end);
        long minutes = (long) Math.ceil(Math.max(1, seconds) / 60.0);
        BigDecimal perMinute = hourlyRate.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return perMinute.multiply(BigDecimal.valueOf(minutes));
    }

    private static BigDecimal getHourlyRateForZone(String roomName) {
        if (roomName == null) return BigDecimal.valueOf(10000);
        return switch (roomName.toLowerCase()) {
            case "atmin2", "atmin5" -> BigDecimal.valueOf(20000);
            case "atmin3", "atmin6" -> BigDecimal.valueOf(30000);
            default -> BigDecimal.valueOf(10000);
        };
    }
}

