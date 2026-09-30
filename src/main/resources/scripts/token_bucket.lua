-- Token Bucket Rate Limiter
-- KEYS[1] = bucket key (e.g. "rl:tb:{clientId}")
-- ARGV[1] = capacity (max tokens)
-- ARGV[2] = refill_rate (tokens per second)
-- ARGV[3] = now (current unix time in millis)
-- ARGV[4] = requested_tokens (usually 1)
--
-- Returns: { allowed(1/0), tokens_remaining, retry_after_ms }

local bucket_key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local bucket = redis.call("HMGET", bucket_key, "tokens", "last_refill")
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

if tokens == nil then
    tokens = capacity
    last_refill = now
end

-- Refill based on elapsed time
local elapsed_seconds = math.max(0, (now - last_refill) / 1000)
local refill_amount = elapsed_seconds * refill_rate
tokens = math.min(capacity, tokens + refill_amount)

local allowed = 0
local retry_after_ms = 0

if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
else
    local deficit = requested - tokens
    retry_after_ms = math.ceil((deficit / refill_rate) * 1000)
end

redis.call("HMSET", bucket_key, "tokens", tokens, "last_refill", now)
-- Expire the key if bucket is idle long enough to fully refill twice over,
-- so we don't leak memory for clients that stop sending requests.
local ttl_seconds = math.ceil((capacity / refill_rate) * 2)
redis.call("EXPIRE", bucket_key, ttl_seconds)

return { allowed, math.floor(tokens), retry_after_ms }
