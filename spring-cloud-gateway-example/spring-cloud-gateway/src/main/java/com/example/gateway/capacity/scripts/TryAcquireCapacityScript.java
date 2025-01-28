package com.example.gateway.capacity.scripts;

import com.example.gateway.utils.CapacityResults;
import com.example.gateway.utils.RedisUtils;
import com.example.gateway.dto.AcquireCapacityResult;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TryAcquireCapacityScript extends BaseRedisScript<AcquireCapacityResult> {
    private static final String SCRIPT_PATH = "/lua_scripts/try_acquire_capacity.lua";

    public TryAcquireCapacityScript(RedissonClient redisson) {
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
    protected AcquireCapacityResult processResult(List<Object> result) {
        log.debug("Processing try acquire capacity result: {}", result);

        Map<String, Object> resultMap = RedisUtils.toMap(result);
        log.debug("Mapped result map: {}", resultMap);

        boolean acquired = Boolean.TRUE.equals(resultMap.get("acquired"));
        Integer requestedTier =  resultMap.get("requested_tier") == null ? null
                : Integer.valueOf((String) resultMap.get("requested_tier"));

        if (!acquired) {
            String error = (String) resultMap.get("error");
            log.debug("Failed to acquire capacity from tier {}: {}", requestedTier, error);
            return CapacityResults.failedAcquire(error, requestedTier);
        }

        int sourceTier = Integer.parseInt((String) resultMap.get("source_tier"));
        log.info("Successfully acquired capacity from tier {}, requested tier {}", sourceTier, requestedTier);

        return CapacityResults.successfulAcquire(sourceTier, requestedTier);
    }
}
