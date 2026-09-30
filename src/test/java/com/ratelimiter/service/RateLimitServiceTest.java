package com.ratelimiter.service;

import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.exception.RuleNotFoundException;
import com.ratelimiter.model.RateLimitAlgorithm;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.repository.RateLimitRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimitRuleRepository ruleRepository;

    @Mock
    private RateLimiterFactory limiterFactory;

    @Mock
    private RateLimiter tokenBucketLimiter;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    void throwsWhenNoRuleConfigured() {
        when(ruleRepository.findByClientIdAndResourceAndEnabledTrue("client-a", "/orders"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rateLimitService.check("client-a", "/orders", 1))
                .isInstanceOf(RuleNotFoundException.class)
                .hasMessageContaining("client-a");
    }

    @Test
    void delegatesToCorrectStrategyAndReturnsResult() {
        RateLimitRule rule = RateLimitRule.builder()
                .clientId("client-a")
                .resource("/orders")
                .algorithm(RateLimitAlgorithm.TOKEN_BUCKET)
                .limit(10)
                .windowMillis(60_000)
                .enabled(true)
                .build();

        when(ruleRepository.findByClientIdAndResourceAndEnabledTrue("client-a", "/orders"))
                .thenReturn(Optional.of(rule));
        when(limiterFactory.get(RateLimitAlgorithm.TOKEN_BUCKET)).thenReturn(tokenBucketLimiter);
        when(tokenBucketLimiter.checkLimit(rule, "client-a", "/orders", 1))
                .thenReturn(RateLimitCheckResponse.builder()
                        .allowed(true)
                        .remaining(9)
                        .retryAfterMs(0)
                        .algorithm("TOKEN_BUCKET")
                        .build());

        RateLimitCheckResponse response = rateLimitService.check("client-a", "/orders", 1);

        assertThat(response.isAllowed()).isTrue();
        assertThat(response.getRemaining()).isEqualTo(9);
    }
}
