package com.cstudy.viewcount.controller;

import com.cstudy.viewcount.dto.StatisticsResponse;
import com.cstudy.viewcount.dto.ViewRecordRequest;
import com.cstudy.viewcount.service.LookUpService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LookUpController {

    private final LookUpService lookUpService;

    public LookUpController(LookUpService lookUpService) {
        this.lookUpService = lookUpService;
    }

    /** 조회수 기록. 조회용 GET 과 분리된 별도 API. (TC-004, FR-006) */
    @PostMapping("/api/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordView(@Valid @RequestBody ViewRecordRequest request) {
        lookUpService.recordView(request.mediumCategoryId());
    }

    /** 기간별 중분류 조회수 통계. 운영자 전용(/api/admin/**). (FR-007, FR-008, NFR-002) */
    @GetMapping("/api/admin/statistics")
    public StatisticsResponse getStatistics(
            @RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return lookUpService.getStatistics(start, end);
    }
}
