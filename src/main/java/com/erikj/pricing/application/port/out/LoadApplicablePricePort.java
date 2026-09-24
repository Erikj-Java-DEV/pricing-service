package com.erikj.pricing.application.port.out;

import com.erikj.pricing.domain.model.Price;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoadApplicablePricePort {

    Optional<Price> loadApplicablePrice(
            LocalDateTime applicationDate,
            Long productId,
            Long brandId
    );
}