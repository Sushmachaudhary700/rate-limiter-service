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
 * Sliding Window Counter algorithm.
 *
 * Instead of a hard reset at the top of every minute (which lets a client
 * send double their limit right across the boundary — e.g. max requests
 * at 11:59:59 and again at 12:00:00), this blends two fixed windows: the
 * current one and the previous one, weighted by how much of the previous
 * window's time range is still "inside" the last N milliseconds.
 *
 * It's an approximation (not a perfectly precise sliding log), but it's
 * cheap — one Redis key per window instead of storing every request
 * timestamp — and it's what most production rate limiters actually use.
 *
 * Bean name "SLIDING_WINDOW" is how RateLimiterFactory finds this class.
 */
@Component("SLIDING_WINDOW")
public class SlidingWindowRateLimiter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List> script;

    public SlidingWindowRateLimiter(RedisTemplate<String, String> redisTemplate,
                                     @Qualifier("slidingWindowScript") DefaultRedisScript<List> script) {
        this.redisTemplate = redisTemplate;
        this.script = script;
    }

    @Override
    public RateLimitCheckResponse checkLimit(RateLimitRule rule, String clientId, String resource, int tokensRequested) {
        long now = System.currentTimeMillis();
        long windowMillis = rule.getWindowMillis();

        // Which fixed-size "bucket of time" are we in right now, and which
        // one came right before it? E.g. with a 10-second window, window
        // 1523 covers ms 15230000-15239999, window 1522 covers the 10s before that.
        long currentWindowId = now / windowMillis;
        long previousWindowId = currentWindowId - 1;

        // How far we are into the current window — needed to work out how
        // much "weight" the previous window still carries in our sliding view.
        long elapsedInCurrentWindow = now % windowMillis;

        String currentKey = "rl:sw:" + clientId + ":" + resource + ":" + currentWindowId;
        String previousKey = "rl:sw:" + clientId + ":" + resource + ":" + previousWindowId;

        // Same principle as the token bucket: the compare-then-increment
        // logic runs atomically inside Redis via a Lua script, so it stays
        // correct even with several instances of this service running.
        @SuppressWarnings("unchecked")
        List<Long> luaResult = redisTemplate.execute(
                script,
                List.of(currentKey, previousKey),
                String.valueOf(rule.getLimit()),
                String.valueOf(windowMillis),
                String.valueOf(elapsedInCurrentWindow),
                String.valueOf(tokensRequested) // how many "requests" this call counts as
        );

        boolean allowed = luaResult.get(0) == 1L;
        long effectiveCount = luaResult.get(1);
        long retryAfterMs = luaResult.get(2);
        long remaining = Math.max(0, rule.getLimit() - effectiveCount);

        return RateLimitCheckResponse.builder()
                .allowed(allowed)
                .remaining(remaining)
                .retryAfterMs(retryAfterMs)
                .algorithm("SLIDING_WINDOW")
                .build();
    }
}
