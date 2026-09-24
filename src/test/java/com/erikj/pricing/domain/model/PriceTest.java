package com.erikj.pricing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceTest {

    @Test
    void shouldRejectPriceWhenStartDateIsAfterEndDate() {
        LocalDateTime startDate =
                LocalDateTime.of(2020, 6, 15, 10, 0);

        LocalDateTime endDate =
                LocalDateTime.of(2020, 6, 14, 10, 0);

        assertThatThrownBy(() -> new Price(
                1L,
                35455L,
                1L,
                startDate,
                endDate,
                0,
                new BigDecimal("35.50"),
                Currency.getInstance("EUR")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startDate must not be after endDate");
    }

    @Test
    void shouldRejectPriceWhenRequiredFieldIsNull() {
        assertThatThrownBy(() -> new Price(
                null,
                35455L,
                1L,
                LocalDateTime.of(2020, 6, 14, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59, 59),
                0,
                new BigDecimal("35.50"),
                Currency.getInstance("EUR")
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("brandId must not be null");
    }
}