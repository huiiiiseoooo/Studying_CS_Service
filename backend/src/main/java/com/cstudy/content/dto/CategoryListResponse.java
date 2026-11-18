package com.cstudy.content.dto;

import java.util.List;

/**
 * 분류 목록 응답.
 *
 * @param path     현재 위치까지의 상위 분류 (대 → 중 순서). 대분류 목록에서는 비어 있다.
 * @param overview 현재 위치한 분류 자체의 개념 설명(마크다운). 소분류 목록(중분류 안)에서만 채워지고, 없으면 null.
 * @param items    이 위치에 속한 하위 분류 목록
 */
public record CategoryListResponse(List<CategoryResponse> path, String overview, List<CategoryResponse> items) {
}
