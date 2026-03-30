package com.atmin.saber.util;

import com.atmin.saber.model.User;
import com.atmin.saber.model.enums.UserRole;

import java.util.Optional;

public final class SessionContext {
    private static volatile User currentUser;
    private static volatile UserRole currentRole;

    private SessionContext() {
    }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clearCurrentUser() {
        currentUser = null;
    }

    public static Optional<UserRole> getCurrentRole() {
        return Optional.ofNullable(currentRole);
    }

    public static void setCurrentRole(UserRole userRole) {
        currentRole = userRole;
        System.out.println("\tLogged in as: " + userRole);

    }

    public static void clearCurrentRole() {
        currentRole = null;
    }

}

