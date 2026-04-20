package com.ratefort.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

class SlidingWindowRateLimiterTest {

    private ReactiveRedisTemplate<String, String> redisTemplate;
    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        rateLimiter = new SlidingWindowRateLimiter(redisTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkReturnsAllowedWhenScriptAllows() {
        Mockito.when(redisTemplate.execute(Mockito.any(RedisScript.class), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Flux.just(List.of(1L, 0L)));

        StepVerifier.create(rateLimiter.check("client-b", 5, 60))
                .expectNextMatches(result -> result.isAllowed() && result.getRetryAfter() == 0L)
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkReturnsBlockedWhenScriptRejects() {
        Mockito.when(redisTemplate.execute(Mockito.any(RedisScript.class), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Flux.just(List.of(0L, 19L)));

        StepVerifier.create(rateLimiter.check("client-b", 5, 60))
                .expectNextMatches(result -> !result.isAllowed() && result.getRetryAfter() == 19L)
                .verifyComplete();
    }
}
