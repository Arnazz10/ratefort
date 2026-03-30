package com.ratefort.ratelimiter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RateLimitResult {
    private final boolean allowed;
    private final long retryAfter;
}
