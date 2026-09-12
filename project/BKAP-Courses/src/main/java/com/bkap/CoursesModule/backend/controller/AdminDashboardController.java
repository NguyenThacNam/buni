package com.bkap.CoursesModule.backend.controller;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bkap.CoursesModule.backend.service.ICategoryService;
import com.bkap.CoursesModule.backend.service.ICourseService;
import com.bkap.CoursesModule.backend.service.IEnrollmentService;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.CoursesModule.entity.Enrollment;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.entity.UserRole;

/**
 * Trang tổng quan của khu quản trị.
 *
 * Trước đây vào /admin là 404, phải nhớ chính xác /admin/course mới vào được.
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

	@Autowired
	private ICourseService courseService;

	@Autowired
	private ICategoryService categoryService;

	@Autowired
	private IUserService userService;

	@Autowired
	private IEnrollmentService enrollmentService;

	/**
	 * Trang đăng nhập của khu quản trị.
	 *
	 * Chỉ phục vụ GET để hiển thị form. Phần POST do Spring Security tự xử lý qua
	 * loginProcessingUrl("/admin/login") khai trong WebConfiguration, nên ở đây
	 * không có handler POST nào cả.
	 */
	@GetMapping("/login")
	public String login() {
		return "admin/login";
	}

	@GetMapping
	public String dashboard(Model model) {
		List<Course> courses = courseService.getAllCourses();
		List<Enrollment> enrollments = enrollmentService.findAll();

		model.addAttribute("soKhoaHoc", courses.size());
		model.addAttribute("soDanhMuc", categoryService.findAll().size());
		model.addAttribute("soNguoiDung", userService.count());
		model.addAttribute("soAdmin", userService.countByRole(UserRole.ADMIN));
		model.addAttribute("soGhiDanh", enrollments.size());
		model.addAttribute("soDangHoc", enrollmentService.countByStatus(Enrollment.STATUS_IN_PROGRESS));
		model.addAttribute("soHoanThanh", enrollmentService.countByStatus(Enrollment.STATUS_COMPLETED));

		// Tổng học viên cộng dồn trên tất cả khóa học — số marketing hiển thị ngoài
		// trang chủ, khác với số lượt ghi danh thật trong bảng enrollment.
		model.addAttribute("tongHocVien",
				courses.stream().mapToInt(c -> c.getStudentCount() == null ? 0 : c.getStudentCount()).sum());

		// 5 khóa học mới nhất
		model.addAttribute("khoaHocMoi", courses.stream()
				.sorted(Comparator.comparing(Course::getId, Comparator.reverseOrder())).limit(5)
				.collect(Collectors.toList()));

		// 5 lượt ghi danh gần đây
		model.addAttribute("ghiDanhMoi", enrollments.stream()
				.sorted(Comparator.comparing(Enrollment::getId, Comparator.reverseOrder())).limit(5)
				.collect(Collectors.toList()));

		return "admin/dashboard";
	}
}
