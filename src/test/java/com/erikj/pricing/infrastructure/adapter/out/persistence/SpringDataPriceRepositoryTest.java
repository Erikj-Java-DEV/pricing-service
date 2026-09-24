package com.erikj.pricing.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SpringDataPriceRepositoryTest {

    @Autowired
    private SpringDataPriceRepository repository;

    @Test
    void shouldReturnHighestPriorityPriceWhenSeveralPricesAreApplicable() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2020, 6, 14, 16, 0);

        var result = repository
                .findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
                        1L,
                        35455L,
                        applicationDate,
                        applicationDate
                );

        assertThat(result).isPresent();
        assertThat(result.get().getPriceList()).isEqualTo(2L);
        assertThat(result.get().getPrice())
                .isEqualByComparingTo(new BigDecimal("25.45"));
        assertThat(result.get().getPriority()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyWhenNoPriceIsApplicable() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2019, 6, 14, 16, 0);

        var result = repository
                .findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
                        1L,
                        35455L,
                        applicationDate,
                        applicationDate
                );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnPriceWhenApplicationDateMatchesStartDate() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2020, 6, 14, 15, 0);

        var result = repository
                .findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
                        1L,
                        35455L,
                        applicationDate,
                        applicationDate
                );

        assertThat(result).isPresent();
        assertThat(result.get().getPriceList()).isEqualTo(2L);
    }

    @Test
    void shouldReturnPriceWhenApplicationDateMatchesEndDate() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2020, 6, 14, 18, 30);

        var result = repository
                .findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
                        1L,
                        35455L,
                        applicationDate,
                        applicationDate
                );

        assertThat(result).isPresent();
        assertThat(result.get().getPriceList()).isEqualTo(2L);
    }

    @Test
    void shouldNotReturnPriceFromAnotherBrand() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2020, 6, 14, 16, 0);

        var result = repository
                .findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
                        2L,
                        35455L,
                        applicationDate,
                        applicationDate
                );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotReturnPriceFromAnotherProduct() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2020, 6, 14, 16, 0);

        var result = repository
                .findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
                        1L,
                        99999L,
                        applicationDate,
                        applicationDate
                );

        assertThat(result).isEmpty();
    }
}