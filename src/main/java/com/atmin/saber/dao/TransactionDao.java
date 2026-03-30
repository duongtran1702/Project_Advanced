package com.atmin.saber.dao;

import com.atmin.saber.model.Transaction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionDao {
    void insert(Connection con, Transaction tx);

    default void insert(Transaction tx) {
        throw new UnsupportedOperationException("Use insert(Connection, Transaction) inside a transaction");
    }

    List<Transaction> findByUserId(String userId, int limit);

    List<Transaction> findByUserIdAndRange(String userId, LocalDateTime from, LocalDateTime to);
}

