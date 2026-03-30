package com.atmin.saber.service;

import com.atmin.saber.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    BigDecimal getBalance(String userId);

    /**
     * Top up wallet (creates TOPUP transaction and adds balance).
     */
    void topUp(String userId, BigDecimal amount, String description);

    /**
     * Charge wallet (creates PAYMENT transaction and deducts balance if enough).
     *
     * @return true if charged, false if insufficient balance.
     */
    boolean charge(String userId, BigDecimal amount, String description);

    List<Transaction> getRecentTransactions(String userId, int limit);
}

