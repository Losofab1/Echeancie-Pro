package com.mycompany.jsf.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    public static final int BCRYPT_COST = 12;
    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern BCRYPT_HASH =
            Pattern.compile("^\\$2a\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private PasswordHasher() {
    }

    public static String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(BCRYPT_COST));
    }

    public static boolean isBcryptHash(String value) {
        return value != null && BCRYPT_HASH.matcher(value).matches();
    }

    public static boolean verify(char[] password, String storedValue) {
        if (password == null || storedValue == null || storedValue.isEmpty()) {
            return false;
        }
        if (isBcryptHash(storedValue)) {
            try {
                return BCrypt.checkpw(new String(password), storedValue);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        if (storedValue.startsWith("$2a$")
                || storedValue.startsWith("$2b$")
                || storedValue.startsWith("$2y$")) {
            return false;
        }

        byte[] candidate = new String(password).getBytes(StandardCharsets.UTF_8);
        byte[] stored = storedValue.getBytes(StandardCharsets.UTF_8);
        try {
            return MessageDigest.isEqual(candidate, stored);
        } finally {
            Arrays.fill(candidate, (byte) 0);
            Arrays.fill(stored, (byte) 0);
        }
    }
}
