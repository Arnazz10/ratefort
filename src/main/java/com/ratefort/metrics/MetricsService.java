package com.ratefort.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordHit(String path, String algorithm) {
        registry.counter("rate_limit_hits_total", "path", path, "algorithm", algorithm).increment();
    }

    public void recordThrottle(String path, String algorithm) {
        registry.counter("rate_limit_throttles_total", "path", path, "algorithm", algorithm).increment();
    }
}
