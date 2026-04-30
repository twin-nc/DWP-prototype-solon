package com.netcompany.dcms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dcms")
public record ApplicationProperties(
    String logLevel
) {}
