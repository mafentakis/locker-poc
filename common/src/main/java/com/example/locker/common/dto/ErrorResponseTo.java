package com.example.locker.common.dto;

import java.time.Instant;

public record ErrorResponseTo(
        String errorCode,
        String message,
        Instant timestamp
) {}
