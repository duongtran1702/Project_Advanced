package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.BookingDao;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.enums.BookingStatus;
import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookingDaoImpl implements BookingDao {
    
    private final DBConnection db;
    
    public BookingDaoImpl(DBConnection db) {
        this.db = db;
    }
    
    @Override
    public List<Booking> findAll() {
        String sql = "SELECT customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee " +
                    "FROM bookings ORDER BY start_time DESC";
        List<Booking> bookings = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                bookings.add(mapBooking(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load bookings: " + e.getMessage(), e);
        }
        return bookings;
    }

    @Override
    public List<Booking> findPendingBookings() {
        String sql = "SELECT customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee " +
                "FROM bookings WHERE status = 'PENDING' ORDER BY start_time ASC";
        List<Booking> bookings = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                bookings.add(mapBooking(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pending bookings: " + e.getMessage(), e);
        }
        return bookings;
    }
    
    @Override
    public Optional<Booking> findById(String customerId, int pcId, LocalDateTime startTime) {
        String sql = "SELECT customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee " +
                    "FROM bookings WHERE customer_id = ? AND pc_id = ? AND start_time = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setInt(2, pcId);
            ps.setObject(3, startTime);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapBooking(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find booking: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Booking> findByCustomerId(String customerId) {
        String sql = "SELECT customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee " +
                    "FROM bookings WHERE customer_id = ? ORDER BY start_time DESC";
        List<Booking> bookings = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find bookings by customer: " + e.getMessage(), e);
        }
        return bookings;
    }

    @Override
    public Optional<Booking> findActiveByCustomerId(String customerId) {
        String sql = "SELECT customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee " +
                "FROM bookings WHERE customer_id = ? AND status = 'ACTIVE' ORDER BY start_time DESC LIMIT 1";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapBooking(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active booking: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Booking> findByPcId(int pcId) {
        String sql = "SELECT customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee " +
                    "FROM bookings WHERE pc_id = ? ORDER BY start_time DESC";
        List<Booking> bookings = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pcId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find bookings by PC: " + e.getMessage(), e);
        }
        return bookings;
    }
    
    @Override
    public List<Booking> findActiveBookingsForPc(int pcId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee " +
                    "FROM bookings WHERE pc_id = ? AND status IN ('PENDING', 'ACTIVE') AND " +
                    "((start_time <= ? AND expected_end_time > ?) OR " +
                    "(start_time < ? AND expected_end_time >= ?) OR " +
                    "(start_time >= ? AND expected_end_time <= ?))";
        List<Booking> bookings = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pcId);
            ps.setObject(2, startTime);
            ps.setObject(3, startTime);
            ps.setObject(4, endTime);
            ps.setObject(5, endTime);
            ps.setObject(6, startTime);
            ps.setObject(7, endTime);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active bookings: " + e.getMessage(), e);
        }
        return bookings;
    }
    
    @Override
    public void insert(Booking booking) {
        String sql = "INSERT INTO bookings(customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, booking.getCustomerId());
            ps.setInt(2, booking.getPcId());
            ps.setObject(3, booking.getStartTime());
            ps.setObject(4, booking.getExpectedEndTime());
            ps.setObject(5, booking.getActualEndTime());
            ps.setString(6, booking.getStatus().name());
            ps.setBigDecimal(7, booking.getTotalFee());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert booking: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void update(Booking booking) {
        String sql = "UPDATE bookings SET expected_end_time = ?, actual_end_time = ?, status = ?, total_fee = ? " +
                    "WHERE customer_id = ? AND pc_id = ? AND start_time = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, booking.getExpectedEndTime());
            ps.setObject(2, booking.getActualEndTime());
            ps.setString(3, booking.getStatus().name());
            ps.setBigDecimal(4, booking.getTotalFee());
            ps.setString(5, booking.getCustomerId());
            ps.setInt(6, booking.getPcId());
            ps.setObject(7, booking.getStartTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update booking: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void delete(String customerId, int pcId, LocalDateTime startTime) {
        String sql = "DELETE FROM bookings WHERE customer_id = ? AND pc_id = ? AND start_time = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setInt(2, pcId);
            ps.setObject(3, startTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete booking: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isPcAvailable(int pcId, LocalDateTime startTime, LocalDateTime endTime) {
        // Canonical overlap check: two intervals overlap iff existing.start < newEnd AND existing.end > newStart
        // If any match exists => NOT available
        String sql = "SELECT 1 FROM bookings " +
                "WHERE pc_id = ? AND status IN ('PENDING', 'ACTIVE') " +
                "AND start_time < ? AND expected_end_time > ? LIMIT 1";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pcId);
            ps.setObject(2, endTime);
            ps.setObject(3, startTime);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check PC availability: " + e.getMessage(), e);
        }
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
