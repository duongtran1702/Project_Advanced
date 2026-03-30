package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.UserDao;
import com.atmin.saber.model.User;
import com.atmin.saber.model.enums.UserRole;
import com.atmin.saber.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class UserDaoImpl implements UserDao {

    private final DBConnection db;

    public UserDaoImpl(DBConnection db) {
        this.db = Objects.requireNonNull(db, "db must not be null");
    }

    @Override
    public Optional<User> findByUsernameWithRoles(String username) {
        if (username == null) return Optional.empty();

        String sql = """
                SELECT u.user_id, u.username, u.password_hash, u.phone, u.fullname, u.balance, r.role_name,u.status
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.user_id
                LEFT JOIN roles r ON r.role_id = ur.role_id
                WHERE u.username = ?
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                User user = null;
                Set<UserRole> roles = new HashSet<>();

                while (rs.next()) {
                    if (user == null) {
                        user = mapUser(rs);
                    }

                    String roleName = rs.getString("role_name");
                    if (roleName != null) {
                        // Dùng optional để tránh lỗi nếu roleName không hợp lệ
                        UserRole.fromString(roleName).ifPresent(roles::add);
                    }
                }

                if (user == null) return Optional.empty();

                user.setRoles(roles);
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error when searching for user: " + username, e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getString("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));
        u.setFullname(rs.getString("fullname"));
        u.setBalance(Objects.requireNonNullElse(rs.getBigDecimal("balance"), BigDecimal.ZERO));
        u.setStatus(rs.getInt("status"));
        return u;
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return false;

        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("No user exists: " + username, e);
        }
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;

        String sql = "SELECT 1 FROM users WHERE phone = ? LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("No user exists with phone: " + phone, e);
        }
    }

    @Override
    public String createUser(String username, String passwordHash, String phone, String fullname) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("phone is required");
        }
        if (fullname == null || fullname.trim().isEmpty()) {
            throw new IllegalArgumentException("fullname is required");
        }

        String sqlInsertUser = "INSERT INTO users(username, password_hash, phone, fullname, balance) VALUES (?,?,?,?,?)";
        String sqlSelectUserId = "SELECT user_id FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlInsertUser)) {
                    ps.setString(1, username.trim());
                    ps.setString(2, passwordHash);
                    ps.setString(3, phone);
                    ps.setString(4, fullname.trim());
                    ps.setBigDecimal(5, BigDecimal.ZERO);
                    ps.executeUpdate();
                }

                String userId;
                try (PreparedStatement ps = conn.prepareStatement(sqlSelectUserId)) {
                    ps.setString(1, username.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Cannot read user_id after insert for username=" + username);
                        }
                        userId = rs.getString("user_id");
                    }
                }

                conn.commit();
                return userId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error while creating user", e);
        }
    }

    @Override
    public void assignDefaultCustomerRole(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId is required");
        }

        String sql = """
                INSERT INTO user_roles(user_id, role_id)
                VALUES (?, (SELECT role_id FROM roles WHERE role_name = 'CUSTOMER' LIMIT 1))
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.trim());
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new RuntimeException("Failed to assign default role CUSTOMER for userId=" + userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error while assigning default role", e);
        }
    }

    @Override
    public BigDecimal getBalance(String userId) {
        if (userId == null || userId.trim().isEmpty()) return BigDecimal.ZERO;
        String sql = "SELECT balance FROM users WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return BigDecimal.ZERO;
                BigDecimal b = rs.getBigDecimal("balance");
                return b == null ? BigDecimal.ZERO : b;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error while reading balance", e);
        }
    }

    @Override
    public void addBalance(Connection con, String userId, BigDecimal delta) {
        if (con == null) throw new IllegalArgumentException("Connection is required");
        if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException("userId is required");
        if (delta == null) delta = BigDecimal.ZERO;

        String sql = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, delta);
            ps.setString(2, userId.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error while updating balance", e);
        }
    }

    @Override
    public boolean deductBalanceIfEnough(Connection con, String userId, BigDecimal amount) {
        if (con == null) throw new IllegalArgumentException("Connection is required");
        if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException("userId is required");
        if (amount == null) amount = BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return true;

        String sql = "UPDATE users SET balance = balance - ? WHERE user_id = ? AND balance >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, userId.trim());
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("DB error while deducting balance", e);
        }
    }
}

