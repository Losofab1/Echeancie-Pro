package com.mycompany.jsf.security;

import java.util.Locale;

public final class PaymentDisplayLabels {

    private PaymentDisplayLabels() {
    }

    public static String status(String status) {
        if (status == null || status.isBlank()) {
            return "Non défini";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case PaymentSubmissionStatus.PENDING -> "En attente";
            case PaymentSubmissionStatus.APPROVED -> "Approuvé";
            case PaymentSubmissionStatus.REJECTED -> "Refusé";
            default -> status;
        };
    }

    public static String confirmationStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Non défini";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case ScheduleConfirmationStatus.AWAITING_CLIENT -> "En attente de confirmation";
            case ScheduleConfirmationStatus.CONFIRMED -> "Confirmé";
            default -> status;
        };
    }

    public static String channel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "Non précisé";
        }
        return switch (channel.trim().toUpperCase(Locale.ROOT)) {
            case "BANK_TRANSFER" -> "Virement bancaire";
            case "MOBILE_MONEY" -> "Mobile money";
            case "CASH" -> "Espèces";
            case "CLIENT_PORTAL" -> "Portail client";
            default -> channel;
        };
    }
}
