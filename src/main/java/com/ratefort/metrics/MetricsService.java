package com.ratefort.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry registry;
    private final LongAdder hitCount = new LongAdder();
    private final LongAdder throttleCount = new LongAdder();

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordHit(String path, String algorithm) {
        registry.counter("rate_limit_hits_total", "path", path, "algorithm", algorithm).increment();
        hitCount.increment();
    }

    public void recordThrottle(String path, String algorithm) {
        registry.counter("rate_limit_throttles_total", "path", path, "algorithm", algorithm).increment();
        throttleCount.increment();
    }

    @Async
    @Scheduled(fixedDelayString = "${ratefort.stats.flush-interval-ms:30000}")
    public void flushStatsSnapshot() {
        long hits = hitCount.sumThenReset();
        long throttles = throttleCount.sumThenReset();
        log.info("Rate limiter stats snapshot - hits: {}, throttles: {}", hits, throttles);
    }
}
