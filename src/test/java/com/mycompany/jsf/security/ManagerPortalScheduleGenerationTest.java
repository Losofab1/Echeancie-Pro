package com.mycompany.jsf.security;

import entities.Installments;
import entities.PaymentSchedules;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagerPortalScheduleGenerationTest {

    @Test
    void createsInstallmentsOnTheProposedDatesAndPreservesTheApprovedTotal() {
        PaymentSchedules schedule = new PaymentSchedules();
        List<LocalDate> proposedDates = List.of(
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2027, 3, 31));
        List<Installments> installments = ManagerPortalService.buildInstallments(
                schedule, 1000, proposedDates);

        assertEquals(3, installments.size());
        assertEquals(334, installments.get(0).getAmount());
        assertEquals(333, installments.get(1).getAmount());
        assertEquals(333, installments.get(2).getAmount());
        assertEquals(1000, installments.stream()
                .mapToLong(Installments::getAmount)
                .sum());
        assertEquals(LocalDate.of(2026, 2, 28),
                ((java.sql.Date) installments.get(0).getDueDate()).toLocalDate());
        assertEquals(LocalDate.of(2026, 3, 31),
                ((java.sql.Date) installments.get(1).getDueDate()).toLocalDate());
        assertEquals(LocalDate.of(2027, 3, 31),
                ((java.sql.Date) installments.get(2).getDueDate()).toLocalDate());
        assertFalse(installments.get(0).getPaid());
        assertSame(schedule, installments.get(0).getScheduleId());
    }

    @Test
    void rejectsSchedulesThatWouldContainZeroValueInstallments() {
        assertThrows(IllegalArgumentException.class,
                () -> ManagerPortalService.buildInstallments(
                        new PaymentSchedules(), 2, List.of(
                                LocalDate.of(2026, 2, 28),
                                LocalDate.of(2026, 3, 28),
                                LocalDate.of(2026, 4, 28))));
    }

    @Test
    void rejectsDatesThatAreNotStrictlyChronological() {
        assertThrows(IllegalArgumentException.class,
                () -> ManagerPortalService.buildInstallments(
                        new PaymentSchedules(), 100, List.of(
                                LocalDate.of(2026, 2, 28),
                                LocalDate.of(2026, 3, 27))));
    }
}
