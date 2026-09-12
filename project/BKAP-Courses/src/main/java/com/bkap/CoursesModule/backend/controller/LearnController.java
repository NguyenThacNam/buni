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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.service.ICourseService;
import com.bkap.CoursesModule.backend.service.IEnrollmentService;
import com.bkap.CoursesModule.backend.service.LmsContentService;
import com.bkap.CoursesModule.backend.service.LmsSsoService;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.CoursesModule.entity.Enrollment;
import com.bkap.config.JwtUtil;
import com.bkap.config.MoodleConfig;

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
	private ICourseService courseService;

	@Autowired
	private LmsContentService lmsContentService;

	@Autowired
	private LmsSsoService lmsSsoService;

	@Autowired
	private IEnrollmentService enrollmentService;

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
	 * Học viên: phải có lượt ghi danh do admin cấp. Tài khoản chỉ là tấm vé vào
	 * cổng, còn khóa nào mở cho ai là do trung tâm quyết.
	 *
	 * Admin: vào được mọi khóa, để kiểm tra nội dung trước khi cấp cho học viên.
	 *
	 * Không cần chặn riêng /learn/file: đường dẫn file chỉ được ký và phát ra ở
	 * endpoint nội dung khóa bên dưới, nơi đã kiểm tra quyền rồi.
	 */
	private boolean coQuyenHoc(Authentication authentication, Course course) {
		if (authentication == null || authentication.getName() == null) {
			return false;
		}
		boolean laAdmin = authentication.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		return laAdmin || enrollmentService.daGhiDanh(authentication.getName(), course.getId());
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

		List<Map<String, Object>> ds = new ArrayList<>();
		for (Enrollment e : enrollmentService.findByUsername(authentication.getName())) {
			Course c = e.getCourse();
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("courseId", c.getId());
			m.put("title", c.getTitle());
			m.put("slug", c.getSlug());
			m.put("thumbnailUrl", c.getThumbnailUrl());
			m.put("category", c.getCategory() == null ? null : c.getCategory().getName());
			m.put("status", e.getStatus());
			m.put("statusLabel", e.getStatusLabel());
			m.put("progressPercent", e.getProgressPercent());
			m.put("enrolledAt", e.getEnrolledAt() == null ? null : e.getEnrolledAt().toString());
			m.put("lastStudiedAt", e.getLastStudiedAt() == null ? null : e.getLastStudiedAt().toString());
			m.put("coNoiDung", c.getMoodleCourseId() != null);
			ds.add(m);
		}
		return ResponseEntity.ok(ds);
	}

	// ─────────────────────────────────────────────────────────────
	// NỘI DUNG KHÓA HỌC
	// ─────────────────────────────────────────────────────────────

	@GetMapping("/{courseId}")
	public ResponseEntity<?> layNoiDung(@PathVariable Short courseId, Authentication authentication) {
		Course course = courseService.getCourseById(courseId);
		if (course == null) {
			return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy khóa học"));
		}
		if (!coQuyenHoc(authentication, course)) {
			return ResponseEntity.status(403).body(Map.of("error", CHUA_GHI_DANH));
		}

		try {
			Map<String, Object> noiDung = lmsContentService.layNoiDung(course);

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
		// trang chính của khóa (nút "Đăng ký để làm bài kiểm tra").
		Course course;
		Integer cmid = null;
		try {
			course = courseService.getCourseById(Short.parseShort(body.getOrDefault("courseId", "").trim()));
			String cmidChuoi = body.get("cmid");
			if (cmidChuoi != null && !cmidChuoi.isBlank()) {
				cmid = Integer.parseInt(cmidChuoi.trim());
			}
		} catch (NumberFormatException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Mã khóa học hoặc mã hoạt động không hợp lệ"));
		}

		if (course == null) {
			return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy khóa học"));
		}
		if (!coQuyenHoc(authentication, course)) {
			return ResponseEntity.status(403).body(Map.of("error", CHUA_GHI_DANH));
		}

		try {
			String loginUrl = cmid == null ? lmsSsoService.duongDanVaoKhoa(authentication.getName(), course)
					: lmsSsoService.duongDanVaoHoatDong(authentication.getName(), course, cmid, body.get("loai"));
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
