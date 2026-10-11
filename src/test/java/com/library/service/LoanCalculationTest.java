package com.library.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LoanCalculationTest {

    @Test
    @DisplayName("Should correctly calculate standard 14-day loan due date")
    void shouldCalculateDueDate() {
        LocalDate issueDate = LocalDate.of(2026, 10, 1);
        int loanDurationDays = 14;
        LocalDate dueDate = issueDate.plusDays(loanDurationDays);

        assertThat(dueDate).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(ChronoUnit.DAYS.between(issueDate, dueDate)).isEqualTo(14);
    }

    @Test
    @DisplayName("Should correctly determine overdue days when returned late")
    void shouldCalculateOverdueDays() {
        LocalDate dueDate = LocalDate.of(2026, 10, 15);
        LocalDate returnDate = LocalDate.of(2026, 10, 20);

        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        assertThat(overdueDays).isEqualTo(5);
    }

    @Test
    @DisplayName("Should report zero overdue days when returned on or before due date")
    void shouldReportZeroOverdueWhenOnTime() {
        LocalDate dueDate = LocalDate.of(2026, 10, 15);
        LocalDate onTimeReturn = LocalDate.of(2026, 10, 15);
        LocalDate earlyReturn = LocalDate.of(2026, 10, 10);

        long diffOnTime = Math.max(0, ChronoUnit.DAYS.between(dueDate, onTimeReturn));
        long diffEarly = Math.max(0, ChronoUnit.DAYS.between(dueDate, earlyReturn));

        assertThat(diffOnTime).isEqualTo(0);
        assertThat(diffEarly).isEqualTo(0);
    }

    @Test
    @DisplayName("Should compute overdue fine using BigDecimal precision")
    void shouldComputeOverdueFineWithBigDecimal() {
        long overdueDays = 7;
        BigDecimal dailyRate = new BigDecimal("5.00");
        BigDecimal calculatedFine = dailyRate.multiply(BigDecimal.valueOf(overdueDays));

        assertThat(calculatedFine).isEqualByComparingTo(new BigDecimal("35.00"));
    }

    @Test
    @DisplayName("Should compute partial payments and remaining balance properly")
    void shouldComputeFineBalance() {
        BigDecimal totalFine = new BigDecimal("50.00");
        BigDecimal paidAmount = new BigDecimal("20.00");
        BigDecimal remaining = totalFine.subtract(paidAmount);

        assertThat(remaining).isEqualByComparingTo(new BigDecimal("30.00"));
    }
}
