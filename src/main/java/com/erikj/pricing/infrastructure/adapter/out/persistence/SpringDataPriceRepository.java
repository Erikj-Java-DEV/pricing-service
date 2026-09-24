package com.erikj.pricing.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataPriceRepository
        extends JpaRepository<PriceJpaEntity, Long> {

    Optional<PriceJpaEntity>
    findFirstByBrandIdAndProductIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityDesc(
            Long brandId,
            Long productId,
            LocalDateTime applicationDateStart,
            LocalDateTime applicationDateEnd
    );
}