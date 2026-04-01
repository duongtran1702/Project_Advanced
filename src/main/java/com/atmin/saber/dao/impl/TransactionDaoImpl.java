package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.TransactionDao;
import com.atmin.saber.dao.base.BaseDao;
import com.atmin.saber.model.Transaction;
import com.atmin.saber.model.enums.TransactionType;
import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionDaoImpl extends BaseDao implements TransactionDao {

    public TransactionDaoImpl(DBConnection db) {
        super(db);
    }

    @Override
    public void insert(Connection con, Transaction tx) {
        String sql = "INSERT INTO transactions(user_id, amount, transaction_type, description, created_at) VALUES(?, ?, ?, ?, ?)";
        try {
            executeUpdate(con, sql, tx.getUserId(), tx.getAmount(), normalizeTypeForDb(tx.getTransactionType()),
                        tx.getDescription(), tx.getCreatedAt());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction: " + e.getMessage(), e);
        }
    }

    private static String normalizeTypeForDb(TransactionType type) {
        if (type == null) return "PAYMENT";
        return type.name();
    }
}
