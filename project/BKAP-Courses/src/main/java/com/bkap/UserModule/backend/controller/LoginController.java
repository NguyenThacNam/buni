package com.bkap.UserModule.backend.controller;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.UserModule.backend.service.IRefreshTokenService;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.dto.LoginRequest;
import com.bkap.UserModule.entity.User;
import com.bkap.config.JwtUtil;

@RestController
@RequestMapping("api/v1/login")
@CrossOrigin("*")
public class LoginController {

	@Autowired
	private IUserService userService;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private IRefreshTokenService refreshTokenService;

	@PostMapping()
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
		try {
			User user = userService.login(request.getUsername(), request.getPassword());

			if (user == null) {
				return ResponseEntity.status(401).body("Username hoặc mật khẩu không đúng!");
			}

			// Lấy last_login cũ từ DB trước khi cập nhật
			String lastLogin = "Chưa có lần đăng nhập trước"; // Default nếu lần đầu login
			if (user.getLastLogin() != null) {
				lastLogin = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(user.getLastLogin());
			}

			// Sau đó mới cập nhật last_login mới vào DB
			userService.updateLastLogin(request.getUsername());

			// ← Tạo JWT token
			String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

			// Format createDate
			String createDate = new SimpleDateFormat("dd-MM-yyyy").format(user.getCreateDate());

			// Build response với thông tin cũ
			Map<String, Object> userInfo = new HashMap<>();
			userInfo.put("name", user.getFullname());
			userInfo.put("email", user.getEmail());
			userInfo.put("username", user.getUsername());
			userInfo.put("phone", user.getPhone());
			userInfo.put("birthday", user.getBirthday());
			userInfo.put("last_login", lastLogin);
			userInfo.put("createDate", createDate);
			userInfo.put("status", true);

			Map<String, Object> response = new HashMap<>();
			response.put("user", userInfo);
			response.put("token", token);
			// Access token trên chỉ sống 15 phút. Kèm thêm refresh token để frontend
			// tự xin token mới khi hết hạn, khỏi bắt đăng nhập lại.
			response.put("refreshToken", refreshTokenService.cap(user));

			return ResponseEntity.status(200).body(response);

		} catch (Exception e) {
			return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
		}
	}
}
