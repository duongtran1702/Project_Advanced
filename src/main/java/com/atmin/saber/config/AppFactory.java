package com.atmin.saber.config;

import com.atmin.saber.dao.TransactionDao;
import com.atmin.saber.dao.UserDao;
import com.atmin.saber.dao.impl.*;
import com.atmin.saber.service.*;
import com.atmin.saber.service.impl.*;
import com.atmin.saber.util.DBConnection;

/**
 * Centralized wiring to avoid duplicated createDefault() blocks across menus.
 * Keep it minimal: only what presentation layer needs.
 */
public final class AppFactory {
    private static final DBConnection DB = DBConnection.getInstance();

    private AppFactory() {
    }

    public static DBConnection db() {
        return DB;
    }

    public static ProductService productService() {
        return new ProductServiceImpl(new ProductDaoImpl(DB));
    }

    public static OrderService orderService() {
        return new OrderServiceImpl(new OrderDaoImpl(DB), new OrderDetailDaoImpl(DB), productService(), walletService());
    }

    public static WalletService walletService() {
        UserDao userDao = new UserDaoImpl(DB);
        TransactionDao txDao = new TransactionDaoImpl(DB);
        return new WalletServiceImpl(userDao, txDao, DB);
    }

    public static UserDao userDao() {
        return new UserDaoImpl(DB);
    }
}

