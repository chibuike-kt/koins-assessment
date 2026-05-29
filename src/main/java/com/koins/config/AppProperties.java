package com.koins.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Paystack paystack = new Paystack();
    private Otp otp = new Otp();

    @Data
    public static class Jwt {
        private String secret;
        private long expiration;
    }

    @Data
    public static class Paystack {
        private String secretKey;
        private String baseUrl;
    }

    @Data
    public static class Otp {
        private int expirationMinutes;
    }
}
