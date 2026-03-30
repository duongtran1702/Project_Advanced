package com.atmin.saber.dao;

import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface OrderDao {
    void insert(Order order);

    void insertDetails(String orderId, List<OrderDetail> details);

    void insert(Connection con, Order order);

    void insertDetails(Connection con, String orderId, List<OrderDetail> details);

    /**
     * Staff: list orders that are waiting to be processed.
     * (PENDING / PREPARING)
     */
    List<Order> findPendingForStaff();

    /**
     * Customer: get latest order (by order_time).
     */
    Optional<Order> findLatestByCustomerId(String customerId);

    /**
     * Customer: list all orders sorted by order_time DESC.
     */
    List<Order> findAllByCustomerId(String customerId);

    /**
     * Update order status.
     *
     * @return true if updated.
     */
    boolean updateStatus(String orderId, String newStatus);
}

