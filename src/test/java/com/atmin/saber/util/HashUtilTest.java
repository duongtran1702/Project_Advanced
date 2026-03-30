package com.atmin.saber.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void hashAndVerify_ok() {
        String password = "123456";
        String stored = HashUtil.hashPassword(password);

        assertNotNull(stored);
        // BCrypt hashes typically start with $2a$, $2b$ or $2y$
        assertTrue(stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
        assertTrue(HashUtil.verifyPassword(password, stored));
        assertFalse(HashUtil.verifyPassword("wrong", stored));
    }

    @Test
    void hash_shouldUseRandomSalt() {
        String p = "same";
        String h1 = HashUtil.hashPassword(p);
        String h2 = HashUtil.hashPassword(p);
        assertNotEquals(h1, h2);

        assertTrue(HashUtil.verifyPassword(p, h1));
        assertTrue(HashUtil.verifyPassword(p, h2));
    }

    @Test
    void verify_invalidFormat_false() {
        assertFalse(HashUtil.verifyPassword("a", null));
        assertFalse(HashUtil.verifyPassword(null, "$2b$12$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOpqrstuv"));
        assertFalse(HashUtil.verifyPassword("a", ""));
        assertFalse(HashUtil.verifyPassword("a", "sha256$1$a$b"));
        assertFalse(HashUtil.verifyPassword("a", "not-a-bcrypt-hash"));
    }
}

