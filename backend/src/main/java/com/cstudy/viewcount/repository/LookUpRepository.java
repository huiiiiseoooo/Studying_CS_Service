package com.cstudy.viewcount.repository;

import com.cstudy.viewcount.domain.DailyViewCountModel;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LookUpRepository extends JpaRepository<DailyViewCountModel, Long> {

    /**
     * 해당 날짜의 행이 없으면 1로 만들고, 있으면 1 올린다.
     * DB가 한 문장으로 처리하므로 동시에 여러 요청이 와도 조회수가 누락되지 않는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO daily_view_count (medium_category_id, view_date, view_count)
            VALUES (:mediumCategoryId, :viewDate, 1)
            ON DUPLICATE KEY UPDATE view_count = view_count + 1
            """, nativeQuery = true)
    void incrementViewCount(@Param("mediumCategoryId") Long mediumCategoryId, @Param("viewDate") LocalDate viewDate);

    @Query("""
            SELECT v.mediumCategoryId AS mediumCategoryId, SUM(v.viewCount) AS totalViews
            FROM DailyViewCountModel v
            WHERE v.viewDate BETWEEN :start AND :end
            GROUP BY v.mediumCategoryId
            """)
    List<MediumViewSum> sumViewsByMediumCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
