package com.ratefort.ratelimiter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class SlidingWindowRateLimiter implements RateLimiter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisScript<List> script;

    public SlidingWindowRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>(new ClassPathResource("lua/sliding_window.lua"), List.class);
    }

    @Override
    public Mono<RateLimitResult> check(String key, int limit, int durationSeconds) {
        long now = Instant.now().toEpochMilli();

        return redisTemplate.execute(script, 
                List.of("ratelimit:sw:" + key), 
                List.of(String.valueOf(limit), String.valueOf(durationSeconds), String.valueOf(now)))
                .next()
                .map(result -> new RateLimitResult(
                        ((Long) result.get(0)) == 1,
                        (Long) result.get(1))
                );
    }
}
