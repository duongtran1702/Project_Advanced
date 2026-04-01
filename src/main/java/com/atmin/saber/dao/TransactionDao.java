package com.atmin.saber.dao;

import com.atmin.saber.model.Transaction;

import java.sql.Connection;

public interface TransactionDao {
    void insert(Connection con, Transaction tx);

}

