-- Sliding Window Log Lua Script
-- KEYS[1] : window key (sorted set)
-- ARGV[1] : limit
-- ARGV[2] : duration (seconds)
-- ARGV[3] : now (current timestamp in milliseconds)

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local duration = tonumber(ARGV[2]) * 1000 -- Convert to ms
local now = tonumber(ARGV[3])
local window_start = now - duration

-- Remove old entries
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Count current entries
local current_count = redis.call('ZCARD', key)

local allowed = 0
local retry_after = 0

if current_count < limit then
    redis.call('ZADD', key, now, now)
    allowed = 1
else
    -- Find the oldest entry to calculate retry_after
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    if oldest[2] then
        retry_after = math.ceil((tonumber(oldest[2]) + duration - now) / 1000)
    else
        retry_after = 1 -- Fallback
    end
end

redis.call('EXPIRE', key, math.ceil(duration / 1000) + 10)

return {allowed, retry_after}
