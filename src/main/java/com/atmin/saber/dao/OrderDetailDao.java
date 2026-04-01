package com.atmin.saber.dao;

import com.atmin.saber.model.OrderDetail;

import java.sql.Connection;
import java.util.List;

public interface OrderDetailDao {
    /**
     * Find all order detail rows for an order.
     */
    List<OrderDetail> findByOrderId(int orderId);

    /**
     * Find all order detail rows for an order using provided connection.
     * (For use within transactions)
     */
    List<OrderDetail> findByOrderId(Connection con, int orderId) throws java.sql.SQLException;
}
