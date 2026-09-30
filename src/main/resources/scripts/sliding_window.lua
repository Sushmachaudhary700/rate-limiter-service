-- Sliding Window Counter Rate Limiter (approximated, weighted average of
-- current + previous fixed windows -- cheap alternative to a sliding log)
-- KEYS[1] = current window key   (e.g. "rl:sw:{clientId}:{windowId}")
-- KEYS[2] = previous window key  (e.g. "rl:sw:{clientId}:{windowId-1}")
-- ARGV[1] = limit (max requests per window)
-- ARGV[2] = window_size_ms
-- ARGV[3] = elapsed_in_current_window_ms
-- ARGV[4] = requested_count (how many "requests" this call counts as, usually 1)
--
-- Returns: { allowed(1/0), effective_count, retry_after_ms }

local current_key = KEYS[1]
local previous_key = KEYS[2]
local limit = tonumber(ARGV[1])
local window_size_ms = tonumber(ARGV[2])
local elapsed_ms = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local current_count = tonumber(redis.call("GET", current_key) or "0")
local previous_count = tonumber(redis.call("GET", previous_key) or "0")

-- Weight the previous window by how much of it still "overlaps" the sliding window
local weight = math.max(0, (window_size_ms - elapsed_ms) / window_size_ms)
local effective_count = (previous_count * weight) + current_count

local allowed = 0
local retry_after_ms = 0

if effective_count + requested <= limit then
    redis.call("INCRBY", current_key, requested)
    redis.call("PEXPIRE", current_key, window_size_ms * 2)
    allowed = 1
    effective_count = effective_count + requested
else
    retry_after_ms = window_size_ms - elapsed_ms
end

return { allowed, math.floor(effective_count), retry_after_ms }
