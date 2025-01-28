package com.example.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ExchangeContextFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Add the ServerWebExchange to the context so it can be accessed in the downstream services
        return chain.filter(exchange)
                .contextWrite(context -> context.put(ServerWebExchange.class, exchange));
    }

    @Override
    public int getOrder() {
        // Return a high priority (low value) to execute this filter early
        return -1;
    }
}
