package com.cstudy.content.repository;

import com.cstudy.content.domain.VideoModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<VideoModel, Long> {

    Optional<VideoModel> findByContentId(Long contentId);
}
