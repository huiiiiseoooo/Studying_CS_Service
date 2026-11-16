package com.cstudy.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 학습 콘텐츠. 소분류 1개에 1개가 연결된다. 설명은 마크다운 텍스트. */
@Entity
@Table(name = "cs_content")
public class CSModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "minor_category_id", nullable = false, unique = true)
    private Long minorCategoryId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    protected CSModel() {
    }

    public Long getId() {
        return id;
    }

    public Long getMinorCategoryId() {
        return minorCategoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getExplanation() {
        return explanation;
    }
}
