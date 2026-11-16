package com.cstudy.viewcount.domain;

/** 중분류별 조회수 합계 (통계 그래프의 막대 1개). */
public record CategoryViewStat(Long mediumCategoryId, String mediumCategoryName, long totalViews) {
}
