package com.mycompany.jsf.security;

import java.util.Locale;
import java.util.Set;

/**
 * Valeurs de la colonne {@code payment_schedules.confirmation_status}, telles que
 * définies par la contrainte {@code payment_schedules_confirmation_status_check}.
 */
public final class ScheduleConfirmationStatus {

    public static final String AWAITING_CLIENT = "AWAITING_CLIENT";
    public static final String CONFIRMED = "CONFIRMED";
    private static final Set<String> ALLOWED = Set.of(AWAITING_CLIENT, CONFIRMED);

    private ScheduleConfirmationStatus() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isAllowed(String value) {
        return ALLOWED.contains(normalize(value));
    }
}
