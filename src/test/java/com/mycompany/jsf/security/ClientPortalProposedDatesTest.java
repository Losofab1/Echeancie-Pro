package com.mycompany.jsf.security;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientPortalProposedDatesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 31);

    @Test
    void acceptsMonthlyIntervalsAndPlansThatExtendBeyondOneYear() {
        assertDoesNotThrow(() -> ClientPortalService.validateProposedDueDates(
                List.of(
                        LocalDate.of(2026, 2, 28),
                        LocalDate.of(2027, 2, 28),
                        LocalDate.of(2028, 2, 28)),
                3, TODAY));
    }

    @Test
    void rejectsMissingDatesAndIntervalsOutsideTheAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientPortalService.validateProposedDueDates(null, 1, TODAY));
        assertThrows(IllegalArgumentException.class,
                () -> ClientPortalService.validateProposedDueDates(
                        List.of(LocalDate.of(2026, 2, 27)), 1, TODAY));
        assertThrows(IllegalArgumentException.class,
                () -> ClientPortalService.validateProposedDueDates(
                        List.of(LocalDate.of(2027, 2, 1)), 1, TODAY));
        assertThrows(IllegalArgumentException.class,
                () -> ClientPortalService.validateProposedDueDates(
                        List.of(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 27)),
                        2, TODAY));
        assertThrows(IllegalArgumentException.class,
                () -> ClientPortalService.validateProposedDueDates(
                        List.of(LocalDate.of(2026, 2, 28), LocalDate.of(2027, 3, 1)),
                        2, TODAY));
    }
}
