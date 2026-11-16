package com.cstudy.content.controller;

import com.cstudy.content.domain.CategoryModel;
import com.cstudy.content.dto.CategoryListResponse;
import com.cstudy.content.service.CSService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MediumCategoryController implements CategoryInterface {

    private final CSService csService;

    public MediumCategoryController(CSService csService) {
        this.csService = csService;
    }

    /** 대분류에 속한 중분류 목록. (FR-002) */
    @Override
    @GetMapping("/api/major-categories/{majorId}/medium-categories")
    public CategoryListResponse getCategoryList(@PathVariable("majorId") Long majorId) {
        return csService.getCategoryList(majorId, CategoryModel.LEVEL_MAJOR);
    }
}
