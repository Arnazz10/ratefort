package com.ratefort.ratelimiter;

import reactor.core.publisher.Mono;

public interface RateLimiter {
    Mono<RateLimitResult> check(String key, int limit, int durationSeconds);
}
