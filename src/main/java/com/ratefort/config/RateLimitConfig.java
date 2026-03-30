package com.ratefort.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ratefort")
public class RateLimitConfig {
    private List<RateLimitRule> rules;

    @Data
    public static class RateLimitRule {
        private String path;
        private int limit;
        private int durationSeconds;
        private Algorithm algorithm;
    }

    public enum Algorithm {
        TOKEN_BUCKET,
        SLIDING_WINDOW
    }
}
