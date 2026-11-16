package com.cstudy.content.dto;

import java.util.List;

/**
 * 학습 화면 응답. 설명(텍스트)만 담고, 영상·이미지는 URL만 내려서 브라우저가 따로 불러오게 한다. (QC-001)
 *
 * @param path 대 → 중 → 소 분류 경로
 */
public record ContentResponse(Long id, String title, String explanation, String videoUrl,
                              String fallbackImageUrl, List<CategoryResponse> path) {
}
