package com.atmin.saber.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class HashUtil {
    private static final int BCRYPT_COST = 12;
    // Tạo 1 instance dùng lại (thread-safe)
    private static final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder(BCRYPT_COST);

    private HashUtil() {
    }

    public static String hashPassword(String password) {
        return encoder.encode(password);
    }

    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;

        try {
            return encoder.matches(password, storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        String hash1 = hashPassword("admin1702");
        System.out.println(hash1);
    }
}

