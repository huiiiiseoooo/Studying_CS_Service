package com.cstudy.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** '오늘'의 기준 시간대를 서버 위치와 무관하게 고정한다. (조회수 날짜, 통계 기간) */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.timeZone()));
    }
}
