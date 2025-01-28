package com.example.gateway.controller;

import com.example.gateway.capacity.manager.CapacityManager;
import com.example.gateway.dto.DynamicCapacityDto;
import com.example.gateway.dto.DynamicCapacityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1.0/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CapacityManager capacityManager;

    /**
     * Dynamically update capacity values.
     * Note: these values are dynamic and are being constantly changed by TryAcquireCapacityScript and ReleaseCapacityScript
     * used in DynamicCapacityPriorityRequestFilter for all incoming requests.
     *
     * @return DynamicCapacityResponse with the capacity values that were set.
     */
    @PutMapping("/capacity")
    public Mono<ResponseEntity<DynamicCapacityResponse>> updateCapacity(@RequestBody DynamicCapacityDto request) {
        return capacityManager.updateCapacities(request)
                .map(result -> !StringUtils.hasText(result.getError())
                        ? ResponseEntity.ok(result)
                        : ResponseEntity.internalServerError().body(result));
    }

    /**
     * Get dynamic capacity values available at the current moment of time.
     * Note: these values are dynamic and are being constantly changed by TryAcquireCapacityScript and ReleaseCapacityScript
     * used in DynamicCapacityPriorityRequestFilter for all incoming request.
     *
     * @return DynamicCapacityResponse with the current capacity values
     */
    @GetMapping("/capacity")
    public Mono<ResponseEntity<DynamicCapacityResponse>> getCurrentCapacity() {
        return capacityManager.getCurrentCapacities()
                .map(result -> !StringUtils.hasText(result.getError())
                        ? ResponseEntity.ok(result)
                        : ResponseEntity.internalServerError().body(result));
    }

    /**
     * Get default capacity values set during the last init or update operation
     *
     * @return DynamicCapacityResponse with the default capacity values
     */
    @GetMapping("/capacity/default")
    public Mono<ResponseEntity<DynamicCapacityResponse>> getDefaultCapacity() {
        return capacityManager.getDefaultCapacities()
                .map(result -> !StringUtils.hasText(result.getError())
                        ? ResponseEntity.ok(result)
                        : ResponseEntity.internalServerError().body(result));
    }
}
