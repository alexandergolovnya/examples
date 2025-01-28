package com.example.gateway.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {
    private boolean enabled;
    private int threads;
    private int nettyThreads;
    private String transportMode;
    private int connectionMinimumIdleSize;
    private int connectionPoolSize;
    private String nodeAddress;
    private String clientName;
    private String password;
}
