package com.erikj.pricing.infrastructure.adapter.in.rest;

import com.erikj.pricing.application.port.in.GetApplicablePriceUseCase;
import com.erikj.pricing.infrastructure.adapter.in.rest.dto.PriceResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final GetApplicablePriceUseCase getApplicablePriceUseCase;

    public PriceController(GetApplicablePriceUseCase getApplicablePriceUseCase) {
        this.getApplicablePriceUseCase = getApplicablePriceUseCase;
    }

    @GetMapping
    public PriceResponse getApplicablePrice(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime applicationDate,

            @RequestParam
            @Positive
            Long productId,

            @RequestParam
            @Positive
            Long brandId
    ) {
        return PriceResponse.fromDomain(
                getApplicablePriceUseCase.getApplicablePrice(
                        applicationDate,
                        productId,
                        brandId
                )
        );
    }
}