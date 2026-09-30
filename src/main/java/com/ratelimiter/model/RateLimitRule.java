package com.ratelimiter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A "rule" is just a row that answers one question: for this client, on
 * this resource, how many requests are they allowed to make, and how fast?
 *
 * Think of it as the settings panel for the rate limiter. Instead of hard
 * coding "everyone gets 100 requests a minute" into the Java code, we look
 * this row up at request time — so a support engineer (or an admin API
 * call) can change a client's limit without anyone redeploying the app.
 *
 * Example row: clientId="mobile-app", resource="/api/orders",
 * algorithm=TOKEN_BUCKET, limit=100, windowMillis=60_000
 * → "mobile-app can make 100 requests to /api/orders per 60 seconds."
 */
@Entity
@Table(
        name = "rate_limit_rules",
        uniqueConstraints = {
                // IMPORTANT: these must be the real DB column names, not the Java
                // field names. Spring's default naming strategy converts camelCase
                // Java fields to snake_case columns (clientId -> client_id), so if
                // this said "clientId" here instead, schema creation would fail on
                // a strict database like Postgres with "column clientId does not
                // exist". One client + resource combo should only ever have one
                // active rule, which is what this constraint enforces.
                @UniqueConstraint(columnNames = {"client_id", "resource"})
        }
)
@Getter
@Setter
@NoArgsConstructor   // required by JPA/Hibernate to instantiate rows from the DB
@AllArgsConstructor  // convenient for tests and the @Builder below
@Builder
public class RateLimitRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who is being rate limited — an API key, a tenant ID, a service name, etc. */
    @NotBlank
    private String clientId;

    /** What they're being rate limited on, e.g. "/api/orders" or "search-endpoint". */
    @NotBlank
    private String resource;

    /** Which algorithm to enforce this rule with — see RateLimitAlgorithm for the tradeoffs. */
    @Enumerated(EnumType.STRING)
    private RateLimitAlgorithm algorithm;

    /**
     * How many requests are allowed in one window.
     * Renamed to "request_limit" in the database because LIMIT is a reserved
     * SQL keyword and would otherwise break table creation on most databases.
     */
    @Min(1)
    @Column(name = "request_limit")
    private long limit;

    /** Window size in milliseconds for SLIDING_WINDOW, or the refill period for TOKEN_BUCKET. */
    @Min(1)
    private long windowMillis;

    /**
     * TOKEN_BUCKET only: how many tokens get added back per second.
     * If left null, we calculate it as (limit / windowMillis) so you don't
     * have to work out the math by hand when creating a rule.
     */
    private Double refillRatePerSecond;

    /** Lets an admin "pause" a rule (e.g. during an incident) without deleting it. */
    private boolean enabled = true;
}
