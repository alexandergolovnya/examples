package com.example.gateway.exception;

/**
 * Custom exception for Redis script execution errors.
 */
public class RedisScriptException extends RuntimeException {
    public RedisScriptException(String message) {
        super(message);
    }

    public RedisScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
