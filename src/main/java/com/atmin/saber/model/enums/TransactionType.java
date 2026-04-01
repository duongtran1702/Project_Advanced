package com.atmin.saber.model.enums;

public enum TransactionType {
    /**
     * Wallet top-up.
     * DB value: TOPUP
     */
    TOPUP,

    /**
     * Payment for services and products.
     * DB value: PAYMENT
     */
    PAYMENT,

    /**
     * Refund transaction.
     * DB value: REFUND
     */
    REFUND
}

