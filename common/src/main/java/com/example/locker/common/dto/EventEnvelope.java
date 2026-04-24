package com.example.locker.common.dto;

public record EventEnvelope<T>(
        EventHeaders headers,
        T payload
) {}
