package com.cstudy.common.config;

import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 개발 전용: S3 대신 프로젝트의 media 폴더를 /media/** 로 내려준다.
 * MEDIA_SERVE_LOCAL=true 일 때만 켜진다. 운영에서는 켜지 않고 MEDIA_BASE_URL 을 S3 주소로 지정한다. (TC-007)
 */
@Configuration
@ConditionalOnProperty(prefix = "app.media", name = "serve-local", havingValue = "true")
public class LocalMediaConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    public LocalMediaConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.media().localDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
