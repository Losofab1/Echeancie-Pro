package com.mycompany.jsf.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentDisplayLabelsTest {

    @Test
    void translatesPaymentStatusesAndChannelsForBothPortals() {
        assertEquals("En attente", PaymentDisplayLabels.status("pending"));
        assertEquals("Approuvé", PaymentDisplayLabels.status("APPROVED"));
        assertEquals("Refusé", PaymentDisplayLabels.status("REJECTED"));
        assertEquals("Virement bancaire", PaymentDisplayLabels.channel("BANK_TRANSFER"));
        assertEquals("Mobile money", PaymentDisplayLabels.channel("MOBILE_MONEY"));
        assertEquals("Espèces", PaymentDisplayLabels.channel("CASH"));
    }

    @Test
    void preservesUnknownValuesAndProvidesLabelsForMissingValues() {
        assertEquals("CUSTOM", PaymentDisplayLabels.status("CUSTOM"));
        assertEquals("Non défini", PaymentDisplayLabels.status(null));
        assertEquals("OTHER", PaymentDisplayLabels.channel("OTHER"));
        assertEquals("Non précisé", PaymentDisplayLabels.channel(" "));
    }
}
