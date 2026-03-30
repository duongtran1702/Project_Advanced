package com.atmin.saber.model.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole {
    ADMIN,
    STAFF,
    CUSTOMER;

    public static Optional<UserRole> fromString(String name) {
        if (name == null) return Optional.empty();
        return Arrays.stream(UserRole.values())
                .filter(role -> role.name().equalsIgnoreCase(name))
                .findFirst();
    }

}

