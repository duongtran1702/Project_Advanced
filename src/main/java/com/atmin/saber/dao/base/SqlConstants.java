package com.atmin.saber.dao.base;

/**
 * SQL column constants to avoid duplication and ensure consistency.
 */
public final class SqlConstants {

    private SqlConstants() {
        // utility class
    }

    public static final String BOOKING_COLUMNS =
        "customer_id, pc_id, start_time, expected_end_time, actual_end_time, status, total_fee";

    public static final String PRODUCT_COLUMNS =
        "id, product_name, description, price, stock_quantity, category";

    public static final String ROOM_COLUMNS =
        "room_id, room_name";

    public static final String ORDER_COLUMNS =
        "order_id, booking_customer_id, booking_pc_id, booking_start_time, customer_id, order_time, status, total_amount, discount_code";

    public static final String ORDER_DETAIL_COLUMNS =
        "detail_id, order_id, id, quantity, unit_price";
}

