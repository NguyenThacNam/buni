package com.bkap.UserModule.backend.controller;

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
import com.bkap.config.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cấp access token mới khi cái cũ hết hạn (15 phút).
 *
 * Trước đây refresh token là chuỗi ngẫu nhiên lưu trong bảng refresh_token, xoay
 * vòng mỗi lần dùng và thu hồi được khi đăng xuất. Bỏ CSDL thì không còn chỗ ghi
 * "vé này đã hủy", nên nó thành JWT tự chứng minh.
 *
 * Hệ quả phải nói thẳng: KHÔNG thu hồi được nữa. Vé lọt ra ngoài thì dùng được
 * tới khi hết hạn, kể cả sau khi đổi mật khẩu. Bù lại bằng cách để hạn ngắn
 * (app.jwt.refresh-token-days) và hỏi lại LMS mỗi lần làm mới — tài khoản bị
 * khóa hoặc bị xóa bên LMS là hết đường vào ngay.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin("*")
public class AuthTokenController {

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private MoodleAuthService moodleAuthService;

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
		String het = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";

		String veCu = body == null ? null : body.get("refreshToken");
		String username = jwtUtil.docRefreshToken(veCu);
		if (username == null) {
			return ResponseEntity.status(401).body(Map.of("error", het));
		}

		// Hỏi lại LMS thay vì tin vào vé cũ: tài khoản có thể đã bị khóa, bị xóa,
		// hoặc vừa được cấp/gỡ quyền quản trị kể từ lúc đăng nhập.
		JsonNode nguoiDung;
		try {
			nguoiDung = moodleAuthService.timTheoTenDangNhap(username);
		} catch (Exception e) {
			System.err.println("[Refresh] Không hỏi được LMS: " + e.getMessage());
			return ResponseEntity.status(502)
					.body(Map.of("error", "Chưa kết nối được hệ thống LMS. Vui lòng thử lại sau."));
		}
		if (nguoiDung == null || nguoiDung.path("suspended").asBoolean(false)) {
			return ResponseEntity.status(401).body(Map.of("error", het));
		}

		// Vai trò đi theo vé làm mới — xem ghi chú ở JwtUtil.docVaiTroTuRefreshToken
		// về hạn chế của cách này.
		String vaiTro = jwtUtil.docVaiTroTuRefreshToken(veCu);

		Map<String, Object> phanHoi = new HashMap<>();
		// Chuyển nguyên phần token LMS (vẫn mã hóa) sang vé mới, để làm bài kiểm
		// tra không bị gián đoạn khi access token 15 phút hết hạn giữa chừng.
		String tokenLms = jwtUtil.docTokenLmsMaHoa(veCu);
		phanHoi.put("token", jwtUtil.generateToken(username, vaiTro, tokenLms));
		phanHoi.put("refreshToken", jwtUtil.taoRefreshToken(username, "ADMIN".equals(vaiTro), tokenLms));
		return ResponseEntity.ok(phanHoi);
	}

}
