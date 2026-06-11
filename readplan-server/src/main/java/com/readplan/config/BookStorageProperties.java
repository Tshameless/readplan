package com.readplan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "readplan.storage")
public record BookStorageProperties(String bookDir) {
}
