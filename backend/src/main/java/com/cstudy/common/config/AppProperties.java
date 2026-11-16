package com.cstudy.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 app.* 설정. 값은 모두 환경 변수에서 온다. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String timeZone, Jwt jwt, Cors cors, Media media, Admin admin) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Media(String baseUrl, boolean serveLocal, String localDir) {
    }

    public record Admin(String initialId, String initialPassword) {
    }
}
