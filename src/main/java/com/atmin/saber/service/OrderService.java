package com.atmin.saber.service;

import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.enums.OrderStatus;

import java.util.List;

import java.util.Map;
import java.util.Optional;

public interface OrderService {
    /**
     * Create an F&B order linked to a booking, default status PENDING.
     * Inputs:
     * - customerId: current customer
     * - booking: booking to link
     * - items: productId -> quantity
     *
     * @return CreatedOrder (order + its details)
     */
    record CreatedOrder(Order order, List<OrderDetail> details) {
    }

    CreatedOrder createOrderForBooking(String customerId, Booking booking, Map<Integer, Integer> items);

    /**
     * Staff: list orders waiting to be processed (PENDING / PREPARING).
     */
    List<Order> getPendingOrdersForStaff();

    /**
     * Staff: advance order status (PENDING -> PREPARING -> SERVED).
     */
    OrderStatus advanceOrderStatusForStaff(String orderId);

    /**
     * Customer: view latest order (by order_time).
     */
    Optional<Order> getLatestOrderOfCustomer(String customerId);

    /**
     * Customer: view all orders sorted by order_time DESC.
     */
    List<Order> getAllOrdersOfCustomer(String customerId);

    /**
     * Customer: view details (items) for an order.
     */
    List<OrderDetail> getOrderDetails(String orderId);
}

