package com.atmin.saber.model;

import com.atmin.saber.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private String orderId;
    private String bookingId; // nullable
    // DB uses composite FK to bookings
    private String bookingCustomerId; // nullable
    private Integer bookingPcId; // nullable
    private LocalDateTime bookingStartTime; // nullable
    private String customerId;
    private LocalDateTime orderTime;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String discountCode; // nullable

    public Order() {
    }

    public Order(String orderId, String bookingId, String customerId, LocalDateTime orderTime, OrderStatus status,
                 BigDecimal totalAmount, String discountCode) {
        this.orderId = orderId;
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.orderTime = orderTime;
        this.status = status;
        this.totalAmount = totalAmount;
        this.discountCode = discountCode;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCustomerId() {
        return bookingCustomerId;
    }

    public void setBookingCustomerId(String bookingCustomerId) {
        this.bookingCustomerId = bookingCustomerId;
    }

    public Integer getBookingPcId() {
        return bookingPcId;
    }

    public void setBookingPcId(Integer bookingPcId) {
        this.bookingPcId = bookingPcId;
    }

    public LocalDateTime getBookingStartTime() {
        return bookingStartTime;
    }

    public void setBookingStartTime(LocalDateTime bookingStartTime) {
        this.bookingStartTime = bookingStartTime;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", bookingId=" + bookingId +
                ", customerId=" + customerId +
                ", orderTime=" + orderTime +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", discountCode='" + discountCode + '\'' +
                '}';
    }
}

