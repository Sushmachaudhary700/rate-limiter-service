package com.ratelimiter.service;

import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.exception.RuleNotFoundException;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.repository.RateLimitRuleRepository;
import org.springframework.stereotype.Service;

/**
 * The "front desk" of the rate limiter. Callers don't need to know or care
 * whether a client is on Token Bucket or Sliding Window — they just ask
 * "can this client proceed?" and this class figures out the rest:
 *   1. look up how this client+resource is configured (the rule)
 *   2. hand off to whichever algorithm that rule says to use
 *   3. return the answer
 */
@Service
public class RateLimitService {

    private final RateLimitRuleRepository ruleRepository;
    private final RateLimiterFactory limiterFactory;

    public RateLimitService(RateLimitRuleRepository ruleRepository, RateLimiterFactory limiterFactory) {
        this.ruleRepository = ruleRepository;
        this.limiterFactory = limiterFactory;
    }

    public RateLimitCheckResponse check(String clientId, String resource, int tokensRequested) {
        // No rule = we don't know how to treat this client, so we fail
        // loudly with a 404 rather than silently allowing or blocking —
        // an admin needs to explicitly configure a limit first.
        RateLimitRule rule = ruleRepository.findByClientIdAndResourceAndEnabledTrue(clientId, resource)
                .orElseThrow(() -> new RuleNotFoundException(clientId, resource));

        RateLimiter limiter = limiterFactory.get(rule.getAlgorithm());
        return limiter.checkLimit(rule, clientId, resource, tokensRequested);
    }
}
