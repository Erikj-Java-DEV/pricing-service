package com.erikj.pricing.application.service;

import com.erikj.pricing.application.exception.PriceNotFoundException;
import com.erikj.pricing.application.port.out.LoadApplicablePricePort;
import com.erikj.pricing.domain.model.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetApplicablePriceServiceTest {

    @Mock
    private LoadApplicablePricePort loadApplicablePricePort;

    private GetApplicablePriceService service;

    @BeforeEach
    void setUp() {
        service = new GetApplicablePriceService(loadApplicablePricePort);
    }

    @Test
    void shouldReturnApplicablePriceWhenPriceExists() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2020, 6, 14, 16, 0);

        Price expectedPrice = new Price(
                1L,
                35455L,
                2L,
                LocalDateTime.of(2020, 6, 14, 15, 0),
                LocalDateTime.of(2020, 6, 14, 18, 30),
                1,
                new BigDecimal("25.45"),
                Currency.getInstance("EUR")
        );

        when(loadApplicablePricePort.loadApplicablePrice(
                applicationDate,
                35455L,
                1L
        )).thenReturn(Optional.of(expectedPrice));

        Price result = service.getApplicablePrice(
                applicationDate,
                35455L,
                1L
        );

        assertThat(result).isEqualTo(expectedPrice);

        verify(loadApplicablePricePort).loadApplicablePrice(
                applicationDate,
                35455L,
                1L
        );
    }

    @Test
    void shouldThrowPriceNotFoundExceptionWhenPriceDoesNotExist() {
        LocalDateTime applicationDate =
                LocalDateTime.of(2020, 6, 14, 16, 0);

        when(loadApplicablePricePort.loadApplicablePrice(
                applicationDate,
                35455L,
                1L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplicablePrice(
                applicationDate,
                35455L,
                1L
        ))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining("35455")
                .hasMessageContaining("1");

        verify(loadApplicablePricePort).loadApplicablePrice(
                applicationDate,
                35455L,
                1L
        );
    }
}