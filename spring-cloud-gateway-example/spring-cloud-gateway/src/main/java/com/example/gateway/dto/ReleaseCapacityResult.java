package com.example.gateway.dto;

public record ReleaseCapacityResult(
        boolean released,
        Integer tier,
        Integer previousCapacity,
        Integer newCapacity,
        String error
) {}
