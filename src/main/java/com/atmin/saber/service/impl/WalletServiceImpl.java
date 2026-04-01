package com.atmin.saber.service.impl;

import com.atmin.saber.dao.TransactionDao;
import com.atmin.saber.dao.UserDao;
import com.atmin.saber.model.Transaction;
import com.atmin.saber.model.enums.TransactionType;
import com.atmin.saber.service.WalletService;
import com.atmin.saber.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

public class WalletServiceImpl implements WalletService {

    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final DBConnection db;

    public WalletServiceImpl(UserDao userDao, TransactionDao transactionDao, DBConnection db) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
        this.transactionDao = Objects.requireNonNull(transactionDao, "transactionDao must not be null");
        this.db = Objects.requireNonNull(db, "db must not be null");
    }

    @Override
    public BigDecimal getBalance(String userId) {
        return userDao.getBalance(userId);
    }

    @Override
    public void topUp(String userId, BigDecimal amount, String description) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        try (Connection con = db.getConnection()) {
            boolean old = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                userDao.addBalance(con, userId, amount);

                Transaction tx = new Transaction();
                tx.setUserId(userId);
                tx.setAmount(amount);
                tx.setTransactionType(TransactionType.TOPUP);
                tx.setDescription(description == null ? "Wallet top-up" : description);
                tx.setCreatedAt(LocalDateTime.now());
                transactionDao.insert(con, tx);

                con.commit();
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            } catch (SQLException ex) {
                con.rollback();
                throw new RuntimeException("Top-up failed: " + ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(old);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Top-up failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean charge(String userId, BigDecimal amount, String description) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return true;

        try (Connection con = db.getConnection()) {
            boolean old = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                boolean ok = userDao.deductBalanceIfEnough(con, userId, amount);
                if (!ok) {
                    con.rollback();
                    return false;
                }

                Transaction tx = new Transaction();
                tx.setUserId(userId);
                tx.setAmount(amount);
                tx.setTransactionType(TransactionType.PAYMENT);
                tx.setDescription(description == null ? "Payment" : description);
                tx.setCreatedAt(LocalDateTime.now());
                transactionDao.insert(con, tx);

                con.commit();
                return true;
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            } catch (SQLException ex) {
                con.rollback();
                throw new RuntimeException("Charge failed: " + ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(old);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Charge failed: " + ex.getMessage(), ex);
        }
    }

}

