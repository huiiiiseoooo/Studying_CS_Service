package com.cstudy.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** 조회용 GET 결과 캐싱. 만료 시간 등은 application.yml 의 spring.cache 에서 정한다. (TC-004) */
@Configuration
@EnableCaching
public class CacheConfig {
}
