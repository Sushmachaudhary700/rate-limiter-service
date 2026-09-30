package com.ratelimiter.exception;

public class RuleNotFoundException extends RuntimeException {
    public RuleNotFoundException(String clientId, String resource) {
        super("No rate limit rule configured for clientId='%s' resource='%s'".formatted(clientId, resource));
    }
}
