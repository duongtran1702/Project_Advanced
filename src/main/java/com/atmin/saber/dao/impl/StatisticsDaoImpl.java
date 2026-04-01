package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.StatisticsDao;
import com.atmin.saber.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
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
    public Map<LocalDate, BigDecimal> sessionRevenueByDay(YearMonth month) {
        // bookings revenue recognized by actual_end_time when COMPLETED
        String sql = "SELECT DATE(actual_end_time) d, SUM(total_fee) total " +
                "FROM bookings WHERE status = 'COMPLETED' AND actual_end_time IS NOT NULL " +
                "AND actual_end_time >= ? AND actual_end_time < ? " +
                "GROUP BY DATE(actual_end_time) ORDER BY d";
        return queryLocalDateSum(sql, month);
    }

    @Override
    public Map<LocalDate, BigDecimal> fnbRevenueByDay(YearMonth month) {
        // orders revenue recognized by order_time when PAID
        String sql = "SELECT DATE(order_time) d, SUM(total_amount) total " +
                "FROM orders WHERE status = 'PAID' " +
                "AND order_time >= ? AND order_time < ? " +
                "GROUP BY DATE(order_time) ORDER BY d";
        return queryLocalDateSum(sql, month);
    }

    @Override
    public Map<LocalDate, BigDecimal> topupByDay(YearMonth month) {
        String sql = "SELECT DATE(created_at) d, SUM(amount) total " +
                "FROM transactions WHERE transaction_type = 'TOPUP' " +
                "AND created_at >= ? AND created_at < ? " +
                "GROUP BY DATE(created_at) ORDER BY d";
        return queryLocalDateSum(sql, month);
    }

    @Override
    public Map<LocalDate, BigDecimal> paymentByDay(YearMonth month) {
        String sql = "SELECT DATE(created_at) d, SUM(amount) total " +
                "FROM transactions WHERE transaction_type = 'PAYMENT' " +
                "AND created_at >= ? AND created_at < ? " +
                "GROUP BY DATE(created_at) ORDER BY d";
        return queryLocalDateSum(sql, month);
    }

    private Map<LocalDate, BigDecimal> queryLocalDateSum(String sql, YearMonth month) {
        Objects.requireNonNull(month, "month must not be null");
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        LocalDate start = month.atDay(1);
        LocalDate endExclusive = month.plusMonths(1).atDay(1);
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(endExclusive.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate("d");
                    BigDecimal total = rs.getBigDecimal("total");
                    if (d == null) continue;
                    map.put(d.toLocalDate(), total == null ? BigDecimal.ZERO : total);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load statistics: " + e.getMessage(), e);
        }
        return map;
    }
}

