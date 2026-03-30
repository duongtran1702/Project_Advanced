package com.atmin.saber.model;

import com.atmin.saber.model.enums.UserRole;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class User {
    private String userId;
    private String username;
    private String passwordHash;
    private String phone;
    private String fullname;
    private BigDecimal balance;
    private int status;
    private final Set<UserRole> roles = new HashSet<>();

    public User() {
    }

    public User(String userId, String username, String passwordHash, String phone, String fullname, BigDecimal balance, Set<UserRole> roles) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.fullname = fullname;
        this.balance = balance;
        setRoles(roles);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Set<UserRole> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void setRoles(Set<UserRole> roles) {
        this.roles.clear();
        if (roles != null) {
            this.roles.addAll(roles);
        }
    }

    public void addRole(UserRole role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void removeRole(UserRole role) {
        if (role != null) {
            this.roles.remove(role);
        }
    }

    public boolean hasRole(UserRole role) {
        return role != null && roles.contains(role);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", phone='" + phone + '\'' +
                ", fullname='" + fullname + '\'' +
                ", balance=" + balance +
                ", roles=" + roles +
                '}';
    }
}

