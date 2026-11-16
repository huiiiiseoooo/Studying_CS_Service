package com.cstudy.viewcount.repository;

/** 기간 합계 조회 결과 1행. */
public interface MediumViewSum {

    Long getMediumCategoryId();

    Long getTotalViews();
}
