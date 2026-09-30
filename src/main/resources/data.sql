-- Demo rules, inserted on startup. ON CONFLICT DO NOTHING means this is
-- safe to run every time the app restarts (won't duplicate rows).
INSERT INTO rate_limit_rules (id, client_id, resource, algorithm, request_limit, window_millis, refill_rate_per_second, enabled)
VALUES
    (1, 'demo-client', '/api/orders', 'TOKEN_BUCKET', 10, 10000, 1.0, true),
    (2, 'demo-client', '/api/search', 'SLIDING_WINDOW', 5, 10000, NULL, true)
ON CONFLICT (id) DO NOTHING;
