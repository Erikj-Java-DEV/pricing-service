package com.erikj.pricing.infrastructure.adapter.out.persistence;

import com.erikj.pricing.domain.model.Price;

import java.util.Currency;

public final class PricePersistenceMapper {

    private PricePersistenceMapper() {
    }

    public static Price toDomain(PriceJpaEntity entity) {
        return new Price(
                entity.getBrandId(),
                entity.getProductId(),
                entity.getPriceList(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPriority(),
                entity.getPrice(),
                Currency.getInstance(entity.getCurrency())
        );
    }
}