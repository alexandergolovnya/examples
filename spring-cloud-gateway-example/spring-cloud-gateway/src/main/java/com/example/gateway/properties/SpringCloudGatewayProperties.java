package com.example.gateway.properties;

import com.example.gateway.dto.ClientTierDto;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties("spring-cloud-gateway")
@Data
public class SpringCloudGatewayProperties {
    private List<ClientTierDto> clients = Collections.emptyList();
    private Map<Integer, Integer> tiersCapacity = new HashMap<>();
    private Map<String, ClientTierDto> clientLimitsMap;

    private final ClientTierDto defaultLimitConfig = ClientTierDto.builder()
            .tier(3)  // Default to lowest tier
            .build();

    @PostConstruct
    public void init() {
        clientLimitsMap = clients.stream().collect(Collectors.toMap(ClientTierDto::getId, Function.identity()));
    }

    public int getTierForClient(String clientId) {
        return clientLimitsMap.getOrDefault(clientId, defaultLimitConfig).getTier();
    }
}
