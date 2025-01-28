package com.example.gateway.dto;

public record AcquireCapacityResult(
        boolean acquired,
        Integer sourceTier,
        Integer requestedTier,
        String error
) {}
