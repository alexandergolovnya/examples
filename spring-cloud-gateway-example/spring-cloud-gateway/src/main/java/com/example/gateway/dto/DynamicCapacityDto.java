package com.example.gateway.dto;

import com.example.gateway.exception.InvalidDynamicCapacityException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Getter
@NoArgsConstructor
public class DynamicCapacityDto {
    private Map<Integer, Integer> capacities;

    public DynamicCapacityDto(Map<Integer, Integer> capacities) {
        setCapacities(capacities);
    }

    private static LinkedHashMap<Integer, Integer> sortCapacities(Map<Integer, Integer> currentCapacities) {
        return currentCapacities.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())  // Reverse sort - higher tiers last
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    public void setCapacities(Map<Integer, Integer> capacities) {
        if (!validate(capacities)) {
            throw new InvalidDynamicCapacityException("Invalid tier configuration: tier number must be a positive integer, capacity must be a positive integer");
        }

        this.capacities = sortCapacities(capacities);
    }

    private boolean validate(Map<Integer, Integer> capacities) {
        log.debug("Validating capacity configuration  to be set: {}", capacities);

        return capacities != null && capacities.entrySet().stream()
                .allMatch(tier -> tier.getKey() > 0 && tier.getValue() >= 0);
    }
}
