package com.erikj.pricing.infrastructure.adapter.in.rest.error;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
}