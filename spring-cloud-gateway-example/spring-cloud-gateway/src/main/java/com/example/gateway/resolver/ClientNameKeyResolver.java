package com.example.gateway.resolver;

import com.example.gateway.utils.RequestUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.gateway.utils.RequestUtils.CLIENT_NAME_ATTR;

@Slf4j
@Primary
@Component
@AllArgsConstructor
public class ClientNameKeyResolver implements KeyResolver {
    private static final String UNKNOWN_CLIENT_NAME = "unknown";

    private List<ClientNameExtractor> clientNameExtractors;

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        // First check if we already resolved the client name for this exchange
        Object cachedClientName = exchange.getAttribute(CLIENT_NAME_ATTR);
        if (cachedClientName != null) {
            return Mono.just((String) cachedClientName);
        }

        // If not cached, resolve the client name
        return resolveClientName(exchange);
    }

    private Mono<String> resolveClientName(ServerWebExchange exchange) {
        String clientName = getClientName(exchange);

        if (clientName == null) {
            clientName = returnDefaultName(exchange);
        }

        // Cache the resolved name in exchange attributes
        log.trace("Adding resolved client name to request attributes: {}", clientName);
        exchange.getAttributes().put(CLIENT_NAME_ATTR, clientName);

        return Mono.just(clientName);
    }

    private String getClientName(ServerWebExchange exchange) {
        for (ClientNameExtractor clientNameExtractor : clientNameExtractors) {
            String clientName = clientNameExtractor.extract(exchange.getRequest());
            if (clientName != null) {
                return clientName.toLowerCase().trim();
            }
        }

        return null;
    }

    private String returnDefaultName(ServerWebExchange exchange) {
        log.warn("Client name cannot be resolved. Authorization header: '{}'",
                RequestUtils.getHeaderValue(exchange.getRequest(), HttpHeaders.AUTHORIZATION)
                        .orElse("null"));

        // return unknown client name if client name cannot be resolved. This is required to allow rate limiting.
        // All unresolved clients will be limited by the same rate limit in this case.
        return UNKNOWN_CLIENT_NAME;
    }
}
