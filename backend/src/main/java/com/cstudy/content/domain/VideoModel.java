package com.cstudy.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 콘텐츠의 애니메이션(MP4/GIF)과 대체 이미지 위치. 파일 자체는 객체 스토리지에 있다. (TC-007) */
@Entity
@Table(name = "video")
public class VideoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false, unique = true)
    private Long contentId;

    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    @Column(name = "fallback_image_url", nullable = false, length = 500)
    private String fallbackImageUrl;

    protected VideoModel() {
    }

    public Long getId() {
        return id;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getFallbackImageUrl() {
        return fallbackImageUrl;
    }
}
