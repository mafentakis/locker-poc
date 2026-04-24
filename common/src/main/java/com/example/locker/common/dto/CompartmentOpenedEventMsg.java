package com.example.locker.common.dto;

import java.time.Instant;

public record CompartmentOpenedEventMsg(
        String compartmentId,
        Instant compartmentOpenUtcDateTime
) {}
