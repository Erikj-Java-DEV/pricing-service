package com.erikj.pricing.application.exception;

import java.time.LocalDateTime;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(
            LocalDateTime applicationDate,
            Long productId,
            Long brandId
    ) {
        super(
                "No applicable price found for productId=%d, brandId=%d at %s"
                        .formatted(productId, brandId, applicationDate)
        );
    }
}