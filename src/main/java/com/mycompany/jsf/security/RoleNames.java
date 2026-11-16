package com.mycompany.jsf.security;

import java.util.Locale;
import java.util.Set;

public final class RoleNames {

    public static final String ADMIN = "ADMIN";
    // Valeur stockée en base (contrainte users_role_check), partagée avec l'application echean.
    public static final String MANAGER = "MANAGER";
    public static final String CLIENT = "CLIENT";
    private static final Set<String> ALLOWED = Set.of(ADMIN, MANAGER, CLIENT);

    private RoleNames() {
    }

    public static String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isAllowed(String role) {
        return ALLOWED.contains(normalize(role));
    }
}
