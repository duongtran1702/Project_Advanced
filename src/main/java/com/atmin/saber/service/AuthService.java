package com.atmin.saber.service;

import com.atmin.saber.model.User;

import java.util.Optional;

public interface AuthService {

    /**
     * Đăng nhập hệ thống.
     * Load user theo username (kèm roles) từ DB
     * Verify password bằng BCrypt
     *
     * @return {@code Optional<User>} có dữ liệu nếu đăng nhập đúng, ngược lại {@code Optional.empty()}
     */
    Optional<User> login(String username, String password);

    /**
     * Đăng ký tài khoản mới. Sau khi tạo user sẽ gán role mặc định CUSTOMER.
     *
     * @return User vừa tạo (kèm roles) nếu đăng ký thành công
     */
    Optional<User> register(String username, String password, String phone, String fullname);

    boolean existsUsername(String username);

    boolean existsPhone(String phone);
}

