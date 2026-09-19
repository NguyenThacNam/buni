package com.bkap.CoursesModule.backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.service.LmsCatalogService;
import com.bkap.CoursesModule.backend.service.LmsContentService;

/**
 * Khóa học cho trang công khai — đọc từ LMS.
 *
 * Các endpoint thêm/sửa/xóa khóa học đã bỏ cùng với trang quản trị: khóa học
 * nay tạo và sửa bên lms.buni.vn, buni chỉ hiển thị. Xem lịch sử git (nhánh
 * main, trước khi chuyển sang LMS) nếu cần đọc lại mã cũ.
 */
@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "*")
public class CourseController {

	@Autowired
	private LmsCatalogService lmsCatalogService;

	@Autowired
	private LmsContentService lmsContentService;

	@GetMapping()
	public ResponseEntity<?> getAllCourses() {
		try {
			return ResponseEntity.ok(lmsCatalogService.danhSachKhoa());
		} catch (Exception e) {
			return loi("danh sách khóa học", e);
		}
	}

	/** Gồm cả khóa của các danh mục con — xem LmsCatalogService. */
	@GetMapping("/category/{slug}")
	public ResponseEntity<?> getCoursesByCategory(@PathVariable String slug) {
		try {
			return ResponseEntity.ok(lmsCatalogService.khoaTheoDanhMuc(slug));
		} catch (Exception e) {
			return loi("khóa học theo danh mục " + slug, e);
		}
	}

	/**
	 * Chương trình học để hiện ở trang giới thiệu khóa: tên chương, tên bài.
	 *
	 * Công khai như phần còn lại của /courses — chỉ có tên, không có link file hay
	 * nội dung bài, nên khách xem được chương trình mà vẫn không học lỏm được.
	 */
	@GetMapping("/{id}/outline")
	public ResponseEntity<?> getCourseOutline(@PathVariable Integer id) {
		try {
			return ResponseEntity.ok(lmsContentService.daiCuong(id));
		} catch (Exception e) {
			return loi("chương trình khóa học " + id, e);
		}
	}

	/** id là id khóa học BÊN LMS, vì buni không còn bảng khóa học riêng. */
	@GetMapping("/{id}")
	public ResponseEntity<?> getCourseById(@PathVariable Integer id) {
		try {
			Map<String, Object> course = lmsCatalogService.chiTietKhoaDayDu(id);
			if (course == null) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy khóa học " + id));
			}
			return ResponseEntity.ok(course);
		} catch (Exception e) {
			return loi("khóa học " + id, e);
		}
	}

	private ResponseEntity<?> loi(String dangLay, Exception e) {
		System.err.println("[Course] Không lấy được " + dangLay + ": " + e.getMessage());
		return ResponseEntity.status(502).body(Map.of("error", "Chưa lấy được dữ liệu từ hệ thống LMS."));
	}
}
