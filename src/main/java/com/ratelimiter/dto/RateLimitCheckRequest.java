package com.ratelimiter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What a caller sends us to ask "can I proceed?"
 * Example JSON: {"clientId": "mobile-app", "resource": "/api/orders", "tokensRequested": 1}
 */
@Getter
@Setter
@NoArgsConstructor  // Jackson needs this to deserialize incoming JSON
@AllArgsConstructor // handy for building requests directly in tests
public class RateLimitCheckRequest {

    /** Who's asking — an API key, tenant ID, or service name. Must match a configured rule. */
    @NotBlank
    private String clientId;

    /** What they're trying to access, e.g. "/api/orders". Must match a configured rule. */
    @NotBlank
    private String resource;

    /** How "expensive" this request is. Most calls just cost 1; defaults to 1 if not sent. */
    @Positive
    private int tokensRequested = 1;
}
