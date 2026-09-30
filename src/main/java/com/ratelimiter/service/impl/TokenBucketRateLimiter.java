package com.ratelimiter.service.impl;

import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.service.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Token Bucket algorithm.
 *
 * Picture a bucket that holds up to {@code limit} tokens. Every request
 * costs one token. Tokens drip back into the bucket at a steady rate
 * ({@code refillRatePerSecond}), so a client that's been idle for a while
 * walks in with a full bucket and can burst through several requests at
 * once — but once it's empty, they have to wait for tokens to trickle back.
 *
 * This is the same idea used by AWS API Gateway and Stripe's rate limiter:
 * it's forgiving of short bursts but still enforces a hard average rate
 * over time.
 *
 * The bean name "TOKEN_BUCKET" below isn't just a label — RateLimiterFactory
 * uses it to find this class by matching it against RateLimitAlgorithm.TOKEN_BUCKET.
 */
@Component("TOKEN_BUCKET")
public class TokenBucketRateLimiter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List> script;

    public TokenBucketRateLimiter(RedisTemplate<String, String> redisTemplate,
                                   @Qualifier("tokenBucketScript") DefaultRedisScript<List> script) {
        this.redisTemplate = redisTemplate;
        this.script = script;
    }

    @Override
    public RateLimitCheckResponse checkLimit(RateLimitRule rule, String clientId, String resource, int tokensRequested) {
        // Every client+resource combination gets its own bucket in Redis,
        // e.g. "rl:tb:mobile-app:/api/orders" — a completely separate
        // counter from any other client hitting the same endpoint.
        String bucketKey = "rl:tb:" + clientId + ":" + resource;

        // If the rule didn't specify an explicit refill rate, work it out
        // from the limit and window instead, e.g. "100 requests per 60s"
        // becomes "1.67 tokens added back every second".
        double refillRatePerSecond = rule.getRefillRatePerSecond() != null
                ? rule.getRefillRatePerSecond()
                : rule.getLimit() / (rule.getWindowMillis() / 1000.0);

        // We hand the whole "check tokens, subtract, refill" sequence off
        // to a Lua script running INSIDE Redis. That's what makes this
        // safe when this service has multiple copies running at once —
        // Redis runs the entire script as one uninterruptible step, so two
        // instances can never both read "3 tokens left" and both proceed.
        @SuppressWarnings("unchecked")
        List<Long> luaResult = redisTemplate.execute(
                script,
                List.of(bucketKey),
                String.valueOf(rule.getLimit()),              // bucket capacity
                String.valueOf(refillRatePerSecond),           // tokens added per second
                String.valueOf(System.currentTimeMillis()),    // "now", used to calculate how much to refill
                String.valueOf(tokensRequested)                 // how many tokens this request costs
        );

        boolean allowed = luaResult.get(0) == 1L;
        long tokensRemaining = luaResult.get(1);
        long retryAfterMs = luaResult.get(2); // only meaningful when allowed == false

        return RateLimitCheckResponse.builder()
                .allowed(allowed)
                .remaining(tokensRemaining)
                .retryAfterMs(retryAfterMs)
                .algorithm("TOKEN_BUCKET")
                .build();
    }
}
