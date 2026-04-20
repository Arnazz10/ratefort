package com.ratefort.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false, length = 128)
    private String clientKey;

    @Column(nullable = false, length = 64)
    private String algorithm;

    @Column(nullable = false)
    private Long retryAfter;

    @Column(nullable = false)
    private Instant throttledAt;
}
