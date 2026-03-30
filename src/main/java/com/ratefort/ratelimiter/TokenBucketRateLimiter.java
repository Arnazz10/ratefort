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
public class TokenBucketRateLimiter implements RateLimiter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisScript<List> script;

    public TokenBucketRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>(new ClassPathResource("lua/token_bucket.lua"), List.class);
    }

    @Override
    public Mono<RateLimitResult> check(String key, int limit, int durationSeconds) {
        double refillRate = (double) limit / durationSeconds;
        long now = Instant.now().getEpochSecond();

        return redisTemplate.execute(script, 
                List.of("ratelimit:tb:" + key), 
                List.of(String.valueOf(limit), String.valueOf(refillRate), String.valueOf(now)))
                .next()
                .map(result -> new RateLimitResult(
                        ((Long) result.get(0)) == 1,
                        (Long) result.get(1))
                );
    }
}
