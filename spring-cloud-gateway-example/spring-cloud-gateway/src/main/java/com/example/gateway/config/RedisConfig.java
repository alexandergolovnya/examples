package com.example.gateway.config;

import com.example.gateway.properties.RedisProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties redisProperties;

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(RedissonConnectionFactory redissonConnectionFactory) {
        return new ReactiveStringRedisTemplate(redissonConnectionFactory);
    }

    @Bean(name = "redissonClient", destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setTransportMode(TransportMode.valueOf(redisProperties.getTransportMode()));
        config.setThreads(redisProperties.getThreads());
        config.setNettyThreads(redisProperties.getNettyThreads());
        config.setCodec(new JsonJacksonCodec(redissonObjectMapper()));

        config.useSingleServer()
                .setPassword(redisProperties.getPassword())
                .setClientName(redisProperties.getClientName())
                .setSubscriptionConnectionMinimumIdleSize(redisProperties.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(redisProperties.getConnectionPoolSize())
                .setAddress(redisProperties.getNodeAddress());

        return Redisson.create(config);
    }

    private ObjectMapper redissonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}