package com.cstudy.viewcount.dto;

import com.cstudy.viewcount.domain.CategoryViewStat;
import java.time.LocalDate;
import java.util.List;

/** 실제로 집계에 사용한 기간과 중분류별 합계. 조회 기록이 없는 중분류도 0으로 포함한다. */
public record StatisticsResponse(LocalDate start, LocalDate end, List<CategoryViewStat> stats) {
}
