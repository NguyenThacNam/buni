package com.bkap.CoursesModule.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bkap.CoursesModule.entity.Enrollment;

public interface IEnrollmentService {

	List<Enrollment> findAll();

	/** Tìm kiếm có phân trang cho danh sách admin. q = "" nghĩa là lấy tất cả. */
	Page<Enrollment> timKiem(String q, Pageable pageable);

	Enrollment findById(Integer id);

	List<Enrollment> findByUsername(String username);

	/** Lưu từ form admin. Khi sửa thì giữ nguyên enrolledAt của bản ghi cũ. */
	Enrollment save(Enrollment enrollment);

	void deleteById(Integer id);

	boolean isDuplicate(Enrollment enrollment);

	long count();

	long countByStatus(String status);

	/**
	 * Người này đã được cấp quyền học khóa này chưa. Mọi trạng thái (đã ghi
	 * danh, đang học, hoàn thành) đều tính là có — học xong vẫn được xem lại.
	 * Gỡ quyền nghĩa là xóa lượt ghi danh.
	 */
	boolean daGhiDanh(String username, Short courseId);

	/** Các khóa người này đang học, không nạp đối tượng ghi danh — xem repository. */
	List<com.bkap.CoursesModule.entity.Course> khoaCuaNguoiDung(short userId);
}
