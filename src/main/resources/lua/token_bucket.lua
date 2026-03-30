-- Token Bucket Lua Script
-- KEYS[1] : bucket key
-- ARGV[1] : max_tokens (burst capacity)
-- ARGV[2] : refill_rate (tokens per second)
-- ARGV[3] : now (current timestamp in seconds)

local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'last_refill_time')
local tokens = tonumber(bucket[1])
local last_refill_time = tonumber(bucket[2])

local max_tokens = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

if tokens == nil then
    tokens = max_tokens
    last_refill_time = now
else
    local elapsed = math.max(0, now - last_refill_time)
    tokens = math.min(max_tokens, tokens + (elapsed * refill_rate))
    last_refill_time = now
end

local allowed = 0
local retry_after = 0

if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
else
    -- Calculate retry after (how long until we have at least 1 token)
    retry_after = math.ceil((1 - tokens) / refill_rate)
end

redis.call('HMSET', KEYS[1], 'tokens', tokens, 'last_refill_time', last_refill_time)
redis.call('EXPIRE', KEYS[1], math.ceil(max_tokens / refill_rate) + 10)

return {allowed, retry_after}
