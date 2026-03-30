package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.TransactionDao;
import com.atmin.saber.model.Transaction;
import com.atmin.saber.model.enums.TransactionType;
import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionDaoImpl implements TransactionDao {

    private final DBConnection db;

    public TransactionDaoImpl(DBConnection db) {
        this.db = Objects.requireNonNull(db, "db must not be null");
    }

    @Override
    public void insert(Connection con, Transaction tx) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "INSERT INTO transactions(user_id, amount, transaction_type, description, created_at) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tx.getUserId());
            ps.setBigDecimal(2, tx.getAmount());
            ps.setString(3, normalizeTypeForDb(tx.getTransactionType()));
            ps.setString(4, tx.getDescription());
            ps.setObject(5, tx.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByUserId(String userId, int limit) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT transaction_id, user_id, amount, transaction_type, description, created_at " +
                "FROM transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";

        List<Transaction> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTx(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Transaction> findByUserIdAndRange(String userId, LocalDateTime from, LocalDateTime to) {
        //noinspection SqlDialectInspection,SqlNoDataSourceInspection
        String sql = "SELECT transaction_id, user_id, amount, transaction_type, description, created_at " +
                "FROM transactions WHERE user_id = ? AND created_at >= ? AND created_at < ? ORDER BY created_at DESC";

        List<Transaction> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setObject(2, from);
            ps.setObject(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTx(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions: " + e.getMessage(), e);
        }
        return list;
    }

    private Transaction mapTx(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(String.valueOf(rs.getInt("transaction_id")));
        t.setUserId(rs.getString("user_id"));
        t.setAmount(rs.getBigDecimal("amount"));

        String typeStr = rs.getString("transaction_type");
        t.setTransactionType(parseType(typeStr));

        t.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) t.setCreatedAt(ts.toLocalDateTime());
        return t;
    }

    private static TransactionType parseType(String dbValue) {
        if (dbValue == null) return TransactionType.PAYMENT;
        if (dbValue.equalsIgnoreCase("TOPUP")) return TransactionType.TOPUP;
        if (dbValue.equalsIgnoreCase("DEPOSIT")) return TransactionType.DEPOSIT;
        if (dbValue.equalsIgnoreCase("REFUND")) return TransactionType.REFUND;
        return TransactionType.PAYMENT;
    }

    private static String normalizeTypeForDb(TransactionType type) {
        if (type == null) return "PAYMENT";
        if (type == TransactionType.DEPOSIT) return "TOPUP";
        return type.name();
    }
}

