package com.example.gateway.capacity.filter;

import com.example.gateway.capacity.manager.CapacityManager;
import com.example.gateway.dto.AcquireCapacityResult;
import com.example.gateway.dto.RejectedResponseDto;
import com.example.gateway.properties.SpringCloudGatewayProperties;
import com.example.gateway.resolver.ClientNameKeyResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class CapacityFilter extends AbstractGatewayFilterFactory<Object> {

    private final CapacityManager capacityManager;
    private final SpringCloudGatewayProperties limitProperties;
    private final ClientNameKeyResolver clientNameKeyResolver;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String clientId = clientNameKeyResolver.resolve(exchange).block();
            int requestedTier = limitProperties.getTierForClient(clientId);

            return Mono.usingWhen(
                    //  Execute acquire capacity operation attempt
                    capacityManager.tryAcquireCapacity(requestedTier, clientId),

                    // Handle acquire capacity operation result
                    acquireResult -> handleAcquireCapacityOperationResult(exchange,
                            chain, acquireResult, clientId, requestedTier),

                    // Handle cleanup on completion when capacity was acquired to release capacity back into the pool
                    acquireResult -> handleAcquireCapacityOperationCleanupOnCompletion(
                            acquireResult, clientId),

                    // Handle cleanup on error to release capacity back into the pool or do nothing if no capacity was acquired
                    (acquireResult, err) -> handleAcquireCapacityOperationCleanupOnError(
                            acquireResult, err, clientId),

                    // Handle cleanup on request cancellation when capacity was acquired to release capacity back into the pool
                    // or do nothing if no capacity was acquired
                    acquireResult -> handleAcquireCapacityOperationCleanupOnRequestCancellation(
                            acquireResult, clientId)
            );
        };
    }

    private Mono<Object> handleAcquireCapacityOperationCleanupOnRequestCancellation(
            AcquireCapacityResult acquireResult,
            String clientId
    ) {
        log.warn("Capacity acquisition cancelled for client {} and tier {}", clientId, acquireResult.sourceTier());
        log.debug("Acquired capacity operation result: {}", acquireResult);

        if (acquireResult.acquired()) {
            log.debug("Cleaning up acquired capacity for client {} and tier {}", clientId, acquireResult.sourceTier());
            capacityManager.handleReleaseCapacity(acquireResult.sourceTier(), clientId);
        } else {
            log.debug("No capacity acquired for client {}, no cleanup needed", clientId);
        }

        return Mono.empty();
    }

    private Mono<Object> handleAcquireCapacityOperationCleanupOnError(
            AcquireCapacityResult acquireResult,
            Throwable err,
            String clientId
    ) {
        log.error("Error occurred during capacity acquisition for client {} and tier {}: {}",
                clientId, acquireResult.sourceTier(), err.getMessage());
        log.debug("Acquired capacity operation result: {}", acquireResult);

        if (acquireResult.acquired()) {
            log.debug("Cleaning up acquired capacity for client {} and tier {}", clientId, acquireResult.sourceTier());
            capacityManager.handleReleaseCapacity(acquireResult.sourceTier(), clientId);
        } else {
            log.debug("No capacity acquired for client {}, no cleanup needed", clientId);
        }

        return Mono.empty();
    }

    private Mono<Object> handleAcquireCapacityOperationCleanupOnCompletion(
            AcquireCapacityResult acquireResult,
            String clientId
    ) {
        log.debug("Cleaning up acquired capacity for client {} and tier {}", clientId, acquireResult.sourceTier());
        log.debug("Acquired capacity operation result: {}", acquireResult);

        if (acquireResult.acquired()) {
            capacityManager.handleReleaseCapacity(acquireResult.sourceTier(), clientId);
        }
        return Mono.empty();
    }

    private Mono<Void> handleAcquireCapacityOperationResult(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            AcquireCapacityResult acquireResult,
            String clientId,
            int requestedTier
    ) {
        log.debug("Acquired capacity operation result: {}", acquireResult);

        if (!acquireResult.acquired()) {
            // Reject early if no capacity
            log.error("Failed to acquire capacity for client {} and requested tier {}",
                    clientId, requestedTier);
            return handleRejection(exchange, requestedTier, clientId)
                    .timeout(Duration.ofSeconds(10)) // Safety timeout
                    .onErrorResume(e -> {
                        log.error("Dynamic capacity error. Rejection handling timed out or failed: {}", e.getMessage());
                        return Mono.empty();
                    });
        }

        log.debug("Successfully acquired capacity for client {} from tier {}, requested tier {}",
                clientId, acquireResult.sourceTier(), requestedTier);

        // Otherwise proceed with filter chain
        return chain.filter(exchange)
                .timeout(Duration.ofSeconds(30)) // Safety timeout for stuck responses
                .onErrorResume(e -> {
                    log.warn("Downstream response processing failed or timed out for client {}: {}",
                            clientId, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> handleRejection(
            ServerWebExchange exchange,
            int tier,
            String clientId
    ) {
        log.debug("Request rejected for client {} (requested tier {}): capacity exhausted", clientId, tier);
        log.trace("Constructing rejection response for return");

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("X-Rejected-Tier", String.valueOf(tier));
        response.getHeaders().add("X-Rejected-Client", clientId);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        RejectedResponseDto dto = new RejectedResponseDto(clientId, tier);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(dto);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        log.trace("Returning rejection response to client: {}", dto);
        return response.writeWith(Mono.just(buffer));
    }
}
