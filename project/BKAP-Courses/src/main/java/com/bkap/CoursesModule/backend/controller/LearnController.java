package com.bkap.CoursesModule.backend.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.service.LmsCatalogService;
import com.bkap.CoursesModule.backend.service.LmsTienDoService;
import com.bkap.CoursesModule.backend.service.LmsTienDoService.TrangThaiMuc;
import com.bkap.CoursesModule.backend.service.LmsContentService;
import com.bkap.CoursesModule.backend.service.LmsDiemDanhService;
import com.bkap.CoursesModule.backend.service.LmsSsoService;
import com.bkap.config.JwtUtil;
import com.bkap.config.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Trang học trên buni: lấy nội dung khóa từ LMS và phục vụ file.
 *
 * Đặt dưới /api/v1/learn chứ không phải /api/v1/courses là có chủ ý — nhánh
 * courses được khai permitAll cho khách xem danh mục, còn nội dung học thì phải
 * đăng nhập mới xem được.
 */
@RestController
@RequestMapping("/api/v1/learn")
public class LearnController {

	@Autowired
	private LmsCatalogService lmsCatalogService;

	@Autowired
	private LmsContentService lmsContentService;

	@Autowired
	private LmsSsoService lmsSsoService;

	@Autowired
	private LmsTienDoService lmsTienDoService;

	@Autowired
	private LmsDiemDanhService lmsDiemDanhService;


	@Autowired
	private MoodleConfig moodleConfig;

	@Autowired
	private JwtUtil jwtUtil;

	@Value("${moodle.site-url}")
	private String moodleSiteUrl;

	/**
	 * Bắt mọi đường dẫn file Moodle nằm trong HTML của bài học.
	 *
	 * Giáo viên chèn ảnh hay tải video thẳng vào trình soạn thảo của Moodle thì
	 * Moodle sinh ra link pluginfile.php — trình duyệt gọi thẳng link đó sẽ bị từ
	 * chối vì không có token dịch vụ. Phải thay bằng đường dẫn đã ký của buni.
	 */
	private static final Pattern LINK_FILE_TRONG_HTML = Pattern
			.compile("https?://[^ \"'<>]*?/pluginfile[.]php(/[^ \"'<>?]*)([?][^ \"'<>]*)?");

	/** Dùng lại một client cho mọi request; tạo mới mỗi lần sẽ rò kết nối. */
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
			.followRedirects(HttpClient.Redirect.NORMAL).build();

	// ─────────────────────────────────────────────────────────────
	// QUYỀN HỌC
	// ─────────────────────────────────────────────────────────────

	private static final String CHUA_GHI_DANH = "Bạn chưa được ghi danh vào khóa học này. "
			+ "Vui lòng liên hệ trung tâm để được cấp quyền học.";

	/**
	 * Người đang đăng nhập có được vào học khóa này không.
	 *
	 * Học viên: phải được ghi danh BÊN LMS. Trước đây hỏi bảng enrollment của
	 * buni; giờ hỏi thẳng Moodle, để cấp quyền một chỗ là có hiệu lực cả hai bên.
	 *
	 * Admin: vào được mọi khóa, để kiểm tra nội dung trước khi cấp cho học viên.
	 *
	 * Không cần chặn riêng /learn/file: đường dẫn file chỉ được ký và phát ra ở
	 * endpoint nội dung khóa bên dưới, nơi đã kiểm tra quyền rồi.
	 */
	private boolean coQuyenHoc(Authentication authentication, int moodleCourseId) throws Exception {
		if (authentication == null || authentication.getName() == null) {
			return false;
		}
		boolean laAdmin = authentication.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		return laAdmin || lmsSsoService.daGhiDanh(authentication.getName(), moodleCourseId);
	}

	/**
	 * Danh sách khóa người đang đăng nhập được học — cho trang "Khóa học của tôi"
	 * và để trang giới thiệu khóa biết nên hiện nút "Vào học" hay "Liên hệ".
	 */
	@GetMapping("/my-courses")
	public ResponseEntity<?> khoaCuaToi(Authentication authentication) {
		if (authentication == null || authentication.getName() == null) {
			return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
		}

		try {
			// Danh sách khóa lấy từ ghi danh bên LMS, rồi ghép với dữ liệu khóa học
			// (tên, ảnh bìa, danh mục) đã có sẵn trong bộ đệm.
			java.util.Set<Integer> idDangHoc = lmsSsoService.khoaDangHoc(authentication.getName());
			Integer idNguoiHoc = lmsSsoService.idTaiKhoanLms(authentication.getName());

			List<Map<String, Object>> ds = new ArrayList<>();
			for (Map<String, Object> khoa : lmsCatalogService.danhSachKhoa()) {
				if (!idDangHoc.contains(khoa.get("id"))) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> danhMuc = (Map<String, Object>) khoa.get("category");

				Map<String, Object> m = new LinkedHashMap<>();
				m.put("courseId", khoa.get("id"));
				m.put("title", khoa.get("title"));
				m.put("slug", khoa.get("slug"));
				m.put("thumbnailUrl", khoa.get("thumbnailUrl"));
				m.put("category", danhMuc == null ? null : danhMuc.get("name"));

				// Tiến độ do Moodle tự ghi nhận (học viên mở tài liệu, nộp bài...).
				// Khóa nào chưa bật theo dõi hoàn thành thì trả null và giao diện ẩn
				// thanh tiến độ — thà không hiện còn hơn hiện 0% cho người đã học xong.
				//
				// Mỗi khóa tốn một lời gọi sang Moodle. Vài khóa thì không sao; lớp nào
				// học chục khóa mà thấy chậm thì phải đệm lại theo từng người.
				Integer tienDo = idNguoiHoc == null ? null
						: lmsTienDoService.phanTramHoanThanh((Integer) khoa.get("id"), idNguoiHoc);
				boolean xong = tienDo != null && tienDo >= 100;

				m.put("status", xong ? "COMPLETED" : "IN_PROGRESS");
				m.put("statusLabel", xong ? "Hoàn thành" : "Đang học");
				m.put("progressPercent", tienDo);
				m.put("enrolledAt", null);
				m.put("coNoiDung", true);
				ds.add(m);
			}
			return ResponseEntity.ok(ds);

		} catch (Exception e) {
			System.err.println("[Learn] Không lấy được khóa của " + authentication.getName() + ": " + e.getMessage());
			return ResponseEntity.status(502)
					.body(Map.of("error", "Chưa lấy được danh sách khóa học từ hệ thống LMS."));
		}
	}

	// ─────────────────────────────────────────────────────────────
	// NỘI DUNG KHÓA HỌC
	// ─────────────────────────────────────────────────────────────

	/** courseId là id khóa học bên LMS. */
	@GetMapping("/{courseId}")
	public ResponseEntity<?> layNoiDung(@PathVariable Integer courseId,
			@RequestHeader(value = "Authorization", required = false) String auth, Authentication authentication) {
		// Nội dung phải hỏi bằng token LMS của chính người học để Moodle áp hạn chế
		// truy cập. Phiên cũ không mang token thì bắt đăng nhập lại, KHÔNG lùi về
		// token dịch vụ — lùi về là lộ luôn các bài đang bị khóa.
		String jwt = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
		String tokenHv = jwt == null ? null : jwtUtil.docTokenLms(jwt);
		if (tokenHv == null) {
			return ResponseEntity.status(403).body(Map.of("error",
					"Phiên đăng nhập đã cũ. Vui lòng đăng nhập lại để vào học.", "ma", "CAN_DANG_NHAP_LAI"));
		}
		try {
			Map<String, Object> khoa = lmsCatalogService.chiTietKhoa(courseId);
			if (khoa == null) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy khóa học"));
			}
			if (!coQuyenHoc(authentication, courseId)) {
				return ResponseEntity.status(403).body(Map.of("error", CHUA_GHI_DANH));
			}

			Map<String, Object> noiDung = lmsContentService.layNoiDung(courseId, String.valueOf(khoa.get("title")),
					tokenHv);

			// Đánh dấu mục đã hoàn thành và điểm bài kiểm tra của chính người đang xem.
			Integer idNguoiHoc = lmsSsoService.idTaiKhoanLms(authentication.getName());
			if (idNguoiHoc != null) {
				Map<Integer, TrangThaiMuc> hoanThanh = lmsTienDoService.chiTietHoanThanh(courseId, idNguoiHoc);
				ganTienDo(noiDung, hoanThanh, lmsTienDoService.diemBaiKiemTra(courseId, idNguoiHoc));

				// Đếm trên toàn khóa, kể cả bài nằm trong chương đang khóa (học viên
				// không thấy danh sách bài đó, nhưng vẫn phải tính vào tổng).
				if (!hoanThanh.isEmpty()) {
					long xong = hoanThanh.values().stream().filter(TrangThaiMuc::xong).count();
					noiDung.put("tienDo", Map.of("xong", xong, "tong", hoanThanh.size()));
				}
			}

			// Ký sẵn đường dẫn cho từng file, để frontend nhúng thẳng vào thẻ
			// iframe/video mà không cần gửi kèm token đăng nhập.
			kySanDuongDanFile(noiDung);

			return ResponseEntity.ok(noiDung);

		} catch (IllegalStateException e) {
			return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			System.err.println("[Learn] Lỗi lấy nội dung khóa " + courseId + ": " + e.getMessage());
			return ResponseEntity.status(502).body(Map.of("error",
					"Chưa lấy được nội dung từ hệ thống LMS. Vui lòng thử lại hoặc liên hệ quản trị viên."));
		}
	}

	/** Gắn cờ hoàn thành và điểm vào từng mục trong cấu trúc khóa học. */
	@SuppressWarnings("unchecked")
	private void ganTienDo(Map<String, Object> noiDung, Map<Integer, TrangThaiMuc> hoanThanh,
			Map<Integer, String> diem) {
		for (Map<String, Object> chuong : (List<Map<String, Object>>) noiDung.get("sections")) {
			for (Map<String, Object> muc : (List<Map<String, Object>>) chuong.get("modules")) {
				Object cmid = muc.get("cmid");
				if (cmid == null) {
					continue;
				}
				TrangThaiMuc t = hoanThanh.get(cmid);
				if (t != null) {
					muc.put("daHoanThanh", t.xong());
					muc.put("tuDanhDau", t.tuDanhDau());
					muc.put("canXem", t.canXem());
					muc.put("daXem", t.daXem());
				}
				if (diem.containsKey(cmid)) {
					muc.put("diem", diem.get(cmid));
				}
			}
		}
	}

	// ─────────────────────────────────────────────────────────────
	// ĐIỂM DANH
	// ─────────────────────────────────────────────────────────────

	/**
	 * Bảng điểm danh của CHÍNH người đang đăng nhập trong một hoạt động điểm danh.
	 *
	 * Người học chỉ gửi mã khóa và mã hoạt động; mã người học lấy từ phiên đăng
	 * nhập, không nhận từ trình duyệt — nếu không thì đổi một con số là xem được
	 * điểm danh của bạn cùng lớp.
	 */
	@GetMapping("/{courseId}/attendance/{cmid}")
	public ResponseEntity<?> diemDanh(@PathVariable int courseId, @PathVariable int cmid,
			Authentication authentication) {
		try {
			if (!coQuyenHoc(authentication, courseId)) {
				return ResponseEntity.status(403).body(Map.of("error", CHUA_GHI_DANH));
			}
			JsonNode hoatDong = lmsContentService.timHoatDong(courseId, cmid);
			if (hoatDong == null || !"attendance".equals(hoatDong.path("modname").asText())) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy mục điểm danh này trong khóa"));
			}
			Integer idNguoiHoc = lmsSsoService.idTaiKhoanLms(authentication.getName());
			if (idNguoiHoc == null) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy tài khoản của bạn trên LMS"));
			}
			return ResponseEntity.ok(lmsDiemDanhService.cuaHocVien(hoatDong.path("instance").asInt(), idNguoiHoc));
		} catch (IllegalStateException e) {
			System.err.println("[Learn] Điểm danh khóa " + courseId + ", cmid " + cmid + ": " + e.getMessage());
			return ResponseEntity.status(502)
					.body(Map.of("error", "Chưa lấy được dữ liệu điểm danh từ LMS. Vui lòng thử lại sau."));
		} catch (Exception e) {
			System.err.println("[Learn] Điểm danh khóa " + courseId + ": " + e.getMessage());
			return ResponseEntity.status(502)
					.body(Map.of("error", "Chưa lấy được dữ liệu điểm danh từ LMS. Vui lòng thử lại sau."));
		}
	}

	/**
	 * Học viên tự điểm danh một buổi. Mã học viên lấy từ phiên đăng nhập; ghi lên
	 * LMS bằng token của chính học viên.
	 */
	@PostMapping("/{courseId}/attendance/{cmid}/sessions/{sessionId}/mark")
	public ResponseEntity<?> tuDiemDanh(@PathVariable int courseId, @PathVariable int cmid,
			@PathVariable int sessionId, @RequestHeader(value = "Authorization", required = false) String auth,
			Authentication authentication) {
		String jwt = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
		String tokenHv = jwt == null ? null : jwtUtil.docTokenLms(jwt);
		if (tokenHv == null) {
			return ResponseEntity.status(403).body(Map.of("error",
					"Vui lòng đăng xuất rồi đăng nhập lại để điểm danh.", "ma", "CAN_DANG_NHAP_LAI"));
		}
		try {
			if (!coQuyenHoc(authentication, courseId)) {
				return ResponseEntity.status(403).body(Map.of("error", CHUA_GHI_DANH));
			}
			JsonNode hoatDong = lmsContentService.timHoatDong(courseId, cmid);
			if (hoatDong == null || !"attendance".equals(hoatDong.path("modname").asText())) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy mục điểm danh này trong khóa"));
			}
			Integer idNguoiHoc = lmsSsoService.idTaiKhoanLms(authentication.getName());
			if (idNguoiHoc == null) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy tài khoản của bạn trên LMS"));
			}
			return ResponseEntity.ok(lmsDiemDanhService.tuDiemDanh(tokenHv, hoatDong.path("instance").asInt(),
					sessionId, idNguoiHoc));
		} catch (LmsDiemDanhService.LoiDiemDanh e) {
			return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			System.err.println("[Learn] Tự điểm danh khóa " + courseId + ": " + e.getMessage());
			return ResponseEntity.status(502)
					.body(Map.of("error", "Chưa ghi được điểm danh. Vui lòng thử lại sau."));
		}
	}

	// ─────────────────────────────────────────────────────────────
	// GHI NHẬN HOÀN THÀNH
	// ─────────────────────────────────────────────────────────────

	/**
	 * Học viên vừa mở một mục trên buni — báo Moodle "đã xem" để tính hoàn thành.
	 *
	 * @return trạng thái hoàn thành mới của cả khóa, để giao diện cập nhật dấu tích
	 */
	@PostMapping("/{courseId}/modules/{cmid}/viewed")
	public ResponseEntity<?> daXem(@PathVariable int courseId, @PathVariable int cmid,
			@RequestHeader("Authorization") String auth, Authentication authentication) {
		return ghiNhan(courseId, cmid, auth, authentication, lmsTienDoService::baoDaXem);
	}

	/** Học viên tự đánh dấu đã học. Body: {"completed": true|false} */
	@PutMapping("/{courseId}/modules/{cmid}/completion")
	public ResponseEntity<?> danhDau(@PathVariable int courseId, @PathVariable int cmid,
			@RequestBody Map<String, Object> body, @RequestHeader("Authorization") String auth,
			Authentication authentication) {
		boolean daXong = Boolean.TRUE.equals(body.get("completed"));
		return ghiNhan(courseId, cmid, auth, authentication, (tk, hoatDong) -> {
			lmsTienDoService.danhDauThuCong(tk, cmid, daXong);
			return true;
		});
	}

	private interface ViecGhiNhan {
		boolean lam(String tokenHocVien, JsonNode hoatDong);
	}

	private ResponseEntity<?> ghiNhan(int courseId, int cmid, String auth, Authentication authentication,
			ViecGhiNhan viec) {
		String jwt = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
		String tokenHv = jwt == null ? null : jwtUtil.docTokenLms(jwt);
		if (tokenHv == null) {
			return ResponseEntity.status(403).body(Map.of("error",
					"Vui lòng đăng xuất rồi đăng nhập lại để lưu tiến độ học.", "ma", "CAN_DANG_NHAP_LAI"));
		}
		// Loại và instance tra từ chính khóa học, không nhận từ trình duyệt.
		JsonNode hoatDong = lmsContentService.timHoatDong(courseId, cmid);
		if (hoatDong == null) {
			return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy mục học này trong khóa"));
		}
		try {
			if (!viec.lam(tokenHv, hoatDong)) {
				return ResponseEntity.ok(Map.of("hoTro", false));
			}
		} catch (IllegalStateException e) {
			System.err.println("[Learn] Ghi nhận hoàn thành cmid " + cmid + ": " + e.getMessage());
			return ResponseEntity.status(409)
					.body(Map.of("error", "Hệ thống LMS chưa ghi nhận được. Vui lòng thử lại."));
		}

		Map<String, Object> ket = new LinkedHashMap<>();
		ket.put("hoTro", true);
		// Lấy lại trạng thái để giao diện cập nhật dấu tích. Hỏng thì thôi — việc
		// ghi nhận đã xong, lần tải trang sau sẽ thấy.
		try {
			Integer idNguoiHoc = lmsSsoService.idTaiKhoanLms(authentication.getName());
			if (idNguoiHoc != null) {
				Map<Integer, Map<String, Boolean>> ds = new LinkedHashMap<>();
				lmsTienDoService.chiTietHoanThanh(courseId, idNguoiHoc)
						.forEach((id, t) -> ds.put(id, Map.of("daHoanThanh", t.xong(), "daXem", t.daXem())));
				ket.put("hoanThanh", ds);
			}
		} catch (Exception e) {
			System.err.println("[Learn] Không lấy lại được tiến độ khóa " + courseId + ": " + e.getMessage());
		}
		return ResponseEntity.ok(ket);
	}

	@SuppressWarnings("unchecked")
	private void kySanDuongDanFile(Map<String, Object> noiDung) {
		for (Map<String, Object> chuong : (List<Map<String, Object>>) noiDung.get("sections")) {
			for (Map<String, Object> muc : (List<Map<String, Object>>) chuong.get("modules")) {
				// Bài đọc: thay link file trong nội dung HTML trước.
				Object html = muc.get("html");
				if (html != null) {
					muc.put("html", kyLinkTrongHtml(html.toString()));
				}

				Object duongDan = muc.get("filePath");
				if (duongDan == null) {
					continue;
				}
				String ky = jwtUtil.kyDuongDanFile(duongDan.toString());
				muc.put("fileUrl", "/api/v1/learn/file?t=" + ky);

				// Học liệu chỉ cho xem tại chỗ, không phát đường dẫn tải về nữa.
				// Tham số tai=1 của /learn/file vẫn còn, phòng khi sau này cần mở lại.
				// muc.put("downloadUrl", "/api/v1/learn/file?tai=1&t=" + ky);

				// Đường dẫn gốc không cần lộ ra ngoài nữa.
				muc.remove("filePath");
			}
		}
	}

	/**
	 * Đổi mọi link pluginfile.php trong HTML thành đường dẫn đã ký của buni.
	 *
	 * Nhờ vậy ảnh minh họa và video giáo viên tải thẳng vào bài đọc vẫn hiện
	 * được, mà token dịch vụ không lọt ra trình duyệt.
	 */
	private String kyLinkTrongHtml(String html) {
		if (html == null || html.isBlank()) {
			return html;
		}
		Matcher m = LINK_FILE_TRONG_HTML.matcher(html);
		StringBuilder ket = new StringBuilder();
		while (m.find()) {
			String thayThe = "/api/v1/learn/file?t=" + jwtUtil.kyDuongDanFile(m.group(1));
			m.appendReplacement(ket, Matcher.quoteReplacement(thayThe));
		}
		m.appendTail(ket);
		return ket.toString();
	}

	// ─────────────────────────────────────────────────────────────
	// MỞ HOẠT ĐỘNG BÊN LMS
	// ─────────────────────────────────────────────────────────────

	/**
	 * Trả về đường dẫn đăng nhập một lần, dẫn thẳng vào bài kiểm tra hay diễn đàn
	 * bên LMS bằng chính tài khoản đang đăng nhập ở buni.
	 *
	 * Trước đây trang học chỉ đưa link trần tới Moodle, nên ai chưa có phiên đăng
	 * nhập bên đó sẽ rơi vào màn hình đăng nhập; tệ hơn là nếu máy đang đăng nhập
	 * bằng tài khoản khác thì bài làm ghi sang tên người khác.
	 *
	 * Client chỉ gửi courseId và cmid. Tên đăng nhập lấy từ token, không nhận từ
	 * thân request — nếu không thì sửa request một chút là mở được phiên của
	 * người khác.
	 */
	@PostMapping("/lms-url")
	public ResponseEntity<?> moHoatDongLms(@RequestBody Map<String, String> body, Authentication authentication) {
		if (authentication == null || authentication.getName() == null) {
			return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
		}

		// cmid không bắt buộc: có thì mở thẳng hoạt động đó, không có thì vào
		// trang chính của khóa. courseId là id bên LMS.
		int courseId;
		Integer cmid = null;
		try {
			courseId = Integer.parseInt(body.getOrDefault("courseId", "").trim());
			String cmidChuoi = body.get("cmid");
			if (cmidChuoi != null && !cmidChuoi.isBlank()) {
				cmid = Integer.parseInt(cmidChuoi.trim());
			}
		} catch (NumberFormatException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Mã khóa học hoặc mã hoạt động không hợp lệ"));
		}

		try {
			if (lmsCatalogService.chiTietKhoa(courseId) == null) {
				return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy khóa học"));
			}
			if (!coQuyenHoc(authentication, courseId)) {
				return ResponseEntity.status(403).body(Map.of("error", CHUA_GHI_DANH));
			}

			String loginUrl = cmid == null ? lmsSsoService.duongDanVaoKhoa(authentication.getName(), courseId)
					: lmsSsoService.duongDanVaoHoatDong(authentication.getName(), courseId, cmid, body.get("loai"));
			return ResponseEntity.ok(Map.of("loginUrl", loginUrl));

		} catch (IllegalStateException e) {
			return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			System.err.println("[Learn] Lỗi mở hoạt động LMS: " + e.getMessage());
			return ResponseEntity.status(502).body(Map.of("error",
					"Chưa kết nối được hệ thống LMS. Vui lòng thử lại hoặc liên hệ quản trị viên."));
		}
	}

	// ─────────────────────────────────────────────────────────────
	// PHỤC VỤ FILE
	// ─────────────────────────────────────────────────────────────

	/**
	 * Lấy file từ Moodle rồi truyền thẳng về trình duyệt.
	 *
	 * Backend đứng giữa vì file trên Moodle đòi token dịch vụ, mà token đó mở được
	 * toàn bộ API — không thể để trình duyệt cầm.
	 *
	 * @param t   đường dẫn file đã ký, sinh từ endpoint nội dung khóa học
	 * @param tai 1 thì tải về máy, bỏ trống thì mở ngay trong trang
	 */
	@GetMapping("/file")
	public void layFile(@RequestParam("t") String t,
			@RequestParam(name = "tai", required = false) String tai,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		String duongDan = jwtUtil.docDuongDanFile(t);
		if (duongDan == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Đường dẫn không hợp lệ hoặc đã hết hạn");
			return;
		}

		// Chốt chặn: chuỗi đã ký nên không ai sửa được, nhưng vẫn kiểm tra lần nữa
		// phòng trường hợp chính backend sinh ra đường dẫn sai.
		if (!duongDan.startsWith("/") || duongDan.contains("..")) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Đường dẫn không hợp lệ");
			return;
		}

		String urlMoodle = moodleSiteUrl + "/webservice/pluginfile.php" + duongDan + "?token="
				+ URLEncoder.encode(moodleConfig.getToken(), StandardCharsets.UTF_8);

		HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(urlMoodle)).GET()
				.timeout(Duration.ofMinutes(10));

		// Chuyển tiếp header Range để trình duyệt tua được video.
		// Thiếu dòng này thì video vẫn chạy nhưng kéo thanh thời gian không nhảy,
		// vì trình duyệt không xin được đoạn giữa file.
		String range = request.getHeader("Range");
		if (range != null && !range.isBlank()) {
			req.header("Range", range);
		}

		HttpResponse<InputStream> res = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());

		// Moodle báo lỗi bằng thân JSON kèm mã 200, không phải mã 4xx. Cứ chuyển
		// tiếp nguyên xi thì trình duyệt tải về một file .pdf chứa mấy dòng JSON —
		// người dùng chỉ thấy "file hỏng" mà không hiểu vì sao. Chặn tại đây và
		// trả lỗi cho ra lỗi.
		String kieuNoiDung = res.headers().firstValue("content-type").orElse("");
		if (kieuNoiDung.contains("application/json")) {
			String than;
			try (InputStream in = res.body()) {
				than = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			System.err.println("[Learn] Moodle tu choi file " + duongDan + ": " + than);
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(
					"{\"error\":\"Hệ thống LMS từ chối tệp này. Kiểm tra mục "
							+ "'Có thể tải file xuống' trong cấu hình dịch vụ web.\"}");
			return;
		}

		response.setStatus(res.statusCode());
		chuyenTiepHeader(res, response, "content-type", "content-length", "content-range", "accept-ranges",
				"last-modified", "etag");

		// Moodle luôn gắn forcedownload nên tự đặt lại cho đúng ý người dùng bấm.
		String tenFile = duongDan.substring(duongDan.lastIndexOf('/') + 1);
		String kieu = "1".equals(tai) ? "attachment" : "inline";
		response.setHeader("Content-Disposition",
				kieu + "; filename*=UTF-8''" + URLEncoder.encode(tenFile, StandardCharsets.UTF_8));

		try (InputStream in = res.body(); OutputStream out = response.getOutputStream()) {
			in.transferTo(out);
		}
	}

	private void chuyenTiepHeader(HttpResponse<?> tu, HttpServletResponse den, String... ten) {
		for (String h : ten) {
			tu.headers().firstValue(h).ifPresent(v -> den.setHeader(h, v));
		}
	}
}
