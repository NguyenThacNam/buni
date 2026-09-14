package com.bkap.UserModule.backend.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.UserModule.backend.service.MoodleAuthService;
import com.bkap.UserModule.backend.service.MoodleAuthService.NguoiDungLms;
import com.bkap.UserModule.dto.LoginRequest;
import com.bkap.config.JwtUtil;

/**
 * Đăng nhập bằng tài khoản LMS.
 *
 * Trước đây đối chiếu mật khẩu băng BCrypt với bảng users của buni. Giờ hỏi
 * thẳng Moodle: một nơi giữ tài khoản, một nơi giữ mật khẩu, buni chỉ cấp vé đi
 * lại trong phạm vi của mình.
 *
 * Hình dạng phản hồi giữ nguyên như cũ (user, token, refreshToken) để frontend
 * không phải sửa.
 */
@RestController
@RequestMapping("api/v1/login")
@CrossOrigin("*")
public class LoginController {

	@Autowired
	private MoodleAuthService moodleAuthService;

	@Autowired
	private JwtUtil jwtUtil;

	@PostMapping()
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
		NguoiDungLms nguoiDung;
		try {
			nguoiDung = moodleAuthService.dangNhap(request.getUsername(), request.getPassword());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			System.err.println("[Login] Không hỏi được LMS: " + e.getMessage());
			return ResponseEntity.status(502)
					.body(Map.of("error", "Chưa kết nối được hệ thống LMS. Vui lòng thử lại sau."));
		}

		// Vai trò lấy từ LMS: ai là quản trị bên đó thì là quản trị ở đây.
		String vaiTro = nguoiDung.laAdmin() ? "ADMIN" : "STUDENT";

		Map<String, Object> thongTin = new HashMap<>();
		thongTin.put("name", nguoiDung.hoTen());
		thongTin.put("email", nguoiDung.email());
		thongTin.put("username", nguoiDung.username());
		thongTin.put("last_login", doiNgayGio(nguoiDung.lanTruyCapCuoi()));
		thongTin.put("createDate", doiNgay(nguoiDung.lanTruyCapDau()));
		thongTin.put("status", true);

		Map<String, Object> phanHoi = new HashMap<>();
		phanHoi.put("user", thongTin);
		phanHoi.put("token", jwtUtil.generateToken(nguoiDung.username(), vaiTro));
		phanHoi.put("refreshToken", jwtUtil.taoRefreshToken(nguoiDung.username(), nguoiDung.laAdmin()));
		return ResponseEntity.ok(phanHoi);
	}

	/** Moodle trả thời gian dạng số giây; 0 nghĩa là chưa từng. */
	private String doiNgayGio(long giay) {
		return giay <= 0 ? "Chưa có lần đăng nhập trước"
				: new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(giay * 1000));
	}

	private String doiNgay(long giay) {
		return giay <= 0 ? "" : new SimpleDateFormat("dd-MM-yyyy").format(new Date(giay * 1000));
	}
}
