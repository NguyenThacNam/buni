package com.bkap.CoursesModule.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // ← THÊM import
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.service.ICourseService;
import com.bkap.CoursesModule.dto.CourseDTO;
import com.bkap.CoursesModule.entity.Course;

@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "*")
public class CourseController {

	@Autowired
	private ICourseService courseService;

	// GET tất cả — ai cũng xem được
	@GetMapping()
	public ResponseEntity<?> getAllCourses() {
		try {
			// Chỉ trả khóa đang hiện. Khóa vừa đồng bộ từ LMS về còn ẩn, chưa có giá
			// và chưa phân loại đúng — lọt ra trang khách là nham nhở.
			List<Course> list = courseService.getPublicCourses();
			return ResponseEntity.status(200).body(list);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Lỗi hệ thống khi lấy danh sách: " + e.getMessage());
		}
	}

	// GET theo category — ai cũng xem được
	@GetMapping("/category/{slug}")
	public ResponseEntity<?> getCoursesByCategory(@PathVariable String slug) {
		try {
			List<Course> list = courseService.getPublicCoursesByCategory(slug);
			return ResponseEntity.status(200).body(list);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Lỗi hệ thống khi lọc danh mục: " + e.getMessage());
		}
	}

	// GET chi tiết — ai cũng xem được
	@GetMapping("/{id}")
	public ResponseEntity<?> getCourseById(@PathVariable Short id) {
		try {
			Course course = courseService.getCourseById(id);
			return ResponseEntity.status(200).body(course);
		} catch (Exception e) {
			return ResponseEntity.status(404).body("Không tìm thấy khóa học với ID: " + id);
		}
	}

	// POST tạo mới — CHỈ ADMIN
	@PostMapping()
	@PreAuthorize("hasRole('ADMIN')") // ← THÊM MỚI
	public ResponseEntity<?> createCourse(@RequestBody CourseDTO courseDTO) {
		try {
			Course newCourse = courseService.createCourse(courseDTO);

			Map<String, Object> courseData = new HashMap<>();
			courseData.put("id", newCourse.getId());
			courseData.put("title", newCourse.getTitle());
			courseData.put("slug", newCourse.getSlug());
			courseData.put("subtitle", newCourse.getSubtitle());
			courseData.put("price", newCourse.getPrice());
			courseData.put("isPro", newCourse.getIsPro());
			courseData.put("category", newCourse.getCategory() != null ? newCourse.getCategory().getName() : null);
			courseData.put("instructor",
					newCourse.getInstructor() != null ? newCourse.getInstructor().getFullname() : null);

			if (newCourse.getCreatedAt() != null) {
				String formattedDate = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
						.format(newCourse.getCreatedAt());
				courseData.put("created_date", formattedDate);
			}
			courseData.put("status", true);

			Map<String, Object> response = new HashMap<>();
			response.put("course", courseData);
			response.put("message", "Tạo khóa học mới thành công!");

			return ResponseEntity.status(201).body(response);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Lỗi hệ thống khi tạo khóa học: " + e.getMessage());
		}
	}

	// PUT cập nhật — CHỈ ADMIN
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')") // ← THÊM MỚI
	public ResponseEntity<?> updateCourse(@PathVariable Short id, @RequestBody CourseDTO courseDTO) {
		try {
			Course updatedCourse = courseService.updateCourse(id, courseDTO);

			Map<String, Object> response = new HashMap<>();
			response.put("course", updatedCourse);
			response.put("message", "Cập nhật khóa học thành công!");

			return ResponseEntity.status(200).body(response);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Lỗi hệ thống khi cập nhật: " + e.getMessage());
		}
	}

	// DELETE xóa — CHỈ ADMIN
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')") // ← THÊM MỚI
	public ResponseEntity<?> deleteCourse(@PathVariable Short id) {
		try {
			courseService.deleteCourse(id);
			return ResponseEntity.status(200).body("Xóa khóa học thành công!");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Lỗi hệ thống khi xóa khóa học: " + e.getMessage());
		}
	}
}