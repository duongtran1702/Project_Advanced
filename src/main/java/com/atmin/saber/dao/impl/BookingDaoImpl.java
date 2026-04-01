package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.BookingDao;
import com.atmin.saber.dao.base.BaseDao;
import com.atmin.saber.dao.base.SqlConstants;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.enums.BookingStatus;
import com.atmin.saber.util.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookingDaoImpl extends BaseDao implements BookingDao {

    public BookingDaoImpl(DBConnection db) {
        super(db);
    }
    
    @Override
    public List<Booking> findAll() {
        String sql = "SELECT " + SqlConstants.BOOKING_COLUMNS + " FROM bookings ORDER BY start_time DESC";
        return executeQuery(sql, rs -> {
            List<Booking> bookings = new ArrayList<>();
            while (rs.next()) {
                bookings.add(mapBooking(rs));
            }
            return bookings;
        });
    }

    @Override
    public Optional<Booking> findById(String customerId, int pcId, LocalDateTime startTime) {
        String sql = "SELECT " + SqlConstants.BOOKING_COLUMNS + " FROM bookings WHERE customer_id = ? AND pc_id = ? AND start_time = ?";
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return Optional.of(mapBooking(rs));
            }
            return Optional.empty();
        }, customerId, pcId, startTime);
    }

    @Override
    public Optional<Booking> findActiveByCustomerId(String customerId) {
        String sql = "SELECT " + SqlConstants.BOOKING_COLUMNS + " FROM bookings WHERE customer_id = ? AND status = 'ACTIVE' ORDER BY start_time DESC LIMIT 1";
        return executeQuery(sql, rs -> {
            if (rs.next()) return Optional.of(mapBooking(rs));
            return Optional.empty();
        }, customerId);
    }

    @Override
    public void insert(Booking booking) {
        String sql = "INSERT INTO bookings(customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?)";
        executeUpdate(sql, booking.getCustomerId(), booking.getPcId(), booking.getStartTime(),
                    booking.getExpectedEndTime(), booking.getActualEndTime(), booking.getStatus().name(), booking.getTotalFee());
    }
    
    @Override
    public void update(Booking booking) {
        String sql = "UPDATE bookings SET expected_end_time = ?, actual_end_time = ?, status = ?, total_fee = ? " +
                    "WHERE customer_id = ? AND pc_id = ? AND start_time = ?";
        executeUpdate(sql, booking.getExpectedEndTime(), booking.getActualEndTime(), booking.getStatus().name(),
                    booking.getTotalFee(), booking.getCustomerId(), booking.getPcId(), booking.getStartTime());
    }
    
    @Override
    public void delete(String customerId, int pcId, LocalDateTime startTime) {
        String sql = "DELETE FROM bookings WHERE customer_id = ? AND pc_id = ? AND start_time = ?";
        executeUpdate(sql, customerId, pcId, startTime);
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking booking = new Booking();
        booking.setCustomerId(rs.getString("customer_id"));
        booking.setPcId(rs.getInt("pc_id"));
        booking.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        booking.setExpectedEndTime(rs.getTimestamp("expected_end_time").toLocalDateTime());
        
        java.sql.Timestamp actualEndTimeTs = rs.getTimestamp("actual_end_time");
        if (actualEndTimeTs != null) {
            booking.setActualEndTime(actualEndTimeTs.toLocalDateTime());
        }
        
        String statusStr = rs.getString("status");
        try {
            booking.setStatus(BookingStatus.valueOf(statusStr));
        } catch (IllegalArgumentException ex) {
            booking.setStatus(BookingStatus.PENDING);
        }
        
        booking.setTotalFee(rs.getBigDecimal("total_fee"));
        return booking;
    }
}
