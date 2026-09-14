package com.bkap.UserModule.backend.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.UserModule.backend.service.MoodleAuthService;
import com.bkap.UserModule.dto.UserResponseDTO;
import com.bkap.config.JwtUtil;

@RestController
@RequestMapping(value = "api/v1/user")
public class UserController {

	@Autowired
	private MoodleAuthService moodleAuthService;

	@Autowired
	private JwtUtil jwtUtil;

	/**
	 * API lấy thông tin profile của user đang đăng nhập URL: GET
	 * http://localhost:8080/api/v1/user/profile
	 */
	@GetMapping("/profile")
	public ResponseEntity<UserResponseDTO> getUserProfile(Principal principal) {
		// Principal chứa thông tin username của người dùng hiện tại đang đăng nhập hệ
		// thống
		if (principal == null) {
			return ResponseEntity.status(401).build(); // Chưa đăng nhập/không có token hợp lệ
		}

		try {
			com.fasterxml.jackson.databind.JsonNode u = moodleAuthService
					.timTheoTenDangNhap(principal.getName());
			if (u == null) {
				return ResponseEntity.status(404).build();
			}

			// Hồ sơ giờ nằm bên LMS. Email và điện thoại có thể trống nếu vai trò
			// của tài khoản dịch vụ chưa được cấp quyền xem chi tiết người dùng.
			UserResponseDTO dto = new UserResponseDTO();
			dto.setUsername(u.path("username").asText(""));
			dto.setFullname(u.path("fullname").asText(""));
			dto.setEmail(u.path("email").asText(""));
			dto.setPhone(u.path("phone1").asText(""));
			dto.setRole("STUDENT");
			// Moodle không có trường ngày sinh.
			dto.setBirthday(null);
			return ResponseEntity.ok(dto);

		} catch (Exception e) {
			System.err.println("[User] Không lấy được hồ sơ: " + e.getMessage());
			return ResponseEntity.status(502).build();
		}
	}

	@PutMapping("/profile")
	public ResponseEntity<UserResponseDTO> updateUserProfile(Principal principal,
			@RequestBody UserResponseDTO updateRequest) {

		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		try {
			moodleAuthService.capNhatHoSo(principal.getName(), updateRequest.getFullname(), updateRequest.getEmail(),
					updateRequest.getPhone());
			return getUserProfile(principal);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		} catch (Exception e) {
			System.err.println("[User] Không cập nhật được hồ sơ: " + e.getMessage());
			return ResponseEntity.status(502).build();
		}
	}

	/**
	 * Học viên tự đổi mật khẩu. PUT /api/v1/user/password
	 * Body: {"currentPassword": "...", "newPassword": "..."}
	 *
	 * Đổi xong thì thu hồi MỌI phiên đăng nhập của tài khoản này, rồi cấp phiên
	 * mới cho đúng thiết bị đang đổi. Lý do: người ta hay đổi mật khẩu chính vì
	 * nghi bị lộ — nếu kẻ kia đang cầm refresh token thì đổi mật khẩu mà không
	 * thu hồi cũng bằng không, họ vẫn tự làm mới phiên được thêm 7 ngày.
	 */
	@PutMapping("/password")
	public ResponseEntity<?> doiMatKhau(Principal principal, @RequestBody Map<String, String> body) {
		if (principal == null) {
			return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
		}

		try {
			moodleAuthService.doiMatKhau(principal.getName(), body.get("currentPassword"), body.get("newPassword"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			System.err.println("[User] Không đổi được mật khẩu: " + e.getMessage());
			return ResponseEntity.status(502)
					.body(Map.of("error", "Chưa kết nối được hệ thống LMS. Vui lòng thử lại sau."));
		}

		// Mật khẩu đổi bên LMS nên phiên cũ trên các máy khác coi như hết giá trị
		// về mặt mật khẩu — nhưng vé làm mới thì KHÔNG thu hồi được nữa (không còn
		// CSDL để đánh dấu). Chúng vẫn dùng được tới khi hết hạn.
		String vaiTro = principal instanceof org.springframework.security.core.Authentication a
				&& a.getAuthorities().stream().anyMatch(x -> "ROLE_ADMIN".equals(x.getAuthority())) ? "ADMIN"
						: "STUDENT";

		return ResponseEntity.ok(Map.of("message", "Đã đổi mật khẩu", "token",
				jwtUtil.generateToken(principal.getName(), vaiTro), "refreshToken",
				jwtUtil.taoRefreshToken(principal.getName(), "ADMIN".equals(vaiTro))));
	}

}
