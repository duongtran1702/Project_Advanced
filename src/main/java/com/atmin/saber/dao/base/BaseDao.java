package com.atmin.saber.dao.base;

import com.atmin.saber.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract base class for all DAO implementations.
 * Provides common methods for connection management and error handling.
 */
public abstract class BaseDao {

    protected final DBConnection db;

    protected BaseDao(DBConnection db) {
        this.db = db;
    }

    /**
     * Execute a query with a callback for processing ResultSet.
     * Handles connection management and error handling.
     * SQL parameters are always bound using PreparedStatement to prevent injection.
     */
    protected <T> T executeQuery(String sql, QueryCallback<T> callback, Object... params) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return callback.process(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute an update operation with optional parameter binding.
     * SQL parameters are always bound using PreparedStatement to prevent injection.
     */
    protected void executeUpdate(String sql, Object... params) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute an update operation within an existing transaction.
     * SQL parameters are always bound using PreparedStatement to prevent injection.
     */
    protected int executeUpdate(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            return ps.executeUpdate();
        }
    }

    /**
     * Set parameters for a prepared statement.
     * This ensures parameters are properly escaped and prevents SQL injection.
     */
    protected void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /**
     * Callback interface for processing query results.
     */
    @FunctionalInterface
    public interface QueryCallback<T> {
        T process(ResultSet rs) throws SQLException;
    }
}

