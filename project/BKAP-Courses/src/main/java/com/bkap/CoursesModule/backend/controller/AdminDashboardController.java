package com.bkap.CoursesModule.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Cửa vào khu quản trị.
 *
 * Trang tổng quan cũ (đếm khóa học, người dùng, ghi danh) đã bỏ cùng với các
 * trang quản trị khác: những con số đó nay nằm bên LMS, đọc lại từ đó chỉ để
 * hiện lên là tốn lời gọi mà không thêm giá trị gì — Moodle đã có sẵn báo cáo.
 *
 * Khu quản trị của buni giờ chỉ còn đúng một việc: nhập học viên từ Excel.
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

	/**
	 * Trang đăng nhập của khu quản trị.
	 *
	 * Chỉ phục vụ GET để hiển thị form. Phần POST do Spring Security tự xử lý qua
	 * loginProcessingUrl("/admin/login") khai trong WebConfiguration.
	 */
	@GetMapping("/login")
	public String login() {
		return "admin/login";
	}

	/** Vào /admin thì đi thẳng tới trang duy nhất còn lại. */
	@GetMapping
	public String vaoKhuQuanTri() {
		return "redirect:/admin/user/import";
	}
}
