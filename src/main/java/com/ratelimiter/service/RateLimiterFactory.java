package com.ratelimiter.service;

import com.ratelimiter.model.RateLimitAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the correct {@link RateLimiter} strategy bean for a given rule.
 * Spring auto-wires all RateLimiter beans into this map, keyed by the
 * @Component qualifier name ("TOKEN_BUCKET", "SLIDING_WINDOW"), so adding a
 * new algorithm later only requires a new @Component - no changes here.
 */
@Component
public class RateLimiterFactory {

    private final Map<String, RateLimiter> strategies;

    public RateLimiterFactory(Map<String, RateLimiter> strategies) {
        this.strategies = strategies;
    }

    public RateLimiter get(RateLimitAlgorithm algorithm) {
        RateLimiter limiter = strategies.get(algorithm.name());
        if (limiter == null) {
            throw new IllegalArgumentException("No RateLimiter implementation registered for " + algorithm);
        }
        return limiter;
    }
}
