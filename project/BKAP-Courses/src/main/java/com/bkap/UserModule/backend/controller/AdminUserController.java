package com.bkap.UserModule.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bkap.CoursesModule.backend.service.IEnrollmentService;
import com.bkap.CoursesModule.backend.service.LmsSsoService;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.UserModule.backend.service.IRefreshTokenService;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.entity.User;
import com.bkap.UserModule.entity.UserRole;

/**
 * Quản lý người dùng — trước đây hoàn toàn không có màn hình nào, muốn phong
 * quyền ADMIN cho ai phải vào thẳng database gõ SQL.
 */
@Controller
@RequestMapping("/admin/user")
public class AdminUserController {

	@Autowired
	private IUserService userService;

	@Autowired
	private IRefreshTokenService refreshTokenService;

	@Autowired
	private IEnrollmentService enrollmentService;

	@Autowired
	private LmsSsoService lmsSsoService;

	/** Số dòng mỗi trang trong bảng danh sách. */
	private static final int SO_DONG_MOI_TRANG = 10;

	@GetMapping
	public String list(@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "page", defaultValue = "0") int page, Model model) {

		Page<User> trang = userService.timKiem(q,
				PageRequest.of(Math.max(page, 0), SO_DONG_MOI_TRANG, Sort.by("id")));

		model.addAttribute("users", trang.getContent());
		model.addAttribute("trang", trang);
		model.addAttribute("q", q);
		return "admin/user/list";
	}

	@GetMapping("/create")
	public String create(Model model) {
		model.addAttribute("user", new User());
		model.addAttribute("roles", UserRole.values());
		return "admin/user/form";
	}

	@GetMapping("/edit/{id}")
	public String edit(@PathVariable Short id, Model model, RedirectAttributes ra) {
		User user = userService.findById(id);
		if (user == null) {
			ra.addFlashAttribute("flashErr", "Không tìm thấy người dùng cần sửa.");
			return "redirect:/admin/user";
		}
		model.addAttribute("user", user);
		model.addAttribute("roles", UserRole.values());
		return "admin/user/form";
	}

	@PostMapping("/save")
	public String save(@ModelAttribute User user,
			@RequestParam(name = "newPassword", required = false) String newPassword, RedirectAttributes ra) {
		boolean isNew = user.getId() == 0;

		// Tạo mới thì bắt buộc có mật khẩu, nếu không tài khoản sẽ không đăng nhập
		// được và cũng không có cách nào đặt lại từ giao diện người dùng.
		if (isNew && (newPassword == null || newPassword.isBlank())) {
			ra.addFlashAttribute("flashErr", "Tạo người dùng mới thì phải đặt mật khẩu.");
			return "redirect:/admin/user/create";
		}

		try {
			User daLuu = userService.saveFromAdmin(user, newPassword);

			// Admin đặt lại mật khẩu (thường vì học viên quên, hoặc nghi bị lộ) thì
			// đăng xuất mọi thiết bị đang cầm phiên cũ. Không làm vậy thì ai đang giữ
			// refresh token vẫn tự làm mới phiên được, đổi mật khẩu cũng như không.
			if (!isNew && newPassword != null && !newPassword.isBlank()) {
				refreshTokenService.thuHoiTatCa(daLuu);
			}
			ra.addFlashAttribute("flashOk",
					isNew ? "Đã thêm người dùng \"" + user.getFullname() + "\"."
							: "Đã cập nhật người dùng \"" + user.getFullname() + "\".");
		} catch (Exception e) {
			// Thường là trùng username / email / số điện thoại (DB có ràng buộc UNIQUE)
			ra.addFlashAttribute("flashErr",
					"Không lưu được. Kiểm tra tên đăng nhập, email và số điện thoại xem có bị trùng không.");
		}
		return "redirect:/admin/user";
	}

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable Short id, RedirectAttributes ra) {
		// Ghi nhớ các khóa người này đang học TRƯỚC khi xóa. CSDL tự xóa theo các
		// lượt ghi danh bên buni, nhưng bên LMS thì không ai gỡ — học viên đã xóa
		// vẫn nằm trong danh sách lớp. Phải tự gỡ từng khóa.
		User user = userService.findById(id);
		java.util.List<Course> dangHoc = user == null ? java.util.List.of()
				: enrollmentService.khoaCuaNguoiDung(user.getId());

		try {
			userService.deleteById(id);
		} catch (Exception e) {
			System.err.println("[AdminUser] Không xóa được người dùng " + id + ": " + e.getMessage());
			// User đang là giảng viên của khóa học nào đó thì khóa ngoại chặn lại
			ra.addFlashAttribute("flashErr",
					"Không xóa được: người dùng này đang là giảng viên của một khóa học, "
							+ "hoặc đã có lượt ghi danh. Gỡ các liên kết đó trước.");
			return "redirect:/admin/user";
		}

		java.util.List<String> chuaGo = new java.util.ArrayList<>();
		for (Course khoa : dangHoc) {
			try {
				lmsSsoService.huyGhiDanhBenLms(user, khoa);
			} catch (Exception ex) {
				System.err.println("[AdminUser] Chưa gỡ được " + user.getUsername() + " khỏi khóa "
						+ khoa.getTitle() + " bên LMS: " + ex.getMessage());
				chuaGo.add(khoa.getTitle());
			}
		}

		if (chuaGo.isEmpty()) {
			ra.addFlashAttribute("flashOk", dangHoc.isEmpty() ? "Đã xóa người dùng."
					: "Đã xóa người dùng và gỡ khỏi " + dangHoc.size() + " khóa học.");
		} else {
			ra.addFlashAttribute("flashWarn", "Đã xóa người dùng, nhưng chưa gỡ được khỏi các khóa sau bên LMS: "
					+ String.join(", ", chuaGo) + ". Cần gỡ tay bên LMS.");
		}
		return "redirect:/admin/user";
	}
}
