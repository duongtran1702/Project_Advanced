package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.OrderDetailDao;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailDaoImpl implements OrderDetailDao {

    private final DBConnection db;

    public OrderDetailDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public List<OrderDetail> findByOrderId(String orderId) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT detail_id, order_id, id, quantity, unit_price " +
                "FROM order_details WHERE order_id = ? ORDER BY detail_id";

        List<OrderDetail> details = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail d = new OrderDetail();
                    d.setDetailId(rs.getString("detail_id"));
                    d.setOrderId(rs.getString("order_id"));
                    d.setId(rs.getInt("id"));
                    d.setQuantity(rs.getInt("quantity"));
                    d.setUnitPrice(rs.getBigDecimal("unit_price"));
                    details.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load order details: " + e.getMessage(), e);
        }

        return details;
    }
}

