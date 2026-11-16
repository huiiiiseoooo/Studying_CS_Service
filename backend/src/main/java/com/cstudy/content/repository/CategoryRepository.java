package com.cstudy.content.repository;

import com.cstudy.content.domain.CategoryModel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryModel, Long> {

    List<CategoryModel> findByParentIdOrderBySortOrderAscIdAsc(Long parentId);

    List<CategoryModel> findByLevelOrderBySortOrderAscIdAsc(int level);
}
