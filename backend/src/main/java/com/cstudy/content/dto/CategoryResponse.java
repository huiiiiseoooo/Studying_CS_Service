package com.cstudy.content.dto;

import com.cstudy.content.domain.CategoryModel;

public record CategoryResponse(Long id, String name, int level, Long parentId, String description, boolean active) {

    public static CategoryResponse from(CategoryModel category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getLevel(),
                category.getParentId(), category.getDescription(), category.isActive());
    }
}
