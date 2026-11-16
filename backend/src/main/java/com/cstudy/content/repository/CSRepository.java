package com.cstudy.content.repository;

import com.cstudy.content.domain.CSModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CSRepository extends JpaRepository<CSModel, Long> {

    Optional<CSModel> findByMinorCategoryId(Long minorCategoryId);
}
