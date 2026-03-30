package com.atmin.saber.model.enums;

public enum OrderStatus {
    PENDING,
    PREPARING,
    SERVED,
    PAID;

    /**
     * Staff flow mapping:
     * - PENDING    = Đã xác nhận
     * - PREPARING  = Đang phục vụ
     * - SERVED     = Hoàn thành
     * - PAID       = Đã thanh toán (terminal)
     */
    public boolean isStaffUpdatable() {
        return this == PENDING || this == PREPARING;
    }

    /**
     * Advance status for staff: PENDING -> PREPARING -> SERVED.
     * @throws IllegalStateException if the status cannot be advanced by staff.
     */
    public OrderStatus nextForStaff() {
        return switch (this) {
            case PENDING -> PREPARING;
            case PREPARING -> SERVED;
            case SERVED, PAID -> throw new IllegalStateException("Order is already completed");
        };
    }
}

