package com.atmin.saber.util;

import java.sql.Connection;

public class TestConnection {
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            if (conn != null) {
                System.out.println("Connected to the database successfully!");
            }
        } catch (Exception e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
        }
    }
}