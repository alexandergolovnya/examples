package com.example.gateway.capacity.manager;

import com.example.gateway.dto.AcquireCapacityResult;
import com.example.gateway.dto.DynamicCapacityDto;
import com.example.gateway.dto.DynamicCapacityResponse;
import com.example.gateway.dto.ReleaseCapacityResult;
import reactor.core.publisher.Mono;

public interface CapacityManager {
    Mono<AcquireCapacityResult> tryAcquireCapacity(int tier, String clientId);
    Mono<ReleaseCapacityResult> releaseCapacity(int tier, String clientId);
    Mono<DynamicCapacityResponse> getCurrentCapacities();
    Mono<DynamicCapacityResponse> getDefaultCapacities();
    Mono<DynamicCapacityResponse> updateCapacities(DynamicCapacityDto dto);
    void handleReleaseCapacity(Integer tierToRelease, String clientId);
}
