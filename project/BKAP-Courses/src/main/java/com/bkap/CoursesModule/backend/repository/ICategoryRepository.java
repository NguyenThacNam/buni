package com.bkap.CoursesModule.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bkap.CoursesModule.entity.Category;

public interface ICategoryRepository extends JpaRepository<Category, Short> {

	/** Đối chiếu khi đồng bộ: danh mục này đã kéo từ Moodle về chưa. */
	Optional<Category> findByMoodleCategoryId(Integer moodleCategoryId);

	boolean existsBySlug(String slug);

	/** Tìm theo tên hoặc slug. q = "" thì khớp tất cả. */
	@Query("""
			SELECT c FROM Category c
			WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :q, '%'))
			""")
	Page<Category> timKiem(@Param("q") String q, Pageable pageable);
}
