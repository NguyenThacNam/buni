package com.bkap.UserModule.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bkap.config.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Xác thực người dùng bằng chính tài khoản LMS.
 *
 * buni không giữ mật khẩu nữa. Học viên gõ mật khẩu vào buni, backend hỏi Moodle
 * "mật khẩu này đúng không" qua login/token.php: lấy được token nghĩa là đúng,
 * nhận invalidlogin nghĩa là sai. Nhờ vậy đổi mật khẩu bên LMS là buni nhận
 * ngay, và không còn hai nơi cùng giữ danh sách người dùng.
 *
 * Dịch vụ dùng để đăng nhập (BuniLogin) cố ý KHÔNG có hàm nào ngoài
 * core_webservice_get_site_info — token cấp ra chỉ đọc được thông tin của chính
 * chủ nó, không gọi được gì khác.
 */
@Service
public class MoodleAuthService {

	/** Thông tin người dùng lấy từ LMS. */
	public record NguoiDungLms(int id, String username, String hoTen, String email, boolean laAdmin, long lanTruyCapDau,
			long lanTruyCapCuoi) {
	}

	@Autowired
	private MoodleConfig moodleConfig;

	@Value("${moodle.site-url}")
	private String moodleSiteUrl;

	/** Tên tắt của dịch vụ chỉ dùng để kiểm tra mật khẩu. */
	@Value("${moodle.login-service:buni_login}")
	private String dichVuDangNhap;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	// ─────────────────────────────────────────────────────────────
	// ĐĂNG NHẬP
	// ─────────────────────────────────────────────────────────────

	/**
	 * Kiểm tra mật khẩu rồi trả về thông tin người dùng.
	 *
	 * @throws IllegalArgumentException khi sai tài khoản hoặc mật khẩu
	 * @throws IllegalStateException    khi không hỏi được LMS
	 */
	public NguoiDungLms dangNhap(String username, String matKhau) throws Exception {
		if (username == null || username.isBlank() || matKhau == null || matKhau.isEmpty()) {
			throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng");
		}

		String than = "username=" + ma(username.trim().toLowerCase()) + "&password=" + ma(matKhau) + "&service="
				+ ma(dichVuDangNhap);

		// KHÔNG ghi log thân request: nó chứa mật khẩu gốc.
		JsonNode r = objectMapper.readTree(guiForm(moodleSiteUrl + "/login/token.php", than));

		if (r.has("error")) {
			String ma = r.path("errorcode").asText("");
			if ("invalidlogin".equals(ma)) {
				// Không nói rõ sai tên hay sai mật khẩu — nói rõ là giúp người dò
				// biết tài khoản nào có thật.
				throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng");
			}
			// Mấy mã còn lại là cấu hình bên LMS hỏng, không phải lỗi người dùng.
			System.err.println("[MoodleAuth] login/token.php trả lỗi " + ma + ": " + r.path("error").asText(""));
			throw new IllegalStateException("Chưa kết nối được hệ thống LMS. Vui lòng liên hệ quản trị viên.");
		}

		String tokenNguoiDung = r.path("token").asText("");
		if (tokenNguoiDung.isBlank()) {
			throw new IllegalStateException("Hệ thống LMS không cấp được phiên đăng nhập.");
		}
		return thongTinTuTokenNguoiDung(tokenNguoiDung, username.trim().toLowerCase());
	}

	/**
	 * Danh tính và quyền, hỏi bằng token của CHÍNH người vừa đăng nhập.
	 *
	 * Cờ userissiteadmin là cách buni biết ai là quản trị — vai trò cũng do LMS
	 * giữ, không còn cột role trong CSDL buni.
	 */
	private NguoiDungLms thongTinTuTokenNguoiDung(String tokenNguoiDung, String username) throws Exception {
		JsonNode s = objectMapper.readTree(guiForm(moodleConfig.getBaseUrl(),
				"wstoken=" + ma(tokenNguoiDung) + "&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json"));

		if (s.has("exception")) {
			System.err.println("[MoodleAuth] get_site_info bị từ chối (" + s.path("errorcode").asText("")
					+ "). Kiểm tra hàm này đã được thêm vào dịch vụ " + dichVuDangNhap + " chưa.");
			throw new IllegalStateException("Chưa lấy được thông tin tài khoản từ hệ thống LMS.");
		}

		// get_site_info không trả email, nên hỏi thêm bằng token quản trị.
		String email = "";
		long dau = 0, cuoi = 0;
		JsonNode chiTiet = timTheoTenDangNhap(username);
		if (chiTiet != null) {
			email = chiTiet.path("email").asText("");
			dau = chiTiet.path("firstaccess").asLong(0);
			cuoi = chiTiet.path("lastaccess").asLong(0);
		}

		return new NguoiDungLms(s.path("userid").asInt(), username, s.path("fullname").asText(username), email,
				s.path("userissiteadmin").asBoolean(false), dau, cuoi);
	}

	/** Hồ sơ người dùng, tra bằng token quản trị. Null nếu bên LMS không có. */
	public JsonNode timTheoTenDangNhap(String username) throws Exception {
		JsonNode r = objectMapper.readTree(restTemplate.getForObject(moodleConfig.getBaseUrl() + "?wstoken="
				+ moodleConfig.getToken() + "&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json"
				+ "&field=username&values[0]=" + ma(username.trim().toLowerCase()), String.class));
		if (r.has("exception")) {
			throw new IllegalStateException(
					"Hệ thống LMS từ chối tra cứu tài khoản (" + r.path("errorcode").asText("") + ")");
		}
		return r.isArray() && r.size() > 0 ? r.get(0) : null;
	}

	/** Tài khoản đang dùng email này, hoặc null. Moodle bắt email không trùng. */
	public JsonNode timTheoEmail(String email) throws Exception {
		JsonNode r = objectMapper.readTree(restTemplate.getForObject(moodleConfig.getBaseUrl() + "?wstoken="
				+ moodleConfig.getToken() + "&wsfunction=core_user_get_users_by_field&moodlewsrestformat=json"
				+ "&field=email&values[0]=" + ma(email.trim().toLowerCase()), String.class));
		if (r.has("exception")) {
			throw new IllegalStateException(
					"Hệ thống LMS từ chối tra cứu email (" + r.path("errorcode").asText("") + ")");
		}
		return r.isArray() && r.size() > 0 ? r.get(0) : null;
	}

	// ─────────────────────────────────────────────────────────────
	// ĐỔI MẬT KHẨU
	// ─────────────────────────────────────────────────────────────

	/**
	 * Đổi mật khẩu của chính người dùng.
	 *
	 * Vẫn bắt nhập đúng mật khẩu hiện tại — kiểm bằng cách thử đăng nhập. Không
	 * có bước này thì ai nhặt được máy đang mở sẵn là đổi được, chiếm hẳn tài khoản.
	 *
	 * @throws IllegalArgumentException kèm câu báo lỗi cho người dùng
	 */
	public void doiMatKhau(String username, String matKhauCu, String matKhauMoi) throws Exception {
		if (matKhauMoi == null || matKhauMoi.trim().length() < 6) {
			throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
		}
		if (matKhauMoi.trim().equals(matKhauCu)) {
			throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
		}

		NguoiDungLms nguoiDung;
		try {
			nguoiDung = dangNhap(username, matKhauCu);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
		}

		JsonNode r = objectMapper.readTree(guiForm(moodleConfig.getBaseUrl(),
				"wstoken=" + ma(moodleConfig.getToken()) + "&wsfunction=core_user_update_users"
						+ "&moodlewsrestformat=json&users[0][id]=" + nguoiDung.id() + "&users[0][password]="
						+ ma(matKhauMoi.trim())));

		if (r.has("exception")) {
			String maLoi = r.path("errorcode").asText("");
			System.err.println("[MoodleAuth] Đổi mật khẩu thất bại (" + maLoi + "): " + r.path("message").asText(""));
			// Moodle từ chối khi mật khẩu không đạt chính sách của site.
			throw new IllegalArgumentException(
					"Hệ thống LMS không nhận mật khẩu này. Hãy thử mật khẩu dài hơn, có cả chữ và số.");
		}
	}

	/**
	 * Cập nhật hồ sơ bên LMS. Moodle tách họ và tên thành hai trường, nên phải
	 * cắt chuỗi họ tên ra.
	 *
	 * Ngày sinh thì Moodle không có sẵn trường nào — bỏ qua, thay vì lưu vào một
	 * chỗ riêng của buni rồi lại lệch dữ liệu.
	 */
	public void capNhatHoSo(String username, String hoTen, String email, String dienThoai) throws Exception {
		JsonNode nguoiDung = timTheoTenDangNhap(username);
		if (nguoiDung == null) {
			throw new IllegalArgumentException("Không tìm thấy tài khoản trên hệ thống LMS");
		}

		StringBuilder than = new StringBuilder("wstoken=" + ma(moodleConfig.getToken())
				+ "&wsfunction=core_user_update_users&moodlewsrestformat=json&users[0][id]="
				+ nguoiDung.path("id").asInt());

		if (hoTen != null && !hoTen.isBlank()) {
			String sach = hoTen.trim();
			int viTri = sach.lastIndexOf(' ');
			String ten = viTri > 0 ? sach.substring(viTri + 1) : sach;
			String ho = viTri > 0 ? sach.substring(0, viTri) : "";
			than.append("&users[0][firstname]=").append(ma(ten));
			if (!ho.isEmpty()) {
				than.append("&users[0][lastname]=").append(ma(ho));
			}
		}
		if (email != null && !email.isBlank()) {
			than.append("&users[0][email]=").append(ma(email.trim().toLowerCase()));
		}
		if (dienThoai != null && !dienThoai.isBlank()) {
			than.append("&users[0][phone1]=").append(ma(dienThoai.trim()));
		}

		JsonNode r = objectMapper.readTree(guiForm(moodleConfig.getBaseUrl(), than.toString()));
		if (r.has("exception")) {
			System.err.println("[MoodleAuth] Cập nhật hồ sơ thất bại (" + r.path("errorcode").asText("") + "): "
					+ r.path("message").asText(""));
			throw new IllegalArgumentException("Hệ thống LMS không nhận thông tin này. Kiểm tra lại email.");
		}
	}

	// ─────────────────────────────────────────────────────────────
	// TIỆN ÍCH
	// ─────────────────────────────────────────────────────────────

	/**
	 * Gửi bằng POST với thân form.
	 *
	 * Mật khẩu và token không được nằm trên URL: chúng sẽ hiện trong log truy cập
	 * của máy chủ web và trong lịch sử của mọi proxy trên đường đi.
	 */
	private String guiForm(String url, String than) {
		org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
		return restTemplate.postForObject(url, new org.springframework.http.HttpEntity<>(than, headers), String.class);
	}

	private String ma(String s) {
		return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
	}
}
