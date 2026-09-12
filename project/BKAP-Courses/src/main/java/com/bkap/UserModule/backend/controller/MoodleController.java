package com.bkap.UserModule.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.bkap.CoursesModule.backend.service.ICourseService;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.UserModule.backend.service.IMoodleService;
import com.bkap.UserModule.backend.service.IUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/moodle")
@CrossOrigin(origins = "*")
public class MoodleController {

	@Autowired
	private IMoodleService moodleService;

	@Autowired
	private IUserService userService;

	@Autowired
	private ICourseService courseService;

	/** Địa chỉ gốc của Moodle, dùng để dựng link tới trang khóa học */
	@Value("${moodle.site-url}")
	private String moodleSiteUrl;

	@Value("${moodle.base-url}")
	private String moodleWsUrl; // https://lms.buni.vn/webservice/rest/server.php

	@Value("${moodle.token}")
	private String moodleToken; // giá trị thật nằm trong secrets.properties, không ghi vào mã nguồn

	@Value("${moodle.ws-format}")
	private String wsFormat; // json

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	// ─────────────────────────────────────────────
	// ENDPOINT 1: LẤY URL AUTO-LOGIN (SSO)
	// ─────────────────────────────────────────────

	// @PostMapping("/autologin-url")
	// public ResponseEntity<?> getAutoLoginUrl(@RequestBody Map<String, String>
	// body) {
	// try {
	// String username = body.get("username");
	//
	// if (username == null || username.isEmpty()) {
	// return ResponseEntity.status(400).body("Thiếu username!");
	// }
	//
	// // Tìm user trong DB để lấy password
	// User user = userService.findByUsername(username);
	// if (user == null) {
	// return ResponseEntity.status(404).body("Không tìm thấy user: " + username);
	// }
	//
	// // Gọi service — trả về URL đầy đủ hoặc null nếu lỗi
	// String loginUrl = moodleService.getMoodleAutoLoginUrl(username,
	// user.getPassword());
	//
	// if (loginUrl != null) {
	// // loginUrl đã có dạng:
	// // https://lms.buni.vn/login/index.php?token=xxx&wantsurl=...
	// return ResponseEntity.ok(Map.of("loginUrl", loginUrl));
	// }
	//
	// // loginUrl null → Moodle đã log lỗi trong service, trả về 500 cho frontend
	// return ResponseEntity.status(500).body("Không thể tạo auto-login URL. Kiểm
	// tra: "
	// + "1) Username đã tồn tại trên Moodle chưa? " + "2) Tên service có đúng
	// không?");
	//
	// } catch (Exception e) {
	// System.err.println("[MoodleController] Lỗi autologin: " + e.getMessage());
	// return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
	// }
	// }

	// ═════════════════════════════════════════════════════════════════════════
	// BA ENDPOINT DƯỚI ĐÂY ĐÃ TẮT — LỖ HỔNG CHIẾM TÀI KHOẢN LMS
	//
	// Cả ba nằm dưới /api/v1/moodle/**, nhánh từng được khai permitAll trong
	// WebConfiguration, tức gọi được mà KHÔNG cần đăng nhập buni.
	//
	// /autologin-url tin luôn "username" trong thân request. Ai gửi
	// {"username": "<tên bất kỳ>"} là nhận về link đăng nhập một lần vào
	// thẳng tài khoản LMS của người đó: xem điểm, làm bài hộ, nộp bài thay.
	// Đã thử thật với tài khoản test — HTTP 200 kèm loginUrl.
	//
	// /enrol cho phép ai cũng ghi danh bất kỳ ai vào bất kỳ khóa nào trên
	// Moodle, vì nhận thẳng mã khóa bên Moodle từ client.
	//
	// /logintoken chưa bao giờ được frontend gọi.
	//
	// Thay thế: POST /api/v1/learn/lms-url (LearnController + LmsSsoService).
	// Danh tính lấy từ JWT, không cần mật khẩu, và chỉ vào được khóa/hoạt động
	// đã được đối chiếu.
	// ═════════════════════════════════════════════════════════════════════════
	// @PostMapping("/autologin-url")
	// public ResponseEntity<?> getMoodleAutoLoginUrl(@RequestBody Map<String, String> body) {
	// try {
	// String username = body.get("username");
	// String password = body.get("password");
	// String email = body.get("email");
	// String fullname = body.get("fullname");

	// if (username == null || username.trim().isEmpty()) {
	// return ResponseEntity.badRequest().body(Map.of("error", "Thiếu username"));
	// }

	// username = username.trim().toLowerCase();
	// if (email == null || email.trim().isEmpty()) {
	// email = username + "@buni.vn";
	// }

	// // Parse firstname / lastname từ fullname
	// String firstname = username, lastname = "Student";
	// if (fullname != null && !fullname.trim().isEmpty()) {
	// fullname = fullname.trim();
	// int idx = fullname.lastIndexOf(" ");
	// if (idx != -1) {
	// firstname = fullname.substring(idx + 1);
	// lastname = fullname.substring(0, idx);
	// } else {
	// firstname = fullname;
	// }
	// }

	// // BƯỚC 1: Kiểm tra user đã tồn tại trên Moodle chưa
	// String checkUrl = moodleWsUrl + "?wstoken=" + moodleToken + "&wsfunction=core_user_get_users_by_field"
	// + "&moodlewsrestformat=json" + "&field=username&values[0]=" + username;

	// String checkResponse = restTemplate.getForObject(checkUrl, String.class);
	// JsonNode ketQuaTraCuu = objectMapper.readTree(checkResponse);

	// // Moodle trả về hai dạng hoàn toàn khác nhau ở cùng một endpoint:
	// //   thành công -> MẢNG người dùng, ví dụ []  hoặc  [{...}]
	// //   thất bại   -> OBJECT lỗi {"exception":..., "errorcode":..., "message":...}
	// //
	// // Bản trước chỉ kiểm tra size() == 0. Object lỗi có 3 trường nên size()
	// // bằng 3, khiến MỌI lỗi từ Moodle đều bị hiểu nhầm thành "đã tìm thấy
	// // người dùng" — bỏ qua bước tạo tài khoản rồi đi tiếp như không có gì.
	// // Vì vậy phải hỏi isArray() trước khi tin vào size().
	// if (ketQuaTraCuu != null && ketQuaTraCuu.has("exception")) {
	// String errorcode = ketQuaTraCuu.path("errorcode").asText("");
	// System.err.println("[MoodleController] Moodle từ chối tra cứu (" + errorcode + "): "
	// + ketQuaTraCuu.path("message").asText(""));
	// return ResponseEntity.status(502).body(Map.of("error",
	// "Chưa kết nối được hệ thống LMS. Vui lòng liên hệ quản trị viên.", "hint",
	// "Moodle trả về mã lỗi '" + errorcode + "'. Kiểm tra moodle.token còn hiệu lực không, "
	// + "và tài khoản gắn với token có đủ quyền chưa."));
	// }

	// boolean daCoTaiKhoan = ketQuaTraCuu != null && ketQuaTraCuu.isArray() && ketQuaTraCuu.size() > 0;
	// Integer moodleUserId = daCoTaiKhoan ? ketQuaTraCuu.get(0).path("id").asInt() : null;

	// // BƯỚC 2: Tạo user Moodle nếu chưa có
	// if (!daCoTaiKhoan) {
	// com.bkap.UserModule.dto.MoodleUserRequest dto = new com.bkap.UserModule.dto.MoodleUserRequest();
	// dto.setUsername(username);
	// dto.setPassword(password);
	// dto.setEmail(email);
	// dto.setFirstname(firstname);
	// dto.setLastname(lastname);

	// // Lấy luôn id từ phản hồi tạo mới. Tra cứu lại ngay lúc này đôi khi
	// // chưa thấy tài khoản, mà không có id thì không ghi danh được.
	// String taoMoi = moodleService.createMoodleUser(dto);
	// if (taoMoi != null) {
	// JsonNode nodeTao = objectMapper.readTree(taoMoi);
	// if (nodeTao.isArray() && nodeTao.size() > 0) {
	// moodleUserId = nodeTao.get(0).path("id").asInt();
	// }
	// }
	// System.out.println("[MoodleController] Đã gọi tạo user Moodle: " + username);
	// } else {
	// System.out.println("[MoodleController] User đã có sẵn trên Moodle: " + username);
	// }

	// // BƯỚC 3: Ghi danh vào khóa học tương ứng bên LMS.
	// //
	// // Frontend gửi lên ID khóa học BÊN BUNI, không phải bên Moodle. Backend tự
	// // tra ra moodleCourseId — làm vậy để không ai sửa request rồi tự ghi danh
	// // mình vào một khóa bất kỳ trên Moodle.
	// Integer moodleCourseId = null;
	// String courseIdStr = body.get("courseId");
	// if (courseIdStr != null && !courseIdStr.isBlank()) {
	// Course course = courseService.getCourseById(Short.parseShort(courseIdStr.trim()));
	// if (course != null) {
	// moodleCourseId = course.getMoodleCourseId();
	// }
	// }

	// boolean ghiDanhThanhCong = false;
	// if (moodleCourseId != null && moodleUserId != null) {
	// ghiDanhThanhCong = moodleService.enrolUserToCourse(moodleCourseId, moodleUserId) != null;
	// } else if (moodleCourseId != null) {
	// // Có khóa để ghi danh nhưng không biết id người dùng — vẫn cho vào LMS,
	// // chỉ là họ sẽ không thấy khóa đó. Ghi log để còn lần ra.
	// System.err.println("[MoodleController] Không xác định được Moodle user id của " + username
	// + ", bỏ qua bước ghi danh.");
	// }

	// // BƯỚC 4: Lấy login URL SSO qua auth_userkey
	// String loginUrl = moodleService.getMoodleAutoLoginUrl(username, password);

	// // Đẩy thẳng vào trang khóa học thay vì thả ở trang chủ Moodle.
	// //
	// // Chỉ làm khi ghi danh thành công. Ghi danh hỏng mà vẫn đẩy tới khóa thì
	// // người học gặp màn hình "Bạn không thể tự ghi danh vào khóa học này" —
	// // khó hiểu hơn là để họ ở trang chủ Moodle.
	// if (loginUrl != null && moodleCourseId != null && ghiDanhThanhCong) {
	// String dichDen = moodleSiteUrl + "/course/view.php?id=" + moodleCourseId;
	// loginUrl += "&wantsurl=" + URLEncoder.encode(dichDen, StandardCharsets.UTF_8);
	// }

	// if (loginUrl != null) {
	// return ResponseEntity.ok(Map.of("loginUrl", loginUrl));
	// }

	// // Không đổ lỗi cho một nguyên nhân cụ thể: getMoodleAutoLoginUrl trả null
	// // cho nhiều trường hợp khác nhau (token sai, plugin chưa bật, hàm không
	// // nằm trong service...). Lý do thật đã được MoodleService in ra log.
	// return ResponseEntity.status(500).body(Map.of("error",
	// "Chưa kết nối được hệ thống LMS. Vui lòng liên hệ quản trị viên.", "hint",
	// "Xem log server: thường do moodle.token không hợp lệ, plugin auth_userkey chưa bật, "
	// + "hoặc auth_userkey_request_login_url chưa nằm trong service của token."));

	// } catch (Exception e) {
	// e.printStackTrace();
	// return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
	// }
	// }

	// // ─────────────────────────────────────────────
	// // ENDPOINT 2: GHI DANH VÀO KHÓA HỌC
	// // ─────────────────────────────────────────────
	// @PostMapping("/enrol")
	// public ResponseEntity<?> enrolToCourse(@RequestBody Map<String, Integer> body) {
	// try {
	// Integer moodleUserId = body.get("moodleUserId");
	// Integer courseId = body.get("courseId");

	// if (moodleUserId == null || courseId == null) {
	// return ResponseEntity.status(400).body("Thiếu moodleUserId hoặc courseId!");
	// }

	// String result = moodleService.enrolUserToCourse(courseId, moodleUserId);
	// return ResponseEntity.ok(Map.of("message", "Ghi danh thành công!", "moodleResponse",
	// result != null ? result : "null (thành công)"));

	// } catch (Exception e) {
	// System.err.println("[MoodleController] Lỗi enrol: " + e.getMessage());
	// return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
	// }
	// }

	// @GetMapping("/logintoken")
	// public ResponseEntity<?> getMoodleLoginToken() {
	// try {
	// // GET trang login để lấy logintoken từ HTML
	// RestTemplate restTemplate = new RestTemplate();
	// String html = restTemplate.getForObject("https://lms.buni.vn/login/index.php", String.class);

	// if (html == null) {
	// return ResponseEntity.status(500).body(Map.of("error", "Không lấy được trang login Moodle"));
	// }

	// // Parse logintoken bằng regex
	// java.util.regex.Pattern pattern = java.util.regex.Pattern
	// .compile("name=\"logintoken\"\\s+value=\"([^\"]+)\"");
	// java.util.regex.Matcher matcher = pattern.matcher(html);

	// if (matcher.find()) {
	// String logintoken = matcher.group(1);
	// System.out.println("[MoodleController] logintoken: " + logintoken);
	// return ResponseEntity.ok(Map.of("logintoken", logintoken));
	// }

	// return ResponseEntity.status(500).body(Map.of("error", "Không tìm thấy logintoken trong HTML"));

	// } catch (Exception e) {
	// System.err.println("[MoodleController] Lỗi lấy logintoken: " + e.getMessage());
	// return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
	// }
	// }

	// ─────────────────────────────────────────────────────────────────────────
	// CÁCH SSO THỨ BA — ĐÃ TẮT, GIỮ LẠI ĐỂ THAM KHẢO
	//
	// Ý tưởng: xin MoodleSession rồi tự set cookie cho trình duyệt, sau đó
	// redirect thẳng vào Dashboard Moodle. Không dùng được, vì hai lý do:
	//
	// 1. Nó đọc kết quả của getMoodleAutoLoginUrl() như JSON rồi lấy trường
	// "moodleSession", trong khi hàm đó trả về một chuỗi URL thuần
	// → readTree() ném lỗi ngay dòng đầu.
	//
	// 2. Kể cả sửa được chỗ đó thì vẫn vô ích: trình duyệt không cho phép
	// server ở localhost:8080 đặt cookie cho domain lms.buni.vn.
	// Cookie cross-domain kiểu này luôn bị chặn.
	//
	// Frontend chưa bao giờ gọi endpoint này. Cách đúng là auth_userkey ở
	// endpoint /autologin-url phía trên.
	// ─────────────────────────────────────────────────────────────────────────

	// @GetMapping("/sso-redirect")
	// public void ssoRedirect(@RequestParam String username, @RequestParam String
	// password,
	// jakarta.servlet.http.HttpServletResponse response) throws Exception {
	//
	// // BƯỚC 1: Lấy MoodleSession từ service
	// String result = moodleService.getMoodleAutoLoginUrl(username, password);
	// if (result == null) {
	// response.sendRedirect("https://lms.buni.vn/login/index.php");
	// return;
	// }
	//
	// com.fasterxml.jackson.databind.JsonNode json = new
	// com.fasterxml.jackson.databind.ObjectMapper()
	// .readTree(result);
	//
	// String moodleSession = json.get("moodleSession").asText();
	// // Tách "MoodleSession=value" thành name và value
	// String[] parts = moodleSession.split("=", 2);
	// String cookieName = parts[0];
	// String cookieValue = parts[1];
	//
	// // BƯỚC 2: Set cookie vào response rồi redirect về Dashboard
	// jakarta.servlet.http.Cookie cookie = new
	// jakarta.servlet.http.Cookie(cookieName, cookieValue);
	// cookie.setDomain("lms.buni.vn");
	// cookie.setPath("/");
	// cookie.setSecure(true);
	// cookie.setMaxAge(7200); // 2 giờ
	// response.addCookie(cookie);
	//
	// // BƯỚC 3: Redirect thẳng về Dashboard Moodle
	// response.sendRedirect("https://lms.buni.vn/my/");
	// }
}