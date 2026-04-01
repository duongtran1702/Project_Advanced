package com.atmin.saber.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A simple JDBC template to reduce boilerplate code for common database operations.
 * This class is inspired by Spring's JdbcTemplate.
 */
public class JdbcTemplate {

    private final DBConnection db;

    public JdbcTemplate(DBConnection db) {
        this.db = db;
    }

    /**
     * Executes a query that returns a list of objects.
     *
     * @param sql       The SQL query to execute.
     * @param rowMapper The mapper to convert a ResultSet row to an object.
     * @param params    The parameters to be set on the PreparedStatement.
     * @param <T>       The type of the objects in the list.
     * @return A list of mapped objects.
     */
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... params) {
        List<T> results = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + e.getMessage(), e);
        }
        return results;
    }

    /**
     * Executes a query that returns a single optional object.
     *
     * @param sql       The SQL query to execute.
     * @param rowMapper The mapper to convert a ResultSet row to an object.
     * @param params    The parameters to be set on the PreparedStatement.
     * @param <T>       The type of the object.
     * @return An Optional containing the mapped object, or empty if no result.
     */
    public <T> Optional<T> queryForObject(String sql, RowMapper<T> rowMapper, Object... params) {
        List<T> results = query(sql, rowMapper, params);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            // This is a programming error, so we throw an exception.
            throw new IllegalStateException("Query returned more than one result.");
        }
        return Optional.of(results.get(0));
    }

    /**
     * Executes an update (INSERT, UPDATE, DELETE).
     *
     * @param sql    The SQL to execute.
     * @param params The parameters to be set on the PreparedStatement.
     * @return The number of rows affected.
     */
    public int update(String sql, Object... params) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParameters(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to set parameters on a PreparedStatement.
     *
     * @param ps     The PreparedStatement.
     * @param params The parameters.
     * @throws SQLException if a database access error occurs.
     */
    private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}

