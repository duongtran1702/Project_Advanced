package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.StatisticsDao;
import com.atmin.saber.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StatisticsDaoImpl implements StatisticsDao {

    private final DBConnection db;

    public StatisticsDaoImpl(DBConnection db) {
        this.db = Objects.requireNonNull(db, "db must not be null");
    }

    @Override
    public Map<YearMonth, BigDecimal> sessionRevenueByMonth(int year) {
        // bookings revenue recognized by actual_end_time when COMPLETED
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT YEAR(actual_end_time) y, MONTH(actual_end_time) m, SUM(total_fee) total " +
                "FROM bookings WHERE status = 'COMPLETED' AND actual_end_time IS NOT NULL AND YEAR(actual_end_time) = ? " +
                "GROUP BY YEAR(actual_end_time), MONTH(actual_end_time) ORDER BY m";
        return queryYearMonthSum(sql, year);
    }

    @Override
    public Map<YearMonth, BigDecimal> fnbRevenueByMonth(int year) {
        // orders revenue recognized by order_time when PAID
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT YEAR(order_time) y, MONTH(order_time) m, SUM(total_amount) total " +
                "FROM orders WHERE status = 'PAID' AND YEAR(order_time) = ? " +
                "GROUP BY YEAR(order_time), MONTH(order_time) ORDER BY m";
        return queryYearMonthSum(sql, year);
    }

    @Override
    public Map<YearMonth, BigDecimal> topupByMonth(int year) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT YEAR(created_at) y, MONTH(created_at) m, SUM(amount) total " +
                "FROM transactions WHERE transaction_type = 'TOPUP' AND YEAR(created_at) = ? " +
                "GROUP BY YEAR(created_at), MONTH(created_at) ORDER BY m";
        return queryYearMonthSum(sql, year);
    }

    @Override
    public Map<YearMonth, BigDecimal> paymentByMonth(int year) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT YEAR(created_at) y, MONTH(created_at) m, SUM(amount) total " +
                "FROM transactions WHERE transaction_type = 'PAYMENT' AND YEAR(created_at) = ? " +
                "GROUP BY YEAR(created_at), MONTH(created_at) ORDER BY m";
        return queryYearMonthSum(sql, year);
    }

    private Map<YearMonth, BigDecimal> queryYearMonthSum(String sql, int year) {
        Map<YearMonth, BigDecimal> map = new HashMap<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int y = rs.getInt("y");
                    int m = rs.getInt("m");
                    BigDecimal total = rs.getBigDecimal("total");
                    map.put(YearMonth.of(y, m), total == null ? BigDecimal.ZERO : total);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load statistics: " + e.getMessage(), e);
        }
        return map;
    }
}

