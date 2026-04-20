package com.ratefort.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void logThrottledRequest(String path, String clientKey, String algorithm, long retryAfter) {
        AuditLog logEntry = new AuditLog();
        logEntry.setPath(path);
        logEntry.setClientKey(clientKey);
        logEntry.setAlgorithm(algorithm);
        logEntry.setRetryAfter(retryAfter);
        logEntry.setThrottledAt(Instant.now());
        auditLogRepository.save(logEntry);
    }
}
