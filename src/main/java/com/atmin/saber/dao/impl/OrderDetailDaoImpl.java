package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.OrderDetailDao;
import com.atmin.saber.dao.base.SqlConstants;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.util.DBConnection;
import com.atmin.saber.util.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OrderDetailDaoImpl implements OrderDetailDao {

    private final JdbcTemplate jdbcTemplate;

    public OrderDetailDaoImpl(DBConnection db) {
        this.jdbcTemplate = new JdbcTemplate(db);
    }

    @Override
    public List<OrderDetail> findByOrderId(int orderId) {
        String sql = "SELECT " + SqlConstants.ORDER_DETAIL_COLUMNS + " FROM order_details WHERE order_id = ?";
        return jdbcTemplate.query(sql, this::map, orderId);
    }

    private OrderDetail map(ResultSet rs) throws SQLException {
        OrderDetail d = new OrderDetail();
        d.setDetailId(rs.getInt("detail_id"));
        d.setOrderId(rs.getInt("order_id"));
        d.setId(rs.getInt("id"));
        d.setQuantity(rs.getInt("quantity"));
        d.setUnitPrice(rs.getBigDecimal("unit_price"));
        return d;
    }
}
