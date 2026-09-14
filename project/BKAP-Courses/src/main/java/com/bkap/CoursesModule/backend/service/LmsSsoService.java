package com.bkap.CoursesModule.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bkap.UserModule.backend.service.IMoodleService;
import com.bkap.config.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Đưa người học từ trang học buni sang đúng một hoạt động bên LMS, đã đăng nhập
 * sẵn bằng chính tài khoản của họ; và trả lời câu "người này được học khóa nào".
 *
 * Thay cho /moodle/autologin-url cũ (đã gỡ vì mở cửa không cần đăng nhập — ai
 * khai tên đăng nhập của người khác là nhận được link vào thẳng tài khoản LMS
 * của họ). Khác bản cũ ở hai điểm:
 *
 * 1. Danh tính lấy từ JWT trong SecurityContext, KHÔNG nhận username từ thân
 * request. Trình duyệt không tự khai được mình là ai.
 *
 * 2. Không cần mật khẩu. auth_userkey chỉ cần username cộng token dịch vụ.
 */
@Service
public class LmsSsoService {

	/** Các loại hoạt động cho phép mở, để cmid không bị lái sang trang quản trị. */
	private static final Set<String> LOAI_HOP_LE = Set.of("quiz", "forum", "assign", "resource", "page", "url", "book",
			"lesson", "feedback", "choice", "workshop", "scorm", "glossary", "wiki", "folder");

	@Autowired
	private IMoodleService moodleService;

	@Autowired
	private LmsContentService lmsContentService;

	@Autowired
	private MoodleConfig moodleConfig;

	@Value("${moodle.site-url}")
	private String moodleSiteUrl;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	// ─────────────────────────────────────────────────────────────
	// ĐƯỜNG SANG LMS
	// ─────────────────────────────────────────────────────────────

	/**
	 * Đường dẫn đăng nhập một lần, dẫn thẳng tới hoạt động cần mở.
	 *
	 * @param username tên đăng nhập lấy từ token, không phải từ client
	 * @param loai     quiz, forum, assign...
	 * @throws IllegalStateException khi hoạt động không thuộc khóa, hoặc LMS từ chối
	 */
	public String duongDanVaoHoatDong(String username, int moodleCourseId, int cmid, String loai) throws Exception {
		if (loai == null || !LOAI_HOP_LE.contains(loai)) {
			throw new IllegalStateException("Loại hoạt động không hợp lệ");
		}

		// Chốt chặn quan trọng: cmid phải nằm trong đúng khóa mà người này đang
		// học. Thiếu bước này thì ai cũng có thể sửa request, mượn cơ chế đăng
		// nhập một lần của buni để chui vào hoạt động của khóa khác.
		if (!lmsContentService.cmidThuocKhoa(moodleCourseId, cmid)) {
			throw new IllegalStateException("Hoạt động này không thuộc khóa học");
		}

		return veDangNhap(username, moodleCourseId, moodleSiteUrl + "/mod/" + loai + "/view.php?id=" + cmid);
	}

	/** Đường dẫn đăng nhập một lần, dẫn tới trang chính của khóa bên LMS. */
	public String duongDanVaoKhoa(String username, int moodleCourseId) throws Exception {
		return veDangNhap(username, moodleCourseId, moodleSiteUrl + "/course/view.php?id=" + moodleCourseId);
	}

	private String veDangNhap(String username, int moodleCourseId, String dichDen) throws Exception {
		Integer moodleUserId = timTaiKhoan(username);
		if (moodleUserId == null) {
			// Không xảy ra trong luồng bình thường: đăng nhập buni chính là đăng nhập
			// bằng tài khoản LMS, nên tới được đây thì tài khoản chắc chắn có.
			throw new IllegalStateException("Không tìm thấy tài khoản trên hệ thống LMS");
		}

		// Ghi danh lại cho chắc. Bình thường trung tâm đã ghi danh sẵn bên LMS; gọi
		// ở đây là lưới an toàn cho trường hợp sót, để người học không kẹt ở màn
		// hình "Bạn không thể tự ghi danh". Đã ghi danh rồi thì Moodle bỏ qua.
		ghiDanhBenLms(moodleUserId, moodleCourseId);

		String loginUrl = moodleService.getMoodleAutoLoginUrl(username, null);
		if (loginUrl == null) {
			throw new IllegalStateException("Chưa lấy được đường dẫn đăng nhập từ hệ thống LMS");
		}
		return loginUrl + "&wantsurl=" + URLEncoder.encode(dichDen, StandardCharsets.UTF_8);
	}

	// ─────────────────────────────────────────────────────────────
	// GHI DANH
	// ─────────────────────────────────────────────────────────────

	/** Ghi danh một người (đã biết id bên LMS) vào một khóa. */
	public void ghiDanhBenLms(int moodleUserId, int moodleCourseId) throws Exception {
		if (moodleService.enrolUserToCourse(moodleCourseId, moodleUserId) == null) {
			throw new IllegalStateException("Hệ thống LMS từ chối ghi danh — xem log máy chủ để biết lý do");
		}
	}

	/**
	 * Các khóa người này đang được ghi danh bên LMS.
	 *
	 * Đây là thứ thay cho bảng enrollment cũ của buni: ai được học khóa nào do LMS
	 * trả lời, nên cấp quyền một chỗ là có hiệu lực cả hai bên.
	 *
	 * Trả về tập rỗng nếu người này chưa có tài khoản bên LMS.
	 */
	public Set<Integer> khoaDangHoc(String username) throws Exception {
		Integer moodleUserId = timTaiKhoan(username);
		if (moodleUserId == null) {
			return Set.of();
		}
		JsonNode r = objectMapper.readTree(restTemplate.getForObject(moodleConfig.getBaseUrl() + "?wstoken="
				+ moodleConfig.getToken() + "&wsfunction=core_enrol_get_users_courses&moodlewsrestformat=json"
				+ "&userid=" + moodleUserId, String.class));
		if (!r.isArray()) {
			throw new IllegalStateException("Hệ thống LMS từ chối tra cứu khóa học của tài khoản ("
					+ r.path("errorcode").asText("") + ")");
		}
		Set<Integer> ids = new LinkedHashSet<>();
		for (JsonNode c : r) {
			ids.add(c.path("id").asInt());
		}
		return ids;
	}

	/** Người này có được học khóa đó không. */
	public boolean daGhiDanh(String username, int moodleCourseId) throws Exception {
		return khoaDangHoc(username).contains(moodleCourseId);
	}

	// ─────────────────────────────────────────────────────────────
	// TÀI KHOẢN
	// ─────────────────────────────────────────────────────────────

	/** Id Moodle của tên đăng nhập này, hoặc null nếu bên đó chưa có tài khoản. */
	public Integer idTaiKhoanLms(String tenDangNhap) throws Exception {
		return timTaiKhoan(tenDangNhap);
	}

	private Integer timTaiKhoan(String tenDangNhap) throws Exception {
		String username = tenDangNhap.trim().toLowerCase();

		String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken()
				+ "&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json&field=username&values[0]="
				+ URLEncoder.encode(username, StandardCharsets.UTF_8);

		JsonNode traCuu = objectMapper.readTree(restTemplate.getForObject(url, String.class));

		// Moodle trả về hai dạng khác hẳn nhau ở cùng endpoint: thành công là MẢNG,
		// thất bại là OBJECT lỗi. Phải hỏi isArray() trước khi tin vào size().
		if (traCuu.has("exception")) {
			throw new IllegalStateException(
					"Hệ thống LMS từ chối tra cứu tài khoản (" + traCuu.path("errorcode").asText("") + ")");
		}
		return traCuu.isArray() && traCuu.size() > 0 ? traCuu.get(0).path("id").asInt() : null;
	}
}
