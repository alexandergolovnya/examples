package com.example.gateway.resolver;

import org.springframework.http.server.reactive.ServerHttpRequest;

public interface ClientNameExtractor {

    String extract(ServerHttpRequest request);
}
