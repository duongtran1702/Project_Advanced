package com.atmin.saber.dao;

import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;

import java.sql.Connection;
import java.util.List;

public interface OrderDao {

    void insert(Connection con, Order order);

    void insertDetails(Connection con, int orderId, List<OrderDetail> details);

    /**
     * Staff: list orders that are waiting to be processed.
     * (PENDING / PREPARING)
     */
    List<Order> findPendingForStaff();

    /**
     * Customer: list all orders sorted by order_time DESC.
     */
    List<Order> findAllByCustomerId(String customerId);

    /**
     * Update order status.
     *
     * @return true if updated.
     */
    boolean updateStatus(int orderId, String newStatus);
}
