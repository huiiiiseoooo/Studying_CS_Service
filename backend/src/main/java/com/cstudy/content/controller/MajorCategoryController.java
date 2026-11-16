package com.cstudy.content.controller;

import com.cstudy.content.dto.CategoryListResponse;
import com.cstudy.content.service.CSService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MajorCategoryController implements CategoryInterface {

    private final CSService csService;

    public MajorCategoryController(CSService csService) {
        this.csService = csService;
    }

    /** 대분류 목록. (FR-001) */
    @GetMapping("/api/major-categories")
    public CategoryListResponse getMajorCategories() {
        return getCategoryList(null);
    }

    @Override
    public CategoryListResponse getCategoryList(Long parentId) {
        return csService.getMajorCategoryList();
    }
}
