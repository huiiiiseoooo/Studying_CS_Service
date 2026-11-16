package com.cstudy.content.controller;

import com.cstudy.content.domain.CategoryModel;
import com.cstudy.content.dto.CategoryListResponse;
import com.cstudy.content.dto.ContentResponse;
import com.cstudy.content.service.CSService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MinorCategoryController implements CategoryInterface {

    private final CSService csService;

    public MinorCategoryController(CSService csService) {
        this.csService = csService;
    }

    /** 중분류에 속한 소분류 목록. (FR-003) */
    @Override
    @GetMapping("/api/medium-categories/{mediumId}/minor-categories")
    public CategoryListResponse getCategoryList(@PathVariable("mediumId") Long mediumId) {
        return csService.getCategoryList(mediumId, CategoryModel.LEVEL_MEDIUM);
    }

    /** 소분류의 학습 콘텐츠. (FR-004) */
    @GetMapping("/api/minor-categories/{minorId}/content")
    public ContentResponse getContent(@PathVariable("minorId") Long minorId) {
        return csService.getContent(minorId);
    }
}
