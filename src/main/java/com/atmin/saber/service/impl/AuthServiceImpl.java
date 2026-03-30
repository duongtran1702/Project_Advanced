package com.atmin.saber.service.impl;

import com.atmin.saber.dao.UserDao;
import com.atmin.saber.model.User;
import com.atmin.saber.service.AuthService;
import com.atmin.saber.util.HashUtil;

import java.util.Optional;

public class AuthServiceImpl implements AuthService {
    private final UserDao userDao;

    public AuthServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public Optional<User> login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null) {
            return Optional.empty();
        }
        Optional<User> userOpt = userDao.findByUsernameWithRoles(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        boolean ok = HashUtil.verifyPassword(password, user.getPasswordHash());
        return ok ? Optional.of(user) : Optional.empty();
    }

    @Override
    public Optional<User> register(String username, String password, String phone, String fullname) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        if (password.trim().length() < 6) {
            return Optional.empty();
        }

        if (fullname == null || fullname.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedUsername = username.trim();
        if (userDao.existsByUsername(normalizedUsername)) {
            return Optional.empty();
        }

        String normalizedPhone = phone.trim();
        if (userDao.existsByPhone(normalizedPhone)) {
            return Optional.empty();
        }

        String passwordHash = HashUtil.hashPassword(password);
        String userId = userDao.createUser(normalizedUsername, passwordHash, normalizedPhone, fullname);
        userDao.assignDefaultCustomerRole(userId);
        return userDao.findByUsernameWithRoles(normalizedUsername);
    }

    @Override
    public boolean existsUsername(String username) {
        return userDao.existsByUsername(username);
    }

    @Override
    public boolean existsPhone(String phone) {
        return userDao.existsByPhone(phone);
    }
}

