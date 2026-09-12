package com.bkap.CoursesModule.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bkap.CoursesModule.dto.CourseDTO;
import com.bkap.CoursesModule.entity.Course;

public interface ICourseService {

	List<Course> getAllCourses();

	List<Course> getCoursesByCategory(String slug);

	/**
	 * Chỉ khóa đang hiện — dành cho API công khai.
	 *
	 * Tách riêng khỏi getAllCourses() vì trang quản trị và bảng tổng quan vẫn cần
	 * đếm đủ cả khóa đang ẩn.
	 */
	List<Course> getPublicCourses();

	List<Course> getPublicCoursesByCategory(String slug);

	Course getCourseById(Short id);

	Course createCourse(CourseDTO courseDTO);

	Course updateCourse(Short id, CourseDTO courseDTO);

	void deleteCourse(Short id);

	// ===== Thêm mới cho module Admin (Thymeleaf) =====
	Course save(Course course);

	/** Tìm kiếm có phân trang cho danh sách admin. q = "" nghĩa là lấy tất cả. */
	Page<Course> timKiem(String q, Pageable pageable);

}
