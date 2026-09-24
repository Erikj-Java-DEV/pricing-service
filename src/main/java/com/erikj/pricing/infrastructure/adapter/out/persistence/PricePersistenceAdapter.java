package com.erikj.pricing.infrastructure.adapter.out.persistence;

import com.erikj.pricing.application.port.out.LoadApplicablePricePort;
import com.erikj.pricing.domain.model.Price;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class PricePersistenceAdapter implements LoadApplicablePricePort {

    private final SpringDataPriceRepository repository;

    public PricePersistenceAdapter(
            SpringDataPriceRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<Price> loadApplicablePrice(
            LocalDateTime applicationDate,
            Long productId,
            Long brandId
    ) {
        return repository
                .findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
                        brandId,
                        productId,
                        applicationDate,
                        applicationDate
                )
                .map(PricePersistenceMapper::toDomain);
    }
}