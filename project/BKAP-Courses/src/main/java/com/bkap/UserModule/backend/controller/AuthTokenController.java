package com.bkap.UserModule.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.UserModule.backend.service.IRefreshTokenService;
import com.bkap.UserModule.backend.service.RefreshTokenService.KetQuaLamMoi;
import com.bkap.UserModule.entity.User;
import com.bkap.config.JwtUtil;

/**
 * Làm mới access token.
 *
 * Access token chỉ sống 15 phút. Khi hết hạn, frontend không bắt người dùng đăng
 * nhập lại mà tự gọi endpoint này kèm refresh token để lấy access token mới —
 * người dùng không nhận ra gì cả.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin("*")
public class AuthTokenController {

	@Autowired
	private IRefreshTokenService refreshTokenService;

	@Autowired
	private JwtUtil jwtUtil;

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
		String refreshToken = body == null ? null : body.get("refreshToken");

		Optional<KetQuaLamMoi> optional = refreshTokenService.xacMinhVaXoay(refreshToken);

		// Không hợp lệ, đã thu hồi, hoặc đã hết hạn — đều trả 401 với cùng một câu.
		// Không nói rõ sai ở đâu để kẻ dò token không suy ra được thông tin gì.
		if (optional.isEmpty()) {
			return ResponseEntity.status(401)
					.body(Map.of("error", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."));
		}

		KetQuaLamMoi ketQua = optional.get();
		User user = ketQua.user();
		String role = user.getRole() == null ? "STUDENT" : user.getRole().name();

		Map<String, Object> response = new HashMap<>();
		response.put("token", jwtUtil.generateToken(user.getUsername(), role));
		// Có xoay vòng: refresh token cũ vừa bị thu hồi, client PHẢI thay bằng cái
		// mới này, nếu không lần làm mới sau sẽ thất bại.
		response.put("refreshToken", ketQua.refreshTokenMoi());

		return ResponseEntity.ok(response);
	}
}
