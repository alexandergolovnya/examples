package com.example.gateway.resolver;

import com.example.gateway.utils.RequestUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
@AllArgsConstructor
public class BasicAuthClientNameExtractor implements ClientNameExtractor {

    @Override
    public String extract(ServerHttpRequest request) {
        return RequestUtils.getHeaderValue(request, HttpHeaders.AUTHORIZATION)
                .filter(header -> header.startsWith("Basic "))
                .map(this::extractBase64)
                .map(this::decodeBase64)
                .map(this::getUsername)
                .orElse(null);
    }

    private String extractBase64(String header) {
        return header.substring("Basic ".length());
    }

    private String decodeBase64(String base64Credentials) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Credentials);
            return new String(bytes);
        } catch (IllegalArgumentException e) {
            log.debug("Failed to decode Base64 credentials: {}", base64Credentials, e);
            return null;
        }
    }

    private String getUsername(String credentials) {
        return credentials.split(":", 2)[0];
    }
}
