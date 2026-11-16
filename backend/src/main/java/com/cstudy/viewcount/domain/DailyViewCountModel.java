package com.cstudy.viewcount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** 중분류 + 날짜별 누적 조회수 1행. */
@Entity
@Table(name = "daily_view_count")
public class DailyViewCountModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medium_category_id", nullable = false)
    private Long mediumCategoryId;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    protected DailyViewCountModel() {
    }

    public Long getId() {
        return id;
    }

    public Long getMediumCategoryId() {
        return mediumCategoryId;
    }

    public LocalDate getViewDate() {
        return viewDate;
    }

    public long getViewCount() {
        return viewCount;
    }
}
