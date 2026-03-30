package com.atmin.saber.util;

import com.atmin.saber.config.DBConfig;

import java.sql.*;

public class DBConnection {
    private static class Helper {
        private static final DBConnection INSTANCE = new DBConnection();
    }

    private DBConnection() {
    }

    public static DBConnection getInstance() {
        return Helper.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DBConfig.URL, DBConfig.USER, DBConfig.PASSWORD);
    }
}