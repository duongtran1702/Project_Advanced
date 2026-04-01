package com.atmin.saber.model;

import com.atmin.saber.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private int orderId;
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


    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
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
                ", customerId=" + customerId +
                ", orderTime=" + orderTime +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", discountCode='" + discountCode + '\'' +
                '}';
    }
}
