package com.bkap.CoursesModule.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bkap.CoursesModule.entity.Enrollment;

public interface IEnrollmentRepository extends JpaRepository<Enrollment, Integer> {

	List<Enrollment> findByUser_Username(String username);

	List<Enrollment> findByCourse_Id(Short courseId);

	/** Chặn ghi danh trùng — DB đã có UNIQUE (user_id, course_id), kiểm tra trước cho báo lỗi đẹp hơn. */
	boolean existsByUser_IdAndCourse_Id(short userId, Short courseId);

	long countByStatus(String status);

	/** Người này có được ghi danh vào khóa này không — dùng để chặn cửa trang học. */
	boolean existsByUser_UsernameAndCourse_Id(String username, Short courseId);

	/**
	 * Các khóa người này đang học — chỉ lấy KHÓA, không nạp đối tượng ghi danh.
	 *
	 * Dùng lúc xóa người dùng. Nạp cả Enrollment vào bộ nhớ JPA thì lúc xóa User,
	 * Hibernate thấy chúng còn trỏ tới người đang bị xóa và từ chối cả lệnh xóa.
	 * Không nạp thì CSDL tự dọn nhờ ON DELETE CASCADE.
	 */
	@Query("SELECT e.course FROM Enrollment e WHERE e.user.id = :userId")
	List<com.bkap.CoursesModule.entity.Course> timKhoaCuaNguoiDung(@Param("userId") short userId);

	/** Tìm theo tên/tên đăng nhập của học viên hoặc tên khóa học. q = "" thì khớp tất cả. */
	@Query("""
			SELECT e FROM Enrollment e
			WHERE LOWER(e.user.fullname) LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(e.user.username) LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(e.course.title)  LIKE LOWER(CONCAT('%', :q, '%'))
			""")
	Page<Enrollment> timKiem(@Param("q") String q, Pageable pageable);
}
