package com.atmin.saber.service;

import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.enums.OrderStatus;

import java.util.List;

import java.util.Map;

public interface OrderService {
    /**
     * Value object returned by {@link #createOrderForBooking(String, Booking, Map)}.
     * It contains the created order and its detail lines.
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
    OrderStatus advanceOrderStatusForStaff(int orderId);

    /**
     * Staff: advance status for ALL pending orders (FIFO by order_time ASC).
     * Each order advances exactly one step:
     * - PENDING -> PREPARING
     * - PREPARING -> SERVED
     *
     * @return number of orders successfully updated.
     */
    int advanceAllPendingOrdersForStaff();

    /**
     * Staff: Reject order (due to out of stock etc.).
     * Refunds customer's wallet and sets order status to CANCEL.
     */
    void rejectOrderAndRefund(int orderId);

    /**
     * Customer: view all orders sorted by order_time DESC.
     */
    List<Order> getAllOrdersOfCustomer(String customerId);

    /**
     * Customer: view details (items) for an order.
     */
    List<OrderDetail> getOrderDetails(int orderId);
}
