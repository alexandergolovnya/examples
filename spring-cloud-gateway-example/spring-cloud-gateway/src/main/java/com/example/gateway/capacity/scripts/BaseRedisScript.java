package com.example.gateway.capacity.scripts;

import com.example.gateway.exception.RedisScriptException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for Redis Lua scripts execution.
 * Provides a template for loading and executing Lua scripts with proper error handling.
 *
 * @param <T> The return type of the script execution
 */
@Slf4j
public abstract class BaseRedisScript<T> {
    private final RedissonClient redisson;
    @Getter
    private final String scriptContent;

    /**
     * Creates a new Redis script with the given RedissonClient and script path.
     *
     * @param redisson RedissonClient instance
     * @param scriptPath Path to the Lua script in resources
     */
    protected BaseRedisScript(RedissonClient redisson, String scriptPath) {
        this.redisson = redisson;
        this.scriptContent = loadScript(scriptPath);
    }

    /**
     * Gets the path to the Lua script file.
     *
     * @return the script file path
     */
    protected abstract String getScriptPath();

    /**
     * Gets the return type for the script execution.
     *
     * @return the RScript.ReturnType for this script
     */
    protected abstract RScript.ReturnType getReturnType();

    /**
     * Processes the raw result from script execution into the desired type.
     *
     * @param result the raw result from Redis
     * @return processed result of type T
     */
    protected abstract T processResult(List<Object> result);

    /**
     * Executes the script with a single key.
     *
     * @param key the Redis key
     * @param args additional arguments for the script
     * @return processed result of type T
     */
    public Mono<T> executeAsync(String key, Object... args) {
        return executeAsync(Collections.singletonList(key), args);
    }

    /**
     * Executes the script with multiple keys.
     *
     * @param keys list of Redis keys
     * @param args additional arguments for the script
     * @return processed result of type T
     * @throws RedisScriptException if script execution fails
     */
    public Mono<T> executeAsync(List<Object> keys, Object... args) {
        RScript script = redisson.getScript(StringCodec.INSTANCE);

        return Mono.fromCompletionStage(
                script.evalAsync(
                        RScript.Mode.READ_WRITE,
                        scriptContent,
                        getReturnType(),
                        keys,
                        args
                )
        ).map(result -> {
            log.debug("Raw script result: {}", result);
            if (result == null || (result instanceof List && ((List<?>) result).isEmpty())) {
                throw new RedisScriptException(
                        String.format("Script %s returned empty result", getScriptPath())
                );
            }
            return processResult((List<Object>) result);
        }).onErrorMap(e -> {
            String keysString = keys.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            log.error("Failed to execute Redis script: {} with keys: [{}]",
                    getScriptPath(), keysString, e);
            return new RedisScriptException(
                    String.format("Failed to execute Redis script: %s with keys: [%s]",
                            getScriptPath(), keysString),
                    e
            );
        }).doOnCancel(() -> {
            log.warn("Script execution cancelled: {}", getScriptPath());
        });
    }

    /**
     * Loads the Lua script content from resources.
     *
     * @param path path to the script file
     * @return the script content as a String
     * @throws IllegalStateException if the script cannot be loaded
     */
    public String loadScript(String path) {
        try (var is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new RedisScriptException("Script not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RedisScriptException("Failed to load script: " + path, e);
        }
    }
}
