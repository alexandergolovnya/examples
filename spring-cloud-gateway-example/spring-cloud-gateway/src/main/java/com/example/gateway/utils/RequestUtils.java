package com.example.gateway.utils;

import lombok.experimental.UtilityClass;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

@UtilityClass
public class RequestUtils {
    public static final String CLIENT_NAME_ATTR = "resolvedClientName";

    public static Optional<String> getHeaderValue(ServerHttpRequest request, String headerName) {
        return Optional.of(request.getHeaders())
                .map(headers -> headers.getOrEmpty(headerName))
                .filter(values -> !CollectionUtils.isEmpty(values))
                .flatMap(values -> values.stream().findFirst());
    }

    public static String getRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "unknown";
    }

    public static String getClientName(ServerWebExchange exchange) {
        return exchange.getAttribute(CLIENT_NAME_ATTR);
    }
}
