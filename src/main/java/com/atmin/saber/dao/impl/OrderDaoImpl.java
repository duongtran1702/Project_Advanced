package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.OrderDao;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.enums.OrderStatus;
import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDaoImpl implements OrderDao {

    private final DBConnection db;

    public OrderDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public void insert(Order order) {
        try (Connection con = db.getConnection()) {
            insert(con, order);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order: " + e.getMessage(), e);
        }
    }

    @Override
    public void insertDetails(String orderId, List<OrderDetail> details) {
        if (details == null || details.isEmpty()) return;
        try (Connection con = db.getConnection()) {
            insertDetails(con, orderId, details);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order details: " + e.getMessage(), e);
        }
    }

    @Override
    public void insert(Connection con, Order order) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "INSERT INTO orders(order_id, booking_customer_id, booking_pc_id, booking_start_time, customer_id, order_time, status, total_amount, discount_code) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, order.getOrderId());

            // booking_* is optional
            String bookingCustomerId = order.getBookingCustomerId();
            Integer bookingPcId = order.getBookingPcId();
            java.time.LocalDateTime bookingStartTime = order.getBookingStartTime();

            ps.setString(2, bookingCustomerId);
            if (bookingPcId == null) {
                ps.setObject(3, null);
            } else {
                ps.setInt(3, bookingPcId);
            }
            ps.setObject(4, bookingStartTime);

            ps.setString(5, order.getCustomerId());
            ps.setObject(6, order.getOrderTime());
            ps.setString(7, order.getStatus().name());
            ps.setBigDecimal(8, order.getTotalAmount());
            ps.setString(9, order.getDiscountCode());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order: " + e.getMessage(), e);
        }
    }

    @Override
    public void insertDetails(Connection con, String orderId, List<OrderDetail> details) {
        if (details == null || details.isEmpty()) return;

        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "INSERT INTO order_details(detail_id, order_id, id, quantity, unit_price) VALUES(UUID(), ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (OrderDetail d : details) {
                ps.setString(1, orderId);
                ps.setInt(2, d.getId());
                ps.setInt(3, d.getQuantity());
                ps.setBigDecimal(4, d.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order details: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> findPendingForStaff() {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT order_id, booking_customer_id, booking_pc_id, booking_start_time, customer_id, order_time, status, total_amount, discount_code " +
                "FROM orders WHERE status IN ('PENDING','PREPARING') ORDER BY order_time ASC";

        List<Order> orders = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                orders.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pending orders: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public Optional<Order> findLatestByCustomerId(String customerId) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT order_id, booking_customer_id, booking_pc_id, booking_start_time, customer_id, order_time, status, total_amount, discount_code " +
                "FROM orders WHERE customer_id = ? ORDER BY order_time DESC LIMIT 1";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapOrder(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load customer order: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> findAllByCustomerId(String customerId) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT order_id, booking_customer_id, booking_pc_id, booking_start_time, customer_id, order_time, status, total_amount, discount_code " +
                "FROM orders WHERE customer_id = ? ORDER BY order_time DESC";

        List<Order> orders = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load customer orders: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public boolean updateStatus(String orderId, String newStatus) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update order status: " + e.getMessage(), e);
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getString("order_id"));
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

