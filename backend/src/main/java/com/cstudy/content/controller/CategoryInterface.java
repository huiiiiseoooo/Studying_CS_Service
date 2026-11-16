package com.cstudy.content.controller;

import com.cstudy.content.dto.CategoryListResponse;

/** 대·중·소 분류 컨트롤러의 공통 규약: 부모 분류에 속한 목록을 돌려준다. */
public interface CategoryInterface {

    /**
     * @param parentId 부모 분류 ID. 대분류 목록은 부모가 없으므로 null.
     */
    CategoryListResponse getCategoryList(Long parentId);
}
