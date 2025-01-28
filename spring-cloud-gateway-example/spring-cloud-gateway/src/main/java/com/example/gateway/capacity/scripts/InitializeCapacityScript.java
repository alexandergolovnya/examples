package com.example.gateway.capacity.scripts;

import com.example.gateway.utils.RedisUtils;
import com.example.gateway.dto.DynamicCapacityResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Script for initializing capacity values in Redis.
 * Ensures atomic operations when setting capacity values.
 */
@Slf4j
@Component
public class InitializeCapacityScript extends BaseRedisScript<DynamicCapacityResponse> {
    private static final String SCRIPT_PATH = "/lua_scripts/initialize_capacity.lua";

    public InitializeCapacityScript(RedissonClient redisson) {
        super(redisson, SCRIPT_PATH);
        log.info("Initialized {} script: {}", this.getClass().getSimpleName(), SCRIPT_PATH);
        log.debug("Loaded script content for {}: {}", SCRIPT_PATH, getScriptContent());
    }

    @Override
    protected String getScriptPath() {
        return SCRIPT_PATH;
    }

    @Override
    protected RScript.ReturnType getReturnType() {
        return RScript.ReturnType.MAPVALUELIST;
    }

    @Override
    protected DynamicCapacityResponse processResult(List<Object> result) {
        Map<String, Object> resultMap = RedisUtils.toMap(result);
        log.debug("Mapped result map: {}", resultMap);

        DynamicCapacityResponse response = DynamicCapacityResponse.builder()
                .success((Boolean) resultMap.get("success"))
                .error((String) resultMap.get("error"))
                .build();

        if (Boolean.TRUE.equals(response.getSuccess())) {
            @SuppressWarnings("unchecked")
            List<Object> capacities = (List<Object>) resultMap.get("capacities");
            log.debug("Received updated capacities from the Lua script: {}", capacities);

            Map<Integer, Integer> currentCapacities = RedisUtils.toCapacityMap(capacities);
            log.debug("Setting updated capacities to the response: {}", currentCapacities);
            response.setCapacities(currentCapacities);
        }

        return response;
    }

    /**
     * Initialize capacities for the given key
     *
     * @param capacitiesKey Redis key for the capacity map
     * @param defaultCapacityKey Redis key for the default capacity map
     * @param capacities Map of tier to capacity values (already sorted in descending order)
     * @return DynamicCapacityResponse with the result
     */
    public Mono<DynamicCapacityResponse> initialize(
            String capacitiesKey,
            String defaultCapacityKey,
            Map<Integer, Integer> capacities
    ) {
        log.debug("Updating capacities: {}", capacities);
        log.trace("Redis capacities key: {}", capacitiesKey);
        log.trace("Redis default capacities key: {}", defaultCapacityKey);

        List<Object> args = formatCapacityArgs(capacities);
        return executeAsync(List.of(capacitiesKey, defaultCapacityKey), args.toArray());
    }

    /**
     * Helper method to properly format capacity arguments
     * Note: The order of arguments is preserved from the input map
     */
    public List<Object> formatCapacityArgs(Map<Integer, Integer> capacities) {
        List<Object> args = new ArrayList<>();

        capacities.forEach((tier, capacity) -> {
            args.add(String.valueOf(tier));
            args.add(String.valueOf(capacity));
        });

        log.debug("Formatted capacity args: {}", args);
        return args;
    }
}
