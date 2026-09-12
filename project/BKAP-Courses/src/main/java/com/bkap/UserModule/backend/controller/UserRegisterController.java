package com.bkap.UserModule.backend.controller;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.UserModule.backend.service.IMoodleService;
import com.bkap.UserModule.backend.service.IRefreshTokenService;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.dto.MoodleUserRequest;
import com.bkap.UserModule.entity.User;
import com.bkap.UserModule.form.UserFormForRegister;
import com.bkap.config.JwtUtil;

@RestController
@RequestMapping("/api/v1/usersRegister")
public class UserRegisterController {

	@Autowired
	private IUserService userService;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private IMoodleService moodleService;

	@Autowired
	private IRefreshTokenService refreshTokenService;

	// ═════════════════════════════════════════════════════════════════════════
	// ĐÃ TẮT TỰ ĐĂNG KÝ TÀI KHOẢN (11/09/2026)
	//
	// Trung tâm chuyển sang mô hình cấp phát: admin tạo tài khoản cho học viên ở
	// /admin/user, rồi chỉ định khóa được học ở /admin/enrollment. Người ngoài
	// không tự tạo tài khoản được nữa.
	//
	// Chỉ ẩn nút ở frontend thì chưa đủ — ai biết địa chỉ vẫn POST thẳng vào
	// đây được. Nên tắt hẳn endpoint, và bỏ luôn permitAll trong WebConfiguration.
	// ═════════════════════════════════════════════════════════════════════════
	// @PostMapping()
	// public ResponseEntity<?> registerCustomer(@RequestBody UserFormForRegister form) {
	// try {
	// // === BƯỚC 1: VALIDATE ===
	// if (userService.existsByUsername(form.getUsername())) {
	// return ResponseEntity.status(400).body("Username đã được đăng ký!");
	// }
	// if (userService.existsByPhone(form.getPhone())) {
	// return ResponseEntity.status(400).body("Số điện thoại đã được đăng ký!");
	// }

	// // === BƯỚC 2: TẠO USER TRONG DB NỘI BỘ ===
	// User newUser = userService.createCustomer(form);

	// // === BƯỚC 3: TẠO JWT TOKEN ===
	// String token = jwtUtil.generateToken(newUser.getUsername(), newUser.getRole().name());

	// // === BƯỚC 4: ĐỒNG BỘ SANG LMS MOODLE ===
	// syncUserToMoodle(newUser, form.getPassword());

	// // === BƯỚC 5: TRẢ KẾT QUẢ VỀ FRONTEND ===
	// Map<String, Object> userInfo = buildUserResponse(newUser);

	// Map<String, Object> response = new HashMap<>();
	// response.put("user", userInfo);
	// response.put("token", token);
	// // Đăng ký xong là đăng nhập luôn, nên cấp kèm refresh token giống hệt
	// // luồng đăng nhập — thiếu nó thì 15 phút sau người vừa đăng ký bị văng ra.
	// response.put("refreshToken", refreshTokenService.cap(newUser));

	// return ResponseEntity.status(201).body(response);

	// } catch (Exception e) {
	// return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
	// }
	// }

	// ─────────────────────────────────────────────
	// PRIVATE HELPERS — CHUẨN HÓA DỮ LIỆU ĐẦU RA
	// ─────────────────────────────────────────────

	/**
	 * Đã sửa đổi: Bọc đối tượng MoodleUserRequest vào cấu trúc Map "users" để đáp
	 * ứng đúng định dạng mảng bắt buộc của hàm core_user_create_users.
	 */
	private void syncUserToMoodle(User user, String plainPassword) {
		try {
			// Phòng thủ: Nếu user null thì dừng luôn, tránh lỗi NullPointerException
			if (user == null) {
				System.err.println("[Register] Lỗi: Đối tượng User truyền vào syncUserToMoodle bị null!");
				return;
			}

			MoodleUserRequest moodleUser = new MoodleUserRequest();

			// Moodle bắt buộc username phải viết thường hoàn toàn
			moodleUser.setUsername(user.getUsername() != null ? user.getUsername().trim().toLowerCase() : null);
			moodleUser.setPassword(plainPassword != null ? plainPassword.trim() : null);
			moodleUser.setFirstname(extractFirstName(user.getFullname()));
			moodleUser.setLastname(extractLastName(user.getFullname()));
			moodleUser.setEmail(user.getEmail() != null ? user.getEmail().trim().toLowerCase() : null);

			// Gọi trực tiếp Service (Đoạn bạn vừa sửa rất chuẩn)
			String moodleResponse = moodleService.createMoodleUser(moodleUser);
			System.out.println("[Register] Đồng bộ Moodle phản hồi thành công: " + moodleResponse);

		} catch (Exception ex) {
			// Bẫy lỗi in ra cả Trace để dễ debug nếu sau này đổi hệ thống Moodle khác
			System.err.println("[Register] Lỗi hệ thống khi gọi Moodle Service: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private Map<String, Object> buildUserResponse(User user) {
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("name", user.getFullname());
		userInfo.put("email", user.getEmail());
		userInfo.put("username", user.getUsername());
		userInfo.put("phone", user.getPhone());
		userInfo.put("birthday", user.getBirthday());
		userInfo.put("role", user.getRole());
		userInfo.put("status", true);

		String formattedDate = new SimpleDateFormat("dd-MM-yyyy").format(user.getCreateDate());
		userInfo.put("register_date", formattedDate);

		return userInfo;
	}

	private String extractFirstName(String fullName) {
		if (fullName == null || fullName.trim().isEmpty()) {
			return "User";
		}
		String[] parts = fullName.trim().split("\\s+");
		return parts[parts.length - 1];
	}

	private String extractLastName(String fullName) {
		if (fullName == null || fullName.trim().isEmpty()) {
			return "Khách";
		}
		String[] parts = fullName.trim().split("\\s+");
		if (parts.length <= 1) {
			return "Nguyễn";
		}
		return String.join(" ", Arrays.asList(parts).subList(0, parts.length - 1));
	}
}