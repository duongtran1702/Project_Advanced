package com.atmin.saber.model.enums;

public enum OrderStatus {
    PENDING,
    COMPLETED,
    PAID,
    CANCELLED;

    /**
     * Staff flow mapping:
     * - PENDING    = Đã xác nhận (mới đặt, chờ xử lý)
     * - COMPLETED  = Hoàn thành (đã mang ra cho khách)
     * - PAID       = Đã thanh toán (terminal)
     */
    public boolean isStaffUpdatable() {
        return this == PENDING;
    }

    /**
     * Advance status for staff: PENDING -> COMPLETED.
     * @throws IllegalStateException if the status cannot be advanced by staff.
     */
    public OrderStatus nextForStaff() {
        return switch (this) {
            case PENDING -> COMPLETED;
            case COMPLETED, PAID, CANCELLED -> throw new IllegalStateException("Order is already completed or cancelled");
        };
    }
}

