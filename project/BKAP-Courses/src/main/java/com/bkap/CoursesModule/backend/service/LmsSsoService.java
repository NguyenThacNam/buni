package com.bkap.CoursesModule.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bkap.CoursesModule.entity.Course;
import com.bkap.UserModule.backend.service.IMoodleService;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.dto.MoodleUserRequest;
import com.bkap.UserModule.entity.User;
import com.bkap.config.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Đưa người học từ trang học buni sang đúng một hoạt động bên LMS, đã đăng nhập
 * sẵn bằng chính tài khoản của họ.
 *
 * Thay cho /moodle/autologin-url (đã gỡ vì mở cửa không cần đăng nhập — ai khai
 * tên đăng nhập của người khác là nhận được link vào thẳng tài khoản LMS của họ).
 * Khác bản cũ ở hai điểm quan trọng:
 *
 * 1. Danh tính lấy từ JWT trong SecurityContext, KHÔNG nhận username từ thân
 * request. Trình duyệt không tự khai được mình là ai.
 *
 * 2. Không cần mật khẩu. Bản cũ bắt frontend gửi mật khẩu gốc lên, nên phải giữ
 * nó trong sessionStorage suốt phiên làm việc — mở DevTools là đọc được. Thật
 * ra auth_userkey chỉ cần username cộng token dịch vụ; mật khẩu chỉ dùng lúc
 * TẠO tài khoản Moodle, và chỗ đó dùng chuỗi ngẫu nhiên là đủ.
 */
@Service
public class LmsSsoService {

	/** Các loại hoạt động cho phép mở, để cmid không bị lái sang trang quản trị. */
	private static final java.util.Set<String> LOAI_HOP_LE = java.util.Set.of("quiz", "forum", "assign", "resource",
			"page", "url", "book", "lesson", "feedback", "choice", "workshop", "scorm", "glossary", "wiki", "folder");

	@Autowired
	private IMoodleService moodleService;

	@Autowired
	private IUserService userService;

	@Autowired
	private LmsContentService lmsContentService;

	@Autowired
	private MoodleConfig moodleConfig;

	@Value("${moodle.site-url}")
	private String moodleSiteUrl;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SecureRandom nguauNhien = new SecureRandom();

	/**
	 * Dựng đường dẫn đăng nhập một lần, dẫn thẳng tới hoạt động cần mở.
	 *
	 * @param username tên đăng nhập lấy từ token, không phải từ client
	 * @param course   khóa học bên buni
	 * @param cmid     mã hoạt động bên Moodle
	 * @param loai     quiz, forum, assign...
	 * @throws IllegalStateException khi khóa chưa nối LMS, hoạt động không thuộc
	 *                               khóa, hoặc Moodle từ chối
	 */
	public String duongDanVaoHoatDong(String username, Course course, int cmid, String loai) throws Exception {
		if (course.getMoodleCourseId() == null) {
			throw new IllegalStateException("Khóa học này chưa được nối với hệ thống LMS");
		}
		if (loai == null || !LOAI_HOP_LE.contains(loai)) {
			throw new IllegalStateException("Loại hoạt động không hợp lệ");
		}

		// Chốt chặn quan trọng: cmid phải nằm trong đúng khóa mà người này đang
		// học. Thiếu bước này thì ai cũng có thể sửa request, mượn cơ chế đăng
		// nhập một lần của buni để chui vào hoạt động của khóa khác.
		if (!lmsContentService.cmidThuocKhoa(course.getMoodleCourseId(), cmid)) {
			throw new IllegalStateException("Hoạt động này không thuộc khóa học");
		}

		return veDangNhap(username, course, moodleSiteUrl + "/mod/" + loai + "/view.php?id=" + cmid);
	}

	/**
	 * Dựng đường dẫn đăng nhập một lần, dẫn tới trang chính của khóa bên LMS.
	 * Dùng cho nút "Đăng ký để làm bài kiểm tra" ở trang giới thiệu khóa học.
	 */
	public String duongDanVaoKhoa(String username, Course course) throws Exception {
		if (course.getMoodleCourseId() == null) {
			throw new IllegalStateException("Khóa học này chưa được nối với hệ thống LMS");
		}
		return veDangNhap(username, course, moodleSiteUrl + "/course/view.php?id=" + course.getMoodleCourseId());
	}

	/**
	 * Phần chung của hai lối vào: bảo đảm có tài khoản LMS, ghi danh vào khóa, rồi
	 * xin vé đăng nhập một lần kèm trang đích.
	 */
	private String veDangNhap(String username, Course course, String dichDen) throws Exception {
		User nguoiDung = userService.findByUsername(username);
		if (nguoiDung == null) {
			throw new IllegalStateException("Không tìm thấy tài khoản");
		}

		// Bình thường admin đã đẩy ghi danh sang LMS ngay lúc cấp khóa. Gọi lại ở
		// đây là lưới an toàn: lần đó LMS trục trặc thì giờ ghi danh bù, người học
		// không bị kẹt ở màn hình "Bạn không thể tự ghi danh". Đã ghi danh rồi thì
		// Moodle bỏ qua, không sinh bản trùng.
		ghiDanhBenLms(nguoiDung, course);

		String loginUrl = moodleService.getMoodleAutoLoginUrl(nguoiDung.getUsername(), null);
		if (loginUrl == null) {
			throw new IllegalStateException("Chưa lấy được đường dẫn đăng nhập từ hệ thống LMS");
		}

		return loginUrl + "&wantsurl=" + URLEncoder.encode(dichDen, StandardCharsets.UTF_8);
	}

	/**
	 * Bảo đảm người này có tài khoản LMS và đã ghi danh vào khóa tương ứng bên
	 * đó. Trang admin gọi hàm này ngay khi cấp khóa, để giáo viên bên LMS thấy
	 * đủ danh sách lớp từ ngày đầu.
	 *
	 * @throws IllegalStateException kèm lý do cụ thể khi không làm được, để admin
	 *                               biết mà xử lý
	 */
	public void ghiDanhBenLms(User nguoiDung, Course course) throws Exception {
		if (course.getMoodleCourseId() == null) {
			throw new IllegalStateException("Khóa học này chưa được nối với hệ thống LMS");
		}

		Integer moodleUserId = timHoacTaoTaiKhoan(nguoiDung);
		if (moodleUserId == null) {
			throw new IllegalStateException("Chưa tạo được tài khoản trên hệ thống LMS");
		}

		if (moodleService.enrolUserToCourse(course.getMoodleCourseId(), moodleUserId) == null) {
			throw new IllegalStateException("Hệ thống LMS từ chối ghi danh — xem log máy chủ để biết lý do");
		}
	}

	/**
	 * Gỡ ghi danh của người này khỏi khóa tương ứng bên LMS. Trang admin gọi khi
	 * xóa lượt ghi danh ở buni, để hai bên luôn khớp nhau.
	 *
	 * Chỉ gỡ lượt ghi danh: tài khoản LMS và bài làm cũ vẫn còn, cấp lại khóa là
	 * thấy lại điểm.
	 *
	 * @throws IllegalStateException kèm lý do khi LMS từ chối
	 */
	public void huyGhiDanhBenLms(User nguoiDung, Course course) throws Exception {
		if (course.getMoodleCourseId() == null) {
			return; // khóa chưa nối LMS thì bên đó vốn không có gì để gỡ
		}

		// Chỉ tra, KHÔNG tạo: người chưa từng có tài khoản LMS thì cũng không có
		// lượt ghi danh nào bên đó để gỡ.
		Integer moodleUserId = timTaiKhoan(nguoiDung.getUsername());
		if (moodleUserId == null) {
			return;
		}

		if (!moodleService.unenrolUserFromCourse(course.getMoodleCourseId(), moodleUserId)) {
			throw new IllegalStateException("Hệ thống LMS từ chối gỡ ghi danh. Kiểm tra hàm "
					+ "enrol_manual_unenrol_users đã được thêm vào dịch vụ web chưa");
		}
	}

	/** Id Moodle của tên đăng nhập này, hoặc null nếu bên đó chưa có tài khoản. */
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

	/** Trả về id Moodle của người dùng, tạo mới nếu bên đó chưa có. */
	private Integer timHoacTaoTaiKhoan(User nguoiDung) throws Exception {
		String username = nguoiDung.getUsername().trim().toLowerCase();

		Integer daCo = timTaiKhoan(username);
		if (daCo != null) {
			return daCo;
		}

		MoodleUserRequest dto = new MoodleUserRequest();
		dto.setUsername(username);
		dto.setEmail(nguoiDung.getEmail() != null && !nguoiDung.getEmail().isBlank() ? nguoiDung.getEmail()
				: username + "@buni.vn");
		dto.setPassword(matKhauNgauNhien());
		datHoTen(dto, nguoiDung.getFullname(), username);

		String taoMoi = moodleService.createMoodleUser(dto);
		if (taoMoi == null) {
			return null;
		}
		JsonNode node = objectMapper.readTree(taoMoi);
		return node.isArray() && node.size() > 0 ? node.get(0).path("id").asInt() : null;
	}

	/**
	 * Moodle bắt buộc có mật khẩu khi tạo tài khoản, nhưng người học không bao giờ
	 * gõ nó — mọi đường vào LMS đều đi qua đăng nhập một lần từ buni. Nên sinh
	 * chuỗi ngẫu nhiên rồi quên luôn, thay vì bắt buni cầm mật khẩu thật.
	 */
	private String matKhauNgauNhien() {
		byte[] bytes = new byte[24];
		nguauNhien.nextBytes(bytes);
		// Thêm ký tự đặc biệt và chữ hoa cho khớp chính sách mật khẩu mặc định
		// của Moodle, nếu không core_user_create_users sẽ từ chối.
		return "Bk1!" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Moodle tách họ và tên thành hai trường bắt buộc. */
	private void datHoTen(MoodleUserRequest dto, String fullname, String username) {
		String ten = username, ho = "Student";
		if (fullname != null && !fullname.isBlank()) {
			String sach = fullname.trim();
			int viTri = sach.lastIndexOf(' ');
			if (viTri > 0) {
				ten = sach.substring(viTri + 1);
				ho = sach.substring(0, viTri);
			} else {
				ten = sach;
			}
		}
		dto.setFirstname(ten);
		dto.setLastname(ho);
	}
}
