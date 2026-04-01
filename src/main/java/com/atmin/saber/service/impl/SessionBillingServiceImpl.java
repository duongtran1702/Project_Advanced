package com.atmin.saber.service.impl;

import com.atmin.saber.dao.BookingDao;
import com.atmin.saber.dao.PcDao;
import com.atmin.saber.dao.UserDao;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.User;
import com.atmin.saber.model.enums.BookingStatus;
import com.atmin.saber.model.enums.PCStatus;
import com.atmin.saber.service.SessionBillingService;
import com.atmin.saber.service.WalletService;
import com.atmin.saber.util.PricingUtil;

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
    private final UserDao userDao;

    public SessionBillingServiceImpl(BookingDao bookingDao, PcDao pcDao, WalletService walletService, UserDao userDao) {
        this.bookingDao = Objects.requireNonNull(bookingDao, "bookingDao must not be null");
        this.pcDao = Objects.requireNonNull(pcDao, "pcDao must not be null");
        this.walletService = Objects.requireNonNull(walletService, "walletService must not be null");
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
    }

    @Override
    public void startBooking(String customerId, int pcId, LocalDateTime startTime) {
        // Check wallet before allowing to start
        BigDecimal balance = walletService.getBalance(customerId);
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Insufficient balance. Please top up before starting.");
        }

        PC pc = pcDao.findById(pcId).orElseThrow(() -> new IllegalArgumentException("PC not found"));
        if (pc.getStatus() != PCStatus.AVAILABLE) {
            throw new IllegalStateException("PC is not available to start. Current status: " + pc.getStatus());
        }

        // Walk-in booking creation
        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setPcId(pcId);
        booking.setStartTime(startTime);
        booking.setExpectedEndTime(startTime.plusDays(1)); // Arbitrary expected end time
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setTotalFee(BigDecimal.ZERO);
        bookingDao.insert(booking);

        // Mark PC in use
        pc.setStatus(PCStatus.IN_USE);
        pcDao.update(pc);
    }

    @Override
    public Optional<BigDecimal> stopActiveBookingAndCharge(String customerId) {
        Optional<Booking> activeOpt = bookingDao.findActiveByCustomerId(customerId);
        if (activeOpt.isEmpty()) return Optional.empty();

        Booking booking = activeOpt.get();
        LocalDateTime end = LocalDateTime.now();
        BigDecimal fee = calculateFeeForBooking(booking, end);

        // Thử trừ tổng nợ PC (Luồng 4)
        boolean charged = walletService.charge(customerId, fee, "PC session fee");
        if (!charged) {
            // Không đủ tiền trả toàn bộ => trừ tối đa số dư còn lại
            BigDecimal currentBalance = walletService.getBalance(customerId);
            if (currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                fee = currentBalance;
                walletService.charge(customerId, fee, "PC session fee (capped due to low balance)");
            } else {
                fee = BigDecimal.ZERO;
            }
        }

        // Đánh dấu kết thúc phiên
        booking.setActualEndTime(end);
        booking.setTotalFee(fee);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingDao.update(booking);

        // Giải phóng PC
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

        // Tính giá mỗi phút
        BigDecimal hourlyRate = PricingUtil.getHourlyRateForZone(resolveRoomName(booking.getPcId()));
        BigDecimal perMinute = hourlyRate.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (perMinute.compareTo(BigDecimal.ZERO) <= 0) return;

        // Tính nợ PC tạm tính
        BigDecimal currentDebt = calculateFeeForBooking(booking, LocalDateTime.now());

        // Lấy số dư từ ví
        BigDecimal balance = walletService.getBalance(customerId);
        if (balance == null) balance = BigDecimal.ZERO;

        // Tính toán trên RAM: Số dư khả dụng
        BigDecimal availableBalance = balance.subtract(currentDebt);

        if (availableBalance.compareTo(BigDecimal.ZERO) <= 0) {
            String username = getUsernameOrId(customerId);
            System.out.println("\n[AutoStop] Detected no available balance for customer: " + username + ". Initiating shutdown.");
            stopActiveBookingAndCharge(customerId);
        }
    }

    @Override
    public BigDecimal calculateCurrentDebt(String customerId) {
        if (customerId == null || customerId.isBlank()) return BigDecimal.ZERO;
        Optional<Booking> activeOpt = bookingDao.findActiveByCustomerId(customerId);
        if (activeOpt.isEmpty()) return BigDecimal.ZERO;

        Booking booking = activeOpt.get();
        if (booking.getStartTime() == null) return BigDecimal.ZERO;

        return calculateFeeForBooking(booking, LocalDateTime.now());
    }

    private BigDecimal calculateFeeForBooking(Booking booking, LocalDateTime endTime) {
        String roomName = resolveRoomName(booking.getPcId());
        BigDecimal hourlyRate = PricingUtil.getHourlyRateForZone(roomName);
        return calculateFee(booking.getStartTime(), endTime, hourlyRate);
    }

    private String resolveRoomName(int pcId) {
        return pcDao.findById(pcId).map(PC::getRoomName).orElse("");
    }

    private String getUsernameOrId(String customerId) {
        Optional<User> userOpt = userDao.findById(customerId);
        return userOpt.map(User::getUsername).orElse(customerId);
    }

    private static BigDecimal calculateFee(LocalDateTime start, LocalDateTime end, BigDecimal hourlyRate) {
        long seconds = ChronoUnit.SECONDS.between(start, end);
        long minutes = (long) Math.ceil(Math.max(1, seconds) / 60.0);
        BigDecimal perMinute = hourlyRate.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return perMinute.multiply(BigDecimal.valueOf(minutes));
    }
}

