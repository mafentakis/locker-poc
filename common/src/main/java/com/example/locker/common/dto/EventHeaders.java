package com.example.locker.common.dto;

import java.time.Instant;

public record EventHeaders(
        String apiVersion,
        String lockerId,
        String eventType,
        Instant eventCreateUtcDateTime
) {}
