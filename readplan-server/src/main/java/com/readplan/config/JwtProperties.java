package com.readplan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "readplan.security")
public class JwtProperties {

    private String jwtSecret;
    private long jwtExpireMinutes;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtExpireMinutes() {
        return jwtExpireMinutes;
    }

    public void setJwtExpireMinutes(long jwtExpireMinutes) {
        this.jwtExpireMinutes = jwtExpireMinutes;
    }
}
