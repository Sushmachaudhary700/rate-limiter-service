package com.ratelimiter.controller;

import com.ratelimiter.dto.RateLimitCheckRequest;
import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The one endpoint every other service actually calls: "am I allowed to
 * proceed?" Callers hit this BEFORE doing the real work (processing an
 * order, running a search, etc.) and only continue if the answer is yes.
 */
@RestController
@RequestMapping("/api/v1/rate-limit")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/check")
    @Operation(summary = "Check whether a request from a client should be allowed",
            description = "Callers (other services) hit this before processing a request. " +
                    "Returns allowed=true/false plus how many tokens/requests remain and, if denied, retryAfterMs.")
    public ResponseEntity<RateLimitCheckResponse> check(@Valid @RequestBody RateLimitCheckRequest request) {
        RateLimitCheckResponse response = rateLimitService.check(
                request.getClientId(), request.getResource(), request.getTokensRequested());

        // 200 = go ahead, 429 = "Too Many Requests" — the standard HTTP
        // status specifically meant for rate limiting, so any client
        // library or gateway already knows how to interpret it.
        HttpStatus status = response.isAllowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;

        // These headers follow the common convention popularized by GitHub/Stripe's
        // APIs, so callers can back off intelligently instead of just retrying blindly.
        return ResponseEntity.status(status)
                .header("X-RateLimit-Remaining", String.valueOf(response.getRemaining()))
                .header("X-RateLimit-Retry-After-Ms", String.valueOf(response.getRetryAfterMs()))
                .body(response);
    }
}
