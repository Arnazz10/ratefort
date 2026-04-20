package com.ratefort.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

class TokenBucketRateLimiterTest {

    private ReactiveRedisTemplate<String, String> redisTemplate;
    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        rateLimiter = new TokenBucketRateLimiter(redisTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkReturnsAllowedWhenScriptAllows() {
        Mockito.when(redisTemplate.execute(Mockito.any(RedisScript.class), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Flux.just(List.of(1L, 0L)));

        StepVerifier.create(rateLimiter.check("client-a", 10, 60))
                .expectNextMatches(result -> result.isAllowed() && result.getRetryAfter() == 0L)
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkReturnsBlockedWhenScriptRejects() {
        Mockito.when(redisTemplate.execute(Mockito.any(RedisScript.class), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Flux.just(List.of(0L, 12L)));

        StepVerifier.create(rateLimiter.check("client-a", 10, 60))
                .expectNextMatches(result -> !result.isAllowed() && result.getRetryAfter() == 12L)
                .verifyComplete();
    }
}
