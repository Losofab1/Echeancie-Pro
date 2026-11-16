package com.mycompany.jsf.security;

import entities.Installments;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTransactionAllocationTest {

    @Test
    void carriesAnExcessPaymentAcrossFollowingInstallments() {
        Installments first = installment(300);
        Installments second = installment(500);
        PaymentTransactionService.AllocationResult result =
                PaymentTransactionService.allocateAmount(
                        450, List.of(first, second), Map.of(first.getId(), 100L));

        assertEquals(200, result.allocations().get(0).amount());
        assertEquals(250, result.allocations().get(1).amount());
        assertEquals(0, result.creditAmount());
    }

    @Test
    void retainsUnallocatedExcessAsClientCredit() {
        Installments first = installment(300);
        Installments second = installment(500);
        PaymentTransactionService.AllocationResult result =
                PaymentTransactionService.allocateAmount(
                        1100, List.of(first, second), Map.of(first.getId(), 100L));

        assertEquals(200, result.allocations().get(0).amount());
        assertEquals(500, result.allocations().get(1).amount());
        assertEquals(400, result.creditAmount());
    }

    @Test
    void rejectsPaymentsWhenNoRemainingInstallmentCanReceiveThem() {
        Installments installment = installment(300);
        assertThrows(IllegalArgumentException.class,
                () -> PaymentTransactionService.allocateAmount(
                        10, List.of(installment), Map.of(installment.getId(), 300L)));
    }

    private Installments installment(long amount) {
        Installments installment = new Installments();
        installment.setId(UUID.randomUUID());
        installment.setAmount(amount);
        return installment;
    }
}
