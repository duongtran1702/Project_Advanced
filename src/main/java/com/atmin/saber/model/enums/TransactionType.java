package com.atmin.saber.model.enums;

public enum TransactionType {
    /**
     * Wallet top-up.
     * DB value: TOPUP
     */
    TOPUP,

    /**
     * Backward-compatible alias of TOPUP used by some older code.
     */
    DEPOSIT,
    PAYMENT,
    REFUND
}

