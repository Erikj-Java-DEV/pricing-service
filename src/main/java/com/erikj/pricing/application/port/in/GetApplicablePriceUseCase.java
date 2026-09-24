package com.erikj.pricing.application.port.in;

import com.erikj.pricing.domain.model.Price;

import java.time.LocalDateTime;

public interface GetApplicablePriceUseCase {

    Price getApplicablePrice(
            LocalDateTime applicationDate,
            Long productId,
            Long brandId
    );
}