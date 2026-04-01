package com.atmin.saber.model;

import com.atmin.saber.model.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Booking {
    private String customerId;
    private int pcId;
    private LocalDateTime startTime;
    private LocalDateTime expectedEndTime;
    private LocalDateTime actualEndTime;
    private BookingStatus status;
    private BigDecimal totalFee;

    public Booking() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public int getPcId() {
        return pcId;
    }

    public void setPcId(int pcId) {
        this.pcId = pcId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getExpectedEndTime() {
        return expectedEndTime;
    }

    public void setExpectedEndTime(LocalDateTime expectedEndTime) {
        this.expectedEndTime = expectedEndTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "customerId='" + customerId + '\'' +
                ", pcId=" + pcId +
                ", startTime=" + startTime +
                ", expectedEndTime=" + expectedEndTime +
                ", actualEndTime=" + actualEndTime +
                ", status=" + status +
                ", totalFee=" + totalFee +
                '}';
    }
}

