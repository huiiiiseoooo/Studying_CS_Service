package com.cstudy.content.service;

import com.cstudy.common.error.ApiException;
import com.cstudy.content.domain.CSModel;
import com.cstudy.content.domain.CategoryModel;
import com.cstudy.content.domain.VideoModel;
import com.cstudy.content.dto.CategoryListResponse;
import com.cstudy.content.dto.CategoryResponse;
import com.cstudy.content.dto.ContentResponse;
import com.cstudy.content.repository.CSRepository;
import com.cstudy.content.repository.CategoryRepository;
import com.cstudy.content.repository.VideoRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분류 탐색과 학습 콘텐츠 조회. 모든 메서드는 읽기 전용이며 결과를 캐싱한다. (TC-004, NFR-001)
 */
@Service
@Transactional(readOnly = true)
public class CSService {

    private final CategoryRepository categoryRepository;
    private final CSRepository csRepository;
    private final VideoRepository videoRepository;
    private final MediaUrlResolver mediaUrlResolver;

    public CSService(CategoryRepository categoryRepository, CSRepository csRepository,
                     VideoRepository videoRepository, MediaUrlResolver mediaUrlResolver) {
        this.categoryRepository = categoryRepository;
        this.csRepository = csRepository;
        this.videoRepository = videoRepository;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    /** 대분류 목록. (FR-001) */
    @Cacheable("majorCategories")
    public CategoryListResponse getMajorCategoryList() {
        return new CategoryListResponse(List.of(),
                toResponses(categoryRepository.findByLevelOrderBySortOrderAscIdAsc(CategoryModel.LEVEL_MAJOR)));
    }

    /**
     * 부모 분류에 속한 하위 분류 목록. (FR-002, FR-003)
     *
     * @param parentId    부모 분류 ID
     * @param parentLevel 부모가 가져야 하는 단계 (대분류의 하위를 찾으면 1, 중분류의 하위를 찾으면 2)
     */
    @Cacheable(value = "categoryLists", key = "#parentLevel + ':' + #parentId")
    public CategoryListResponse getCategoryList(Long parentId, int parentLevel) {
        CategoryModel parent = categoryRepository.findById(parentId)
                .filter(c -> c.getLevel() == parentLevel)
                .orElseThrow(() -> ApiException.notFound("분류를 찾을 수 없습니다."));
        return new CategoryListResponse(pathTo(parent),
                toResponses(categoryRepository.findByParentIdOrderBySortOrderAscIdAsc(parentId)));
    }

    /** 소분류의 학습 콘텐츠. (FR-004) */
    @Cacheable("contents")
    public ContentResponse getContent(Long minorId) {
        CategoryModel minor = categoryRepository.findById(minorId)
                .filter(c -> c.getLevel() == CategoryModel.LEVEL_MINOR)
                .orElseThrow(CSService::contentUnavailable);
        CSModel content = csRepository.findByMinorCategoryId(minorId)
                .orElseThrow(CSService::contentUnavailable);
        // 영상 정보가 없어도 설명은 제공한다. (QC-006)
        VideoModel video = videoRepository.findByContentId(content.getId()).orElse(null);
        return new ContentResponse(
                content.getId(),
                content.getTitle(),
                content.getExplanation(),
                video == null ? null : mediaUrlResolver.resolve(video.getVideoUrl()),
                video == null ? null : mediaUrlResolver.resolve(video.getFallbackImageUrl()),
                pathTo(minor));
    }

    /** 전체 중분류 목록. 다른 모듈(viewcount)이 통계·검증에 사용한다. */
    @Cacheable("mediumCategories")
    public List<CategoryResponse> getMediumCategories() {
        return toResponses(categoryRepository.findByLevelOrderBySortOrderAscIdAsc(CategoryModel.LEVEL_MEDIUM));
    }

    private static ApiException contentUnavailable() {
        return ApiException.notFound("해당 콘텐츠를 이용할 수 없습니다.");
    }

    /** 최상위 대분류부터 주어진 분류까지의 경로. */
    private List<CategoryResponse> pathTo(CategoryModel category) {
        List<CategoryResponse> path = new ArrayList<>();
        CategoryModel current = category;
        while (current != null && path.size() < CategoryModel.LEVEL_MINOR) {
            path.add(CategoryResponse.from(current));
            current = current.getParentId() == null
                    ? null
                    : categoryRepository.findById(current.getParentId()).orElse(null);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private static List<CategoryResponse> toResponses(List<CategoryModel> categories) {
        return categories.stream().map(CategoryResponse::from).toList();
    }
}
