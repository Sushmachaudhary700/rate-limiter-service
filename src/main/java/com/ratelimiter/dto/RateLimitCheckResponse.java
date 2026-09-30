package com.ratelimiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * What we send back after a check — everything a caller needs to decide
 * what to do next, whether that's "proceed" or "back off and retry later".
 */
@Getter
@Builder
@AllArgsConstructor
public class RateLimitCheckResponse {
    /** True = go ahead. False = the client has hit its limit. */
    private boolean allowed;

    /** How many requests/tokens are left in the current window (0 if denied). */
    private long remaining;

    /** Only meaningful when allowed=false: how long to wait before trying again. */
    private long retryAfterMs;

    /** Which algorithm produced this result — handy for debugging which rule fired. */
    private String algorithm;
}
