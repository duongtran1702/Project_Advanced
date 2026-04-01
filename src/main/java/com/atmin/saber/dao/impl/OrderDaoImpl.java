package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.OrderDao;
import com.atmin.saber.dao.base.SqlConstants;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.enums.OrderStatus;
import com.atmin.saber.util.DBConnection;
import com.atmin.saber.util.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDaoImpl implements OrderDao {

    private final JdbcTemplate jdbcTemplate;

    public OrderDaoImpl(DBConnection db) {
        this.jdbcTemplate = new JdbcTemplate(db);
    }


    @Override
    public void insert(Connection con, Order order) {
        String sql = "INSERT INTO orders(booking_customer_id, booking_pc_id, booking_start_time, customer_id, order_time, status, total_amount, discount_code) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            // booking_* is optional
            String bookingCustomerId = order.getBookingCustomerId();
            Integer bookingPcId = order.getBookingPcId();
            java.time.LocalDateTime bookingStartTime = order.getBookingStartTime();

            ps.setString(1, bookingCustomerId);
            if (bookingPcId == null) {
                ps.setObject(2, null);
            } else {
                ps.setInt(2, bookingPcId);
            }
            ps.setObject(3, bookingStartTime);

            ps.setString(4, order.getCustomerId());
            ps.setObject(5, order.getOrderTime());
            ps.setString(6, order.getStatus().name());
            ps.setBigDecimal(7, order.getTotalAmount());
            ps.setString(8, order.getDiscountCode());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    order.setOrderId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order: " + e.getMessage(), e);
        }
    }

    @Override
    public void insertDetails(Connection con, int orderId, List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return; // Nothing to insert
        }
        String sql = "INSERT INTO order_details(order_id, id, quantity, unit_price) VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (OrderDetail d : details) {
                ps.setInt(1, orderId);
                ps.setInt(2, d.getId());
                ps.setInt(3, d.getQuantity());
                ps.setBigDecimal(4, d.getUnitPrice());
                ps.addBatch();
            }
            int[] result = ps.executeBatch();
            // Verify all details were inserted
            if (result.length != details.size()) {
                throw new RuntimeException("Expected " + details.size() + " inserts but got " + result.length);
            }
            for (int i = 0; i < result.length; i++) {
                if (result[i] != 1) {
                    throw new RuntimeException("Failed to insert order detail " + i);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order details: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> findPendingForStaff() {
        String sql = "SELECT " + SqlConstants.ORDER_COLUMNS + " FROM orders WHERE status = 'PENDING' ORDER BY order_time ASC";
        return jdbcTemplate.query(sql, this::mapOrder);
    }

    @Override
    public List<Order> findAllByCustomerId(String customerId) {
        String sql = "SELECT " + SqlConstants.ORDER_COLUMNS + " FROM orders WHERE customer_id = ? ORDER BY order_time DESC";
        return jdbcTemplate.query(sql, this::mapOrder, customerId);
    }

    @Override
    public boolean updateStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        return jdbcTemplate.update(sql, newStatus, orderId) > 0;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setBookingCustomerId(rs.getString("booking_customer_id"));
        Object pcObj = rs.getObject("booking_pc_id");
        if (pcObj != null) {
            o.setBookingPcId(rs.getInt("booking_pc_id"));
        }
        Timestamp bookingStartTs = rs.getTimestamp("booking_start_time");
        if (bookingStartTs != null) {
            o.setBookingStartTime(bookingStartTs.toLocalDateTime());
        }
        o.setCustomerId(rs.getString("customer_id"));
        Timestamp orderTimeTs = rs.getTimestamp("order_time");
        if (orderTimeTs != null) {
            o.setOrderTime(orderTimeTs.toLocalDateTime());
        } else {
            o.setOrderTime(LocalDateTime.now());
        }

        String statusStr = rs.getString("status");
        try {
            o.setStatus(OrderStatus.valueOf(statusStr));
        } catch (IllegalArgumentException ex) {
            o.setStatus(OrderStatus.PENDING);
        }

        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setDiscountCode(rs.getString("discount_code"));
        return o;
    }
}
