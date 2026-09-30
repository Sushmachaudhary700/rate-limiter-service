package com.ratelimiter.repository;

import com.ratelimiter.model.RateLimitRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RateLimitRuleRepository extends JpaRepository<RateLimitRule, Long> {
    Optional<RateLimitRule> findByClientIdAndResource(String clientId, String resource);
    Optional<RateLimitRule> findByClientIdAndResourceAndEnabledTrue(String clientId, String resource);
}
