package com.example.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static java.lang.String.format;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RejectedResponseDto {
    private String errorMessage;
    private String clientId;
    private int requestedTier;

    public RejectedResponseDto(String clientId, int requestedTier) {
        this.errorMessage = format("Service capacity exhausted for client '%s' and tier '%s'", clientId, requestedTier);
        this.clientId = clientId;
        this.requestedTier = requestedTier;
    }
}
