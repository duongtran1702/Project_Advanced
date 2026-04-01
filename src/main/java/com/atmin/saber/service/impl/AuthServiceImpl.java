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
        if (isBlank(username) || isBlank(password)) return Optional.empty();

        return userDao.findByUsernameWithRoles(username.trim())
                .filter(user -> HashUtil.verifyPassword(password, user.getPasswordHash()));
    }

    @Override
    public Optional<User> register(String username, String password, String phone, String fullname) {
        if (isBlank(username) || isBlank(password) || isBlank(phone) || isBlank(fullname) || password.trim().length() < 6) {
            return Optional.empty();
        }

        String normalizedUsername = username.trim();
        if (userDao.existsByUsername(normalizedUsername)) return Optional.empty();

        String normalizedPhone = phone.trim();
        if (userDao.existsByPhone(normalizedPhone)) return Optional.empty();

        String passwordHash = HashUtil.hashPassword(password);
        String userId = userDao.createUser(normalizedUsername, passwordHash, normalizedPhone, fullname.trim());
        userDao.assignDefaultCustomerRole(userId);
        return userDao.findByUsernameWithRoles(normalizedUsername);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
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

