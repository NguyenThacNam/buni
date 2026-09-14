package com.bkap.UserModule.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.UserModule.dto.LogoutRequest;

/**
 * Đăng xuất.
 *
 * Trước đây endpoint này xóa refresh token trong CSDL, nên đăng xuất là phiên
 * chết thật: ai kịp sao chép token cũng không dùng tiếp được.
 *
 * Bỏ CSDL thì refresh token thành JWT tự chứng minh, máy chủ không còn chỗ ghi
 * "vé này đã hủy" — nó sống tới khi hết hạn (app.jwt.refresh-token-days). Việc
 * dọn token giờ nằm ở phía trình duyệt.
 *
 * Vẫn giữ endpoint để frontend gọi như cũ, và để nối lại chỗ thu hồi nếu sau
 * này buni có CSDL riêng.
 */
@RestController
@RequestMapping("api/v1/logout")
@CrossOrigin("*")
public class LogoutController {

	@PostMapping()
	public ResponseEntity<?> logout(@RequestBody(required = false) LogoutRequest request) {
		return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công!"));
	}
}
