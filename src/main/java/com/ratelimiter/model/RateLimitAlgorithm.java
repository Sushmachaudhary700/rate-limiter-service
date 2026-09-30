package com.ratelimiter.model;

public enum RateLimitAlgorithm {
    /** Smooths bursts by allowing saved-up "tokens" to be spent quickly, then refills gradually. */
    TOKEN_BUCKET,
    /** Approximate sliding window using a weighted average of the current and previous fixed windows. */
    SLIDING_WINDOW
}
