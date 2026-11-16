package com.cstudy.viewcount.service;

import com.cstudy.common.error.ApiException;
import com.cstudy.content.dto.CategoryResponse;
import com.cstudy.content.service.CSService;
import com.cstudy.viewcount.domain.CategoryViewStat;
import com.cstudy.viewcount.dto.StatisticsResponse;
import com.cstudy.viewcount.repository.LookUpRepository;
import com.cstudy.viewcount.repository.MediumViewSum;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 조회수 기록과 통계.
 * 분류 정보가 필요할 때는 content 모듈의 Service 만 호출한다. (QC-010)
 */
@Service
public class LookUpService {

    private final LookUpRepository lookUpRepository;
    private final CSService csService;
    private final Clock clock;

    public LookUpService(LookUpRepository lookUpRepository, CSService csService, Clock clock) {
        this.lookUpRepository = lookUpRepository;
        this.csService = csService;
        this.clock = clock;
    }

    /** 중분류 선택 1회를 오늘 날짜 조회수에 더한다. (FR-006) */
    @Transactional
    public void recordView(Long mediumCategoryId) {
        boolean exists = csService.getMediumCategories().stream()
                .anyMatch(c -> c.id().equals(mediumCategoryId));
        if (!exists) {
            throw ApiException.notFound("분류를 찾을 수 없습니다.");
        }
        lookUpRepository.incrementViewCount(mediumCategoryId, LocalDate.now(clock));
    }

    /**
     * 기간 내 중분류별 조회수 합계. (FR-007) 시작일·종료일 모두 포함하며, 비어 있으면 오늘로 본다.
     * 조회만 하고 데이터를 바꾸지 않는다.
     */
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = start == null ? today : start;
        LocalDate to = end == null ? today : end;
        if (!validatePeriod(from, to, today)) {
            throw ApiException.badRequest("INVALID_PERIOD", "조회 기간을 다시 선택해 주세요.");
        }

        Map<Long, Long> totals = lookUpRepository.sumViewsByMediumCategory(from, to).stream()
                .collect(Collectors.toMap(MediumViewSum::getMediumCategoryId, MediumViewSum::getTotalViews));

        List<CategoryViewStat> stats = csService.getMediumCategories().stream()
                .map((CategoryResponse c) -> new CategoryViewStat(c.id(), c.name(), totals.getOrDefault(c.id(), 0L)))
                .toList();
        return new StatisticsResponse(from, to, stats);
    }

    /** 시작일이 종료일보다 늦거나 미래 날짜가 포함되면 잘못된 기간이다. (UC-004 5a) */
    private boolean validatePeriod(LocalDate start, LocalDate end, LocalDate today) {
        return !start.isAfter(end) && !end.isAfter(today);
    }
}
