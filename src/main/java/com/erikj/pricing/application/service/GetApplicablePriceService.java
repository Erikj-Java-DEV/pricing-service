package com.erikj.pricing.application.service;

import com.erikj.pricing.application.exception.PriceNotFoundException;
import com.erikj.pricing.application.port.in.GetApplicablePriceUseCase;
import com.erikj.pricing.application.port.out.LoadApplicablePricePort;
import com.erikj.pricing.domain.model.Price;

import java.time.LocalDateTime;
import java.util.Objects;

public final class GetApplicablePriceService
        implements GetApplicablePriceUseCase {

    private final LoadApplicablePricePort loadApplicablePricePort;

    public GetApplicablePriceService(
            LoadApplicablePricePort loadApplicablePricePort
    ) {
        this.loadApplicablePricePort =
                Objects.requireNonNull(loadApplicablePricePort);
    }

    @Override
    public Price getApplicablePrice(
            LocalDateTime applicationDate,
            Long productId,
            Long brandId
    ) {
        return loadApplicablePricePort
                .loadApplicablePrice(
                        applicationDate,
                        productId,
                        brandId
                )
                .orElseThrow(
                        () -> new PriceNotFoundException(
                                applicationDate,
                                productId,
                                brandId
                        )
                );
    }
}