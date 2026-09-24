package com.erikj.pricing.infrastructure.config;

import com.erikj.pricing.application.port.in.GetApplicablePriceUseCase;
import com.erikj.pricing.application.port.out.LoadApplicablePricePort;
import com.erikj.pricing.application.service.GetApplicablePriceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public GetApplicablePriceUseCase getApplicablePriceUseCase(
            LoadApplicablePricePort loadApplicablePricePort
    ) {
        return new GetApplicablePriceService(loadApplicablePricePort);
    }
}