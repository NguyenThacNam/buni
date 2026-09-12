package com.bkap.CoursesModule.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bkap.CoursesModule.entity.Course;

public interface ICourseRepository extends JpaRepository<Course, Short> {

	List<Course> findByCategory_Slug(String slug);

	/** Chỉ khóa đang hiện — dùng cho trang khách. Khóa mới đồng bộ từ LMS về
	 * còn ẩn, chưa có giá, không được lọt ra ngoài. */
	List<Course> findByStatus(String status);

	List<Course> findByCategory_SlugAndStatus(String slug, String status);

	List<Course> findByCategory_IdInAndStatus(java.util.Collection<Short> categoryIds, String status);

	/** Đối chiếu khi đồng bộ: khóa học này đã kéo từ Moodle về chưa. */
	Optional<Course> findByMoodleCourseId(Integer moodleCourseId);

	boolean existsBySlug(String slug);

	/**
	 * Tìm khóa học theo tên, slug hoặc tên danh mục.
	 *
	 * Truyền q = "" thì LIKE '%%' khớp mọi bản ghi, nên cùng một truy vấn dùng
	 * được cho cả trường hợp không tìm kiếm — không phải viết hai nhánh.
	 */
	@Query("""
			SELECT c FROM Course c
			WHERE LOWER(c.title)          LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(c.slug)           LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(c.category.name)  LIKE LOWER(CONCAT('%', :q, '%'))
			""")
	Page<Course> timKiem(@Param("q") String q, Pageable pageable);
}
