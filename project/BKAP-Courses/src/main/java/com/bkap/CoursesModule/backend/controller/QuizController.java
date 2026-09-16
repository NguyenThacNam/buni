package com.bkap.CoursesModule.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.service.LmsQuizService;
import com.bkap.CoursesModule.backend.service.LmsQuizService.LoiLms;
import com.bkap.config.JwtUtil;

/**
 * Làm bài kiểm tra ngay trên buni — /api/v1/learn/quiz.
 *
 * Nằm dưới /api/v1/** nên bắt buộc đăng nhập. Mọi lời gọi sang Moodle dùng token
 * CỦA HỌC VIÊN lấy từ vé phiên (xem JwtUtil.generateToken), nên Moodle tự kiểm:
 * học viên có được ghi danh không, lượt làm có đúng của họ không, còn giờ không.
 */
@RestController
@RequestMapping("/api/v1/learn/quiz")
public class QuizController {

	@Autowired
	private LmsQuizService lmsQuizService;

	@Autowired
	private JwtUtil jwtUtil;

	/** Thông tin bài: cài đặt, lượt đã làm, điểm cao nhất, lượt đang dở. */
	@GetMapping("/{cmid}")
	public ResponseEntity<?> thongTin(@PathVariable int cmid, @RequestParam int courseId,
			@RequestHeader("Authorization") String auth) {
		return thucHien(auth, tk -> {
			Map<String, Object> bai = lmsQuizService.thongTinBai(tk, courseId, cmid);
			return bai == null ? ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy bài kiểm tra"))
					: ResponseEntity.ok(bai);
		});
	}

	/** Bắt đầu lượt mới, hoặc trả lại lượt đang dở. */
	@PostMapping("/{cmid}/start")
	public ResponseEntity<?> batDau(@PathVariable int cmid, @RequestParam int courseId,
			@RequestHeader("Authorization") String auth) {
		return thucHien(auth, tk -> ResponseEntity.ok(Map.of("attemptId", lmsQuizService.batDau(tk, courseId, cmid))));
	}

	/** Câu hỏi của một lượt đang làm. */
	@GetMapping("/attempt/{attemptId}")
	public ResponseEntity<?> cauHoi(@PathVariable int attemptId, @RequestHeader("Authorization") String auth) {
		return thucHien(auth, tk -> ResponseEntity.ok(lmsQuizService.cauHoi(tk, attemptId)));
	}

	/** Lưu nháp. Body: {"data":[{"name":"q15:1_answer","value":"2"}, ...]}. Trả sequencecheck mới theo slot. */
	@PutMapping("/attempt/{attemptId}")
	public ResponseEntity<?> luuNhap(@PathVariable int attemptId, @RequestBody Map<String, List<Map<String, String>>> body,
			@RequestHeader("Authorization") String auth) {
		return thucHien(auth, tk -> {
			return ResponseEntity.ok(Map.of("sequencecheck", lmsQuizService.luuNhap(tk, attemptId, body.get("data"))));
		});
	}

	/** Nộp bài. Body giống lưu nháp, thêm "hetGio": true khi tự nộp vì đồng hồ về 0. */
	@PostMapping("/attempt/{attemptId}/submit")
	public ResponseEntity<?> nopBai(@PathVariable int attemptId, @RequestBody Map<String, Object> body,
			@RequestHeader("Authorization") String auth) {
		@SuppressWarnings("unchecked")
		List<Map<String, String>> duLieu = (List<Map<String, String>>) body.get("data");
		boolean hetGio = Boolean.TRUE.equals(body.get("hetGio"));
		return thucHien(auth,
				tk -> ResponseEntity.ok(Map.of("trangThai", lmsQuizService.nopBai(tk, attemptId, duLieu, hetGio))));
	}

	/** Kết quả một lượt đã nộp. */
	@GetMapping("/attempt/{attemptId}/review")
	public ResponseEntity<?> ketQua(@PathVariable int attemptId, @RequestHeader("Authorization") String auth) {
		return thucHien(auth, tk -> ResponseEntity.ok(lmsQuizService.ketQua(tk, attemptId)));
	}

	// ─────────────────────────────────────────────────────────────

	private interface ViecCanToken {
		ResponseEntity<?> lam(String tokenHocVien);
	}

	private ResponseEntity<?> thucHien(String auth, ViecCanToken viec) {
		String jwt = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
		String tokenHv = jwt == null ? null : jwtUtil.docTokenLms(jwt);
		if (tokenHv == null) {
			// Phiên đăng nhập từ trước khi có tính năng này không mang token LMS.
			// Trả 403 kèm mã riêng thay vì 401: 401 sẽ khiến frontend tự làm mới
			// phiên, mà vé làm mới cũ cũng không có token LMS — lặp vô ích.
			return ResponseEntity.status(403).body(Map.of("error",
					"Vui lòng đăng xuất rồi đăng nhập lại để làm bài kiểm tra.", "ma", "CAN_DANG_NHAP_LAI"));
		}
		try {
			return viec.lam(tokenHv);
		} catch (LoiLms e) {
			return ResponseEntity.status(409).body(Map.of("error", e.getMessage(), "ma", e.maLoi));
		}
	}
}
