package com.example.gateway.capacity.scripts;

import com.example.gateway.utils.CapacityResults;
import com.example.gateway.utils.RedisUtils;
import com.example.gateway.dto.ReleaseCapacityResult;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ReleaseCapacityScript extends BaseRedisScript<ReleaseCapacityResult> {
    private static final String SCRIPT_PATH = "/lua_scripts/release_capacity.lua";

    public ReleaseCapacityScript(RedissonClient redisson) {
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
    protected ReleaseCapacityResult processResult(List<Object> result) {
        log.debug("Processing release capacity result: {}", result);

        Map<String, Object> resultMap = RedisUtils.toMap(result);
        log.debug("Mapped result map: {}", resultMap);

        boolean released = Boolean.TRUE.equals(resultMap.get("released"));

        if (!released) {
            String error = (String) resultMap.get("error");
            log.info("Failed to release capacity: {}", error);
            return CapacityResults.failedRelease(error);
        }

        ReleaseCapacityResult response = CapacityResults.successfulRelease(
                Integer.parseInt((String) resultMap.get("tier")),
                Integer.parseInt((String) resultMap.get("previous_capacity")),
                Integer.parseInt((String) resultMap.get("new_capacity"))
        );

        log.debug("Successfully released capacity for tier {}: {} -> {}",
                response.tier(), response.previousCapacity(), response.newCapacity());

        return response;
    }
}
