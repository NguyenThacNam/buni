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

import com.bkap.UserModule.backend.service.IRefreshTokenService;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.dto.UserResponseDTO;
import com.bkap.UserModule.entity.User;
import com.bkap.config.JwtUtil;

@RestController
@RequestMapping(value = "api/v1/user")
public class UserController {

	@Autowired
	private IUserService userService;

	@Autowired
	private IRefreshTokenService refreshTokenService;

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

		String username = principal.getName();
		UserResponseDTO userProfile = userService.getUserByUsername(username);

		return ResponseEntity.ok(userProfile);
	}

	@PutMapping("/profile")
	public ResponseEntity<UserResponseDTO> updateUserProfile(Principal principal,
			@RequestBody UserResponseDTO updateRequest) {

		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		String username = principal.getName();
		UserResponseDTO updated = userService.updateUserProfile(username, updateRequest);
		return ResponseEntity.ok(updated);
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

		User user;
		try {
			user = userService.doiMatKhau(principal.getName(), body.get("currentPassword"),
					body.get("newPassword"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}

		refreshTokenService.thuHoiTatCa(user);

		return ResponseEntity.ok(Map.of("message", "Đã đổi mật khẩu", "token",
				jwtUtil.generateToken(user.getUsername(), user.getRole().name()), "refreshToken",
				refreshTokenService.cap(user)));
	}

}
