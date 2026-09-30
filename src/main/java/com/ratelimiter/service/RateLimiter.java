package com.ratelimiter.service;

import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.model.RateLimitRule;

/**
 * Strategy interface for a rate-limiting algorithm. Each implementation
 * must perform its check atomically in Redis (via a Lua script) so that
 * concurrent requests from multiple instances of this service never
 * race each other.
 */
public interface RateLimiter {

    RateLimitCheckResponse checkLimit(RateLimitRule rule, String clientId, String resource, int tokensRequested);
}
