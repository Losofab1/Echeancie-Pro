package com.mycompany.jsf.security;

import entities.Installments;
import entities.PaymentSubmissions;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentInstallmentDetailsTest {

    @Test
    void copiesApprovedPaymentDetailsOntoEachAllocatedInstallment() {
        Installments first = installment(1);
        Installments second = installment(2);
        PaymentSubmissions firstPayment = payment(first, "BANK_TRANSFER", "VIR-123");
        PaymentSubmissions secondPayment = payment(second, "MOBILE_MONEY", "MM-456");

        PaymentTransactionService.applyPaymentDetails(
                List.of(firstPayment, secondPayment),
                Map.of(first.getId(), first, second.getId(), second));

        assertEquals("BANK_TRANSFER", first.getChannel());
        assertEquals("VIR-123", first.getPaymentReference());
        assertEquals("MOBILE_MONEY", second.getChannel());
        assertEquals("MM-456", second.getPaymentReference());
    }

    @Test
    void ignoresPaymentsWhoseInstallmentIsNotInTheAllocation() {
        Installments allocated = installment(1);
        Installments unrelated = installment(2);
        PaymentSubmissions payment = payment(allocated, "CASH", "ESP-1");
        PaymentSubmissions stray = payment(unrelated, "CASH", "ESP-2");

        PaymentTransactionService.applyPaymentDetails(
                List.of(payment, stray), Map.of(allocated.getId(), allocated));

        assertEquals("ESP-1", allocated.getPaymentReference());
        assertNull(unrelated.getPaymentReference());
    }

    private Installments installment(int number) {
        Installments installment = new Installments();
        installment.setId(UUID.randomUUID());
        installment.setInstallmentNumber((short) number);
        return installment;
    }

    private PaymentSubmissions payment(Installments installment, String channel, String reference) {
        PaymentSubmissions payment = new PaymentSubmissions();
        payment.setInstallmentId(installment);
        payment.setChannel(channel);
        payment.setReference(reference);
        return payment;
    }
}
