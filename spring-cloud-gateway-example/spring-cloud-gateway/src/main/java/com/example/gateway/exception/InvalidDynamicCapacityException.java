package com.example.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for the case when the invalid dynamic capacity configuration is being set
 */
@ResponseStatus(
        code = HttpStatus.BAD_REQUEST,
        reason="Invalid tier configuration: tier number must be a positive integer, capacity must be a positive integer"
)
public class InvalidDynamicCapacityException extends RuntimeException {
    public InvalidDynamicCapacityException(String message) {
        super(message);
    }
}
