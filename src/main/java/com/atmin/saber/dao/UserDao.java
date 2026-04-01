package com.atmin.saber.dao;

import com.atmin.saber.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Optional;

public interface UserDao {
    Optional<User> findByUsernameWithRoles(String username);

    Optional<User> findById(String userId);

    // ... existing code ...
    boolean existsByUsername(String username);

    // Kiểm tra phone đã tồn tại chưa
    boolean existsByPhone(String phone);

    /**
     * Tạo mới user (chỉ insert vào bảng user.
     * @return user_id (UUID dạng String) vừa tạo
     */
    String createUser(String username, String passwordHash, String phone, String fullname);

    /**
     * Gán role mặc định cho user sau khi đăng ký.
     */
    void assignDefaultCustomerRole(String userId);

    /**
     * Read current balance.
     */
    BigDecimal getBalance(String userId);

    /**
     * Add delta to balance (+ for topup, - for payment). No validation.
     * MUST be executed within an existing transaction.
     */
    void addBalance(Connection con, String userId, BigDecimal delta);

    /**
     * Subtract amount from balance, only if balance >= amount.
     * MUST be executed within an existing transaction.
     *
     * @return true if deducted, false if insufficient.
     */
    boolean deductBalanceIfEnough(Connection con, String userId, BigDecimal amount);


}

