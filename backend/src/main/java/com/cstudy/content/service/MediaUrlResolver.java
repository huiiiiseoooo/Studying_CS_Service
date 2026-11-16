package com.cstudy.content.service;

import com.cstudy.common.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * DB에 저장된 파일 위치를 브라우저가 접근할 수 있는 전체 URL로 바꾼다.
 * - 'http(s)://' 로 시작하면 그대로 사용
 * - 그 외(예: /media/animations/stack-push.gif)는 MEDIA_BASE_URL 을 앞에 붙임
 * 덕분에 로컬 → S3 로 옮길 때 MEDIA_BASE_URL 만 바꾸면 된다.
 */
@Component
public class MediaUrlResolver {

    private final String baseUrl;

    public MediaUrlResolver(AppProperties properties) {
        String configured = properties.media().baseUrl();
        this.baseUrl = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
    }

    public String resolve(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (stored.startsWith("http://") || stored.startsWith("https://")) {
            return stored;
        }
        return baseUrl + (stored.startsWith("/") ? stored : "/" + stored);
    }
}
