package com.bkap.CoursesModule.backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.service.LmsSsoService;
import com.bkap.CoursesModule.backend.service.LmsThongBaoService;
import com.bkap.config.JwtUtil;

/**
 * Chuông thông báo trên buni — /api/v1/notifications.
 *
 * Lấy đúng chuông thông báo của LMS: giáo viên đăng thông báo, bài kiểm tra sắp
 * đóng, có người trả lời diễn đàn... Mọi lời gọi dùng TOKEN CỦA HỌC VIÊN, nên
 * Moodle tự giới hạn chỉ trả thông báo của chính người đang đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class ThongBaoController {

	@Autowired
	private LmsThongBaoService lmsThongBaoService;

	@Autowired
	private LmsSsoService lmsSsoService;

	@Autowired
	private JwtUtil jwtUtil;

	/** Danh sách thông báo mới nhất kèm số chưa đọc. */
	@GetMapping
	public ResponseEntity<?> danhSach(@RequestParam(defaultValue = "10") int soLuong,
			@RequestHeader(value = "Authorization", required = false) String auth, Authentication authentication) {
		return thucHien(auth, authentication,
				(tokenHv, userId) -> ResponseEntity.ok(lmsThongBaoService.danhSach(tokenHv, userId, soLuong)));
	}

	/** Các việc sắp đến hạn (bài kiểm tra sắp đóng, bài tập sắp hết hạn...). */
	@GetMapping("/due")
	public ResponseEntity<?> sapDenHan(@RequestParam(defaultValue = "5") int soLuong,
			@RequestHeader(value = "Authorization", required = false) String auth, Authentication authentication) {
		return thucHien(auth, authentication,
				(tokenHv, userId) -> ResponseEntity.ok(Map.of("viec", lmsThongBaoService.sapDenHan(tokenHv, soLuong))));
	}

	/**
	 * Bấm vào một thông báo: đánh dấu đã đọc, và nếu Moodle có kèm đường dẫn thì
	 * trả về đường dẫn đăng nhập một lần để mở đúng chỗ đó bên LMS.
	 */
	@PostMapping("/{id}/open")
	public ResponseEntity<?> moThongBao(@PathVariable int id,
			@RequestParam(required = false) String duongDanLms,
			@RequestHeader(value = "Authorization", required = false) String auth, Authentication authentication) {
		return thucHien(auth, authentication, (tokenHv, userId) -> {
			lmsThongBaoService.danhDauDaDoc(tokenHv, id);

			String loginUrl = null;
			if (duongDanLms != null && !duongDanLms.isBlank()) {
				try {
					loginUrl = lmsSsoService.duongDanTrenLms(authentication.getName(), duongDanLms);
				} catch (Exception e) {
					// Link lạ hoặc LMS từ chối: vẫn coi là đã đọc, chỉ không mở được.
					System.err.println("[ThongBao] Không mở được đường dẫn: " + e.getMessage());
				}
			}
			Map<String, Object> ket = new java.util.LinkedHashMap<>();
			ket.put("chuaDoc", lmsThongBaoService.demChuaDoc(tokenHv, userId));
			ket.put("loginUrl", loginUrl);
			return ResponseEntity.ok(ket);
		});
	}

	// ─────────────────────────────────────────────────────────────

	private interface ViecCanToken {
		ResponseEntity<?> lam(String tokenHocVien, int moodleUserId) throws Exception;
	}

	private ResponseEntity<?> thucHien(String auth, Authentication authentication, ViecCanToken viec) {
		String jwt = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
		String tokenHv = jwt == null ? null : jwtUtil.docTokenLms(jwt);
		if (tokenHv == null) {
			// Phiên cũ không mang token LMS. Trả 403 kèm mã riêng thay vì 401: 401 sẽ
			// khiến frontend tự làm mới phiên, mà vé làm mới cũ cũng không có token.
			return ResponseEntity.status(403)
					.body(Map.of("error", "Vui lòng đăng xuất rồi đăng nhập lại để xem thông báo.", "ma",
							"CAN_DANG_NHAP_LAI"));
		}
		try {
			Integer userId = lmsSsoService.idTaiKhoanLms(authentication.getName());
			if (userId == null) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy tài khoản của bạn trên LMS"));
			}
			return viec.lam(tokenHv, userId);
		} catch (Exception e) {
			System.err.println("[ThongBao] " + authentication.getName() + ": " + e.getMessage());
			return ResponseEntity.status(502).body(Map.of("error", "Chưa lấy được thông báo từ hệ thống LMS."));
		}
	}
}
