package com.cstudy.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 분류. level 1: 대분류, 2: 중분류, 3: 소분류. */
@Entity
@Table(name = "category")
public class CategoryModel {

    public static final int LEVEL_MAJOR = 1;
    public static final int LEVEL_MEDIUM = 2;
    public static final int LEVEL_MINOR = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int level;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(length = 200)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** false 면 화면에서 '준비 중'으로 표시되고 선택할 수 없다. */
    @Column(nullable = false)
    private boolean active;

    protected CategoryModel() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
