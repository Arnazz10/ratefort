package com.ratefort.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ratefort.security")
public class ApiSecurityProperties {
    private List<String> validApiKeys = new ArrayList<>();
}
