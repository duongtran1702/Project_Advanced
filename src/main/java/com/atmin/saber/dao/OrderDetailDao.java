package com.atmin.saber.dao;

import com.atmin.saber.model.OrderDetail;

import java.util.List;

public interface OrderDetailDao {
    /**
     * Find all order detail rows for an order.
     */
    List<OrderDetail> findByOrderId(int orderId);
}
