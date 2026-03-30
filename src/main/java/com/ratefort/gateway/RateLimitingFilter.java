package com.ratefort.gateway;

import com.ratefort.config.RateLimitConfig;
import com.ratefort.metrics.MetricsService;
import com.ratefort.ratelimiter.RateLimitResult;
import com.ratefort.ratelimiter.SlidingWindowRateLimiter;
import com.ratefort.ratelimiter.TokenBucketRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final RateLimitConfig config;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final MetricsService metricsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitingFilter(RateLimitConfig config,
                              TokenBucketRateLimiter tokenBucketRateLimiter,
                              SlidingWindowRateLimiter slidingWindowRateLimiter,
                              MetricsService metricsService) {
        this.config = config;
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.slidingWindowRateLimiter = slidingWindowRateLimiter;
        this.metricsService = metricsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        Optional<RateLimitConfig.RateLimitRule> ruleOpt = config.getRules().stream()
                .filter(r -> pathMatcher.match(r.getPath(), path))
                .findFirst();

        if (ruleOpt.isEmpty()) {
            return chain.filter(exchange);
        }

        RateLimitConfig.RateLimitRule rule = ruleOpt.get();
        String clientKey = resolveClientKey(request);
        String limiterKey = path + ":" + clientKey;

        Mono<RateLimitResult> limiterMono = rule.getAlgorithm() == RateLimitConfig.Algorithm.TOKEN_BUCKET
                ? tokenBucketRateLimiter.check(limiterKey, rule.getLimit(), rule.getDurationSeconds())
                : slidingWindowRateLimiter.check(limiterKey, rule.getLimit(), rule.getDurationSeconds());

        return limiterMono.flatMap(result -> {
            if (result.isAllowed()) {
                metricsService.recordHit(path, rule.getAlgorithm().name());
                return chain.filter(exchange);
            } else {
                metricsService.recordThrottle(path, rule.getAlgorithm().name());
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(result.getRetryAfter()));
                return exchange.getResponse().setComplete();
            }
        });
    }

    private String resolveClientKey(ServerHttpRequest request) {
        String apiKey = request.getHeaders().getFirst("X-API-KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return "apikey:" + apiKey;
        }
        return "ip:" + request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }
}
