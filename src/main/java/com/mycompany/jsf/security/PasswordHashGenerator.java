package com.mycompany.jsf.security;

import java.io.Console;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class PasswordHashGenerator {

    private PasswordHashGenerator() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException(
                    "Run this tool from a system terminal so the password is not echoed.");
        }

        String name = console.readLine("Administrator name: ");
        String email = console.readLine("Administrator email: ");
        char[] password = console.readPassword("New password (minimum %d characters): ",
                PasswordHasher.MIN_PASSWORD_LENGTH);
        try {
            if (password == null || password.length < PasswordHasher.MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("The password is missing or too short.");
            }
            console.printf("%nCopy and run this SQL in pgAdmin. It contains only the BCrypt hash:%n%n%s%n",
                    createInsertSql(name, email, password));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    static String createInsertSql(String name, String email, char[] password) {
        String safeName = requireSingleLineValue(name, "Administrator name");
        String safeEmail = requireSingleLineValue(email, "Administrator email")
                .toLowerCase(Locale.ROOT);
        if (!safeEmail.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        String passwordHash = PasswordHasher.hash(password);
        return "INSERT INTO public.users "
                + "(id, name, email, password_hash, role, active, created_at) VALUES ("
                + sqlLiteral(UUID.randomUUID().toString()) + "::uuid, "
                + sqlLiteral(safeName) + ", "
                + sqlLiteral(safeEmail) + ", "
                + sqlLiteral(passwordHash) + ", 'ADMIN', TRUE, CURRENT_TIMESTAMP);";
    }

    private static String requireSingleLineValue(String value, String label) {
        if (value == null || value.isBlank()
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " is required and must be a single line.");
        }
        return value.trim();
    }

    private static String sqlLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
