package com.atmin.saber.util;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A functional interface used by {@link JdbcTemplate} to map rows of a
 * {@link ResultSet} on a per-row basis. Implementations of this interface
 * perform the actual work of mapping each row to a result object.
 *
 * @param <T> the type of the result object
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Implementations must implement this method to map each row of data
     * in the ResultSet.
     *
     * @param rs     the ResultSet to map (pre-initialized for the current row)
     * @return the result object for the current row
     * @throws SQLException if a SQLException is encountered getting
     *                      column values (that is, there's no need to catch SQLException)
     */
    T mapRow(ResultSet rs) throws SQLException;
}

