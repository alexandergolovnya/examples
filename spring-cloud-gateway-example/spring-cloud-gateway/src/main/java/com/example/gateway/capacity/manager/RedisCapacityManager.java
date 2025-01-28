package com.example.gateway.capacity.manager;

import com.example.gateway.capacity.scripts.InitializeCapacityScript;
import com.example.gateway.capacity.scripts.ReleaseCapacityScript;
import com.example.gateway.capacity.scripts.TryAcquireCapacityScript;
import com.example.gateway.dto.AcquireCapacityResult;
import com.example.gateway.dto.DynamicCapacityDto;
import com.example.gateway.dto.DynamicCapacityResponse;
import com.example.gateway.dto.ReleaseCapacityResult;
import com.example.gateway.properties.SpringCloudGatewayProperties;
import com.example.gateway.utils.CapacityResults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapAsync;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class RedisCapacityManager implements CapacityManager {
    public static final String CAPACITY_KEY = "spring_cloud_gateway:capacity_map ";
    public static final String DEFAULT_CAPACITY_KEY = "spring_cloud_gateway:default_capacities ";

    private final RedissonClient redisson;
    private final SpringCloudGatewayProperties limitProperties;
    private final InitializeCapacityScript initializeCapacityScript;
    private final TryAcquireCapacityScript tryAcquireCapacityScript;
    private final ReleaseCapacityScript releaseCapacityScript;

    public RedisCapacityManager(
            RedissonClient redisson,
            SpringCloudGatewayProperties limitProperties,
            InitializeCapacityScript initializeCapacityScript,
            TryAcquireCapacityScript tryAcquireCapacityScript,
            ReleaseCapacityScript releaseCapacityScript
    ) {
        this.redisson = redisson;
        this.limitProperties = limitProperties;
        this.initializeCapacityScript = initializeCapacityScript;
        this.tryAcquireCapacityScript = tryAcquireCapacityScript;
        this.releaseCapacityScript = releaseCapacityScript;

        log.info("Redis capacities key: {}", CAPACITY_KEY);
        log.info("Redis default capacities key: {}", DEFAULT_CAPACITY_KEY);

        // Initialize default capacities and subscribe to the result
        initializeDefaultCapacities()
                .subscribe(
                        result -> log.info("Initialized default dynamic capacities: {}", result),
                        error -> log.error("Failed to initialize default capacities", error)
                );
    }

    public Mono<AcquireCapacityResult> tryAcquireCapacity(int tier, String clientId) {
        return tryAcquireCapacityScript
                .executeAsync(CAPACITY_KEY, String.valueOf(tier))
                .doOnError(e -> {
                    log.error("Failed to acquire capacity for client {} (tier {})", clientId, tier, e);
                })
                .doOnCancel(() -> {
                    log.warn("Release capacity operation cancelled for client {} (tier {})", clientId, tier);
                    log.info("Attempting to release capacity for client {} (tier {})", clientId, tier);
                    handleReleaseCapacity(tier, clientId);
                })
                .onErrorReturn(CapacityResults.failedAcquire("Error executing script", tier));
    }

    public Mono<ReleaseCapacityResult> releaseCapacity(int tier, String clientId) {
        return releaseCapacityScript
                .executeAsync(CAPACITY_KEY, String.valueOf(tier))
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
                .doOnSuccess(result -> {
                    if (result.released()) {
                        log.debug("Released capacity for tier {} and client {}: {} -> {}", tier, clientId,
                                result.previousCapacity(), result.newCapacity());
                    } else {
                        log.warn("Failed to release capacity for tier {} and client {}: {}", tier, clientId, result.error());
                    }
                })
                .doOnError(e -> log.error("Failed to release capacity for client {} (tier {})", clientId, tier, e))
                .doOnCancel(() -> {
                    log.warn("Release capacity operation cancelled for client {} (tier {}), no release was done", clientId, tier);
                });
    }

    public Mono<DynamicCapacityResponse> getCurrentCapacities() {
        return getCapacitiesFromKey(CAPACITY_KEY);
    }

    @Override
    public Mono<DynamicCapacityResponse> getDefaultCapacities() {
        return getCapacitiesFromKey(DEFAULT_CAPACITY_KEY);
    }

    public Mono<DynamicCapacityResponse> updateCapacities(DynamicCapacityDto dto) {
        Map<Integer, Integer> capacities = dto.getCapacities();
        log.info("Updating dynamic capacities: {}", capacities);

        return initializeCapacityScript.initialize(CAPACITY_KEY, DEFAULT_CAPACITY_KEY, capacities)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    log.error("Error during updating dynamic capacities", e);
                    return Mono.just(DynamicCapacityResponse.builder()
                            .error(e.getMessage())
                            .success(false)
                            .build());
                });
    }

    public void handleReleaseCapacity(Integer tierToRelease, String clientId) {
        if (tierToRelease != null) {
            releaseCapacity(tierToRelease, clientId)
                    .subscribe( // we are in a cleanup phase, so subscribe is fine
                            result -> {
                                if (!result.released()) {
                                    log.error("Failed to release capacity for client {} (tier {}): {}",
                                            clientId, tierToRelease, result.error());
                                }
                            },
                            error -> log.error("Error releasing capacity for client {} (tier {}): {}",
                                    clientId, tierToRelease, error.getMessage())
                    );
        } else {
            log.warn("Dynamic capacity filter error. Tier to release is null for client {}, no release was done", clientId);
        }
    }

    private Mono<DynamicCapacityResponse> getCapacitiesFromKey(String capacityKey) {
        log.debug("Getting capacities from Redis key: {}", capacityKey);

        return Mono.fromCompletionStage(() -> {
                    RMapAsync<Integer, Integer> map = redisson.getMap(capacityKey);
                    return map.readAllEntrySetAsync();
                })
                .map(this::convertToCapacitiesResponse)
                .onErrorResume(e -> {
                    log.error("Error during reading capacities: {}", e.getMessage());

                    return Mono.just(createErrorResponse("Error reading capacities: " + e.getMessage()));
                });
    }

    private Mono<DynamicCapacityResponse> initializeDefaultCapacities() {
        DynamicCapacityDto defaultCapacities = new DynamicCapacityDto(limitProperties.getTiersCapacity());
        log.info("Default dynamic capacities from configs: {}", defaultCapacities);

        return updateCapacities(defaultCapacities);
    }

    private DynamicCapacityResponse convertToCapacitiesResponse(Set<Map.Entry<Integer, Integer>> entries) {
        try {
            Map<Integer, Integer> capacities = new HashMap<>(entries.size());
            for (Map.Entry<Integer, Integer> entry : entries) {
                capacities.put(entry.getKey(), entry.getValue());
            }

            return DynamicCapacityResponse.builder()
                    .capacities(capacities)
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing capacity values: {}", e.getMessage());
            return createErrorResponse("Error parsing capacity values: " + e.getMessage());
        }
    }

    private DynamicCapacityResponse createErrorResponse(String errorMessage) {
        return DynamicCapacityResponse.builder()
                .success(false)
                .error(errorMessage)
                .build();
    }
}
