package com.bkap.UserModule.backend.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.bkap.UserModule.dto.MoodleUserRequest;
import com.bkap.config.MoodleConfig;

@Service
public class MoodleService implements IMoodleService {

	@Autowired
	private MoodleConfig moodleConfig;

	// RestTemplate dùng để bắn request HTTP ẩn sang Moodle
	private final RestTemplate restTemplate = new RestTemplate();

	// =========================================================
	// PHƯƠNG THỨC 1: TẠO TÀI KHOẢN MOODLE (BẢN CHUẨN HÓA URL)
	// =========================================================
	@Override
	public String createMoodleUser(MoodleUserRequest user) {
		try {
			if (user == null || user.getUsername() == null || user.getPassword() == null) {
				System.err.println("[MoodleService] Dữ liệu user bị null!");
				return null;
			}

			String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken()
					+ "&wsfunction=core_user_create_users" + "&moodlewsrestformat=" + moodleConfig.getWsFormat();

			String cleanUsername = user.getUsername().trim().toLowerCase();
			String cleanPassword = user.getPassword().trim();
			String cleanEmail = user.getEmail().trim().toLowerCase();

			// GIỮ dấu tiếng Việt. Bản trước bỏ dấu trước khi gửi, nên "Nguyễn Văn A"
			// vào Moodle thành "Nguyen Van A" — giáo viên điểm danh lại phải đoán tên.
			// Moodle nhận UTF-8 bình thường; các lời gọi khác của dự án vẫn gửi tiếng
			// Việt có dấu và không có vấn đề gì.
			String cleanFirstName = user.getFirstname() != null && !user.getFirstname().isBlank()
					? user.getFirstname().trim()
					: "Học viên";
			String cleanLastName = user.getLastname() != null && !user.getLastname().isBlank()
					? user.getLastname().trim()
					: "buni";

			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			formData.add("users[0][username]", cleanUsername);
			formData.add("users[0][password]", cleanPassword);
			formData.add("users[0][firstname]", cleanFirstName);
			formData.add("users[0][lastname]", cleanLastName);
			formData.add("users[0][email]", cleanEmail);
			formData.add("users[0][auth]", "manual");
			if (user.getPhone() != null && !user.getPhone().isBlank()) {
				formData.add("users[0][phone1]", user.getPhone().trim());
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

			// KHÔNG in cả formData: nó chứa users[0][password] là mật khẩu thô của
			// người dùng. Log nằm trên đĩa máy chủ, in mật khẩu vào đó thì việc băm
			// BCrypt trong CSDL cũng thành vô nghĩa.
			System.out.println("[MoodleService] Tạo user Moodle: " + cleanUsername + " <" + cleanEmail + ">");
			String response = restTemplate.postForObject(url, requestEntity, String.class);
			System.out.println("[MoodleService] Moodle trả về: " + response);

			return response;

		} catch (Exception e) {
			System.err.println("[MoodleService] Lỗi: " + e.getMessage());
			return null;
		}
	}

	// ✅ Hàm bỏ dấu tiếng Việt — dùng java.text.Normalizer có sẵn, không cần thêm
	// thư viện
	private String removeAccents(String input) {
		if (input == null) {
			return "";
		}
		// NFD tách ký tự thành base + dấu, replaceAll bỏ phần dấu (\\p{M})
		return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				// Đ/đ không thuộc NFD nên phải xử lý riêng
				.replace("Đ", "D").replace("đ", "d");
	}

	// =========================================================
	// PHƯƠNG THỨC 2: GHI DANH VÀO KHÓA HỌC
	// =========================================================
	@Override
	public String enrolUserToCourse(int courseId, int moodleUserId) {
		try {
			String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken()
					+ "&wsfunction=enrol_manual_enrol_users" + "&moodlewsrestformat=" + moodleConfig.getWsFormat()
					+ "&enrolments[0][roleid]=5" + "&enrolments[0][userid]=" + moodleUserId
					+ "&enrolments[0][courseid]=" + courseId;

			// URL chứa wstoken nên không ghi cả đường dẫn ra log.

			String response = restTemplate.postForObject(url, null, String.class);

			// enrol_manual_enrol_users là hàm void: thành công thì Moodle trả về
			// chuỗi "null" hoặc rỗng. Có nội dung nghĩa là có chuyện.
			//
			// Bản trước trả thẳng response mà không ai đọc, nên khi Moodle từ chối
			// (thiếu quyền, khóa bị ẩn) thì chương trình vẫn coi như ghi danh xong,
			// vẫn đẩy người học sang khóa, để họ gặp màn hình "Bạn không thể tự ghi
			// danh vào khóa học này" mà không hiểu vì sao.
			if (response != null && response.contains("\"exception\"")) {
				com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper()
						.readTree(response);
				String errorcode = node.path("errorcode").asText("");

				// Ngoại lệ: "Message was not sent."
				//
				// Moodle ghi danh XONG rồi mới gửi thư thông báo cho người học. Máy chủ
				// lms.buni.vn chưa cấu hình SMTP nên bước gửi thư ném lỗi — nhưng việc
				// ghi danh đã lưu vào CSDL rồi. Coi đây là hỏng thì lại bỏ mất trang
				// khóa học của một lượt ghi danh thành công.
				//
				// Đây là cách chữa tạm, và nó dựa vào chuỗi thông báo nên dễ vỡ khi
				// Moodle đổi câu chữ. Cách chữa gốc nằm bên Moodle: cấu hình SMTP,
				// hoặc tắt gửi thông báo trong phương thức Ghi danh thủ công của khóa.
				if (errorcode.contains("Message was not sent")) {
					System.out.println("[MoodleService] Ghi danh thành công nhưng Moodle không gửi được thư thông báo"
							+ " (máy chủ LMS chưa cấu hình SMTP) — user " + moodleUserId + ", khóa " + courseId);
					return "";
				}

				System.err.println("[MoodleService] GHI DANH THẤT BẠI (" + errorcode + "): "
						+ node.path("message").asText("") + " — user " + moodleUserId + ", khóa " + courseId);
				return null;
			}

			System.out.println("[MoodleService] Ghi danh thành công: user " + moodleUserId + " vào khóa " + courseId);
			return response == null ? "" : response;

		} catch (Exception e) {
			System.err.println("[MoodleService] Lỗi ghi danh: " + e.getMessage());
			return null;
		}
	}

	
	//PHƯƠNG THỨC 3: LẤY URL AUTO-LOGIN (SSO)
	
	// @Override
	// public String getMoodleAutoLoginUrl(String username, String password) {
	// try {
	// String tokenUrl = "https://lms.buni.vn/login/token.php" + "?username=" +
	// username.trim().toLowerCase()
	// + "&password=" + password.trim() + "&service=bkap_courses"; // ← đổi thành
	// short name đúng sau khi
	// // hỏi thầy
	
	// System.out.println("[MoodleService] Gọi token URL: " + tokenUrl);
	
	// String response = restTemplate.getForObject(tokenUrl, String.class);
	// System.out.println("[MoodleService] Moodle trả về: " + response);
	
	// if (response == null) {
	// return null;
	// }
	
	// // ✅ Parse JSON đúng cách bằng Jackson
	// com.fasterxml.jackson.databind.ObjectMapper mapper = new
	// com.fasterxml.jackson.databind.ObjectMapper();
	// com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(response);
	
	// // Nếu có token → thành công
	// if (node.has("token")) {
	// String token = node.get("token").asText();
	// // Trả về URL auto-login + redirect thẳng vào Dashboard
	// return "https://lms.buni.vn/login/index.php" + "?token=" + token +
	// "&wantsurl=https://lms.buni.vn/my/";
	// }
	
	// // Nếu có lỗi → log ra và trả null
	// if (node.has("error")) {
	// System.err.println("[MoodleService] Moodle lỗi: " +
	// node.get("error").asText() + " | errorcode: "
	// + node.get("errorcode").asText());
	// }
	
	// return null;
	
	// } catch (Exception e) {
	// System.err.println("[MoodleService] Lỗi getMoodleAutoLoginUrl: " +
	// e.getMessage());
	// return null;
	// }
	// }

	@Override
	public boolean unenrolUserFromCourse(int courseId, int moodleUserId) {
		try {
			String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken()
					+ "&wsfunction=enrol_manual_unenrol_users" + "&moodlewsrestformat=" + moodleConfig.getWsFormat()
					+ "&enrolments[0][userid]=" + moodleUserId + "&enrolments[0][courseid]=" + courseId;

			// URL chứa wstoken nên không ghi cả đường dẫn ra log.
			String response = restTemplate.postForObject(url, null, String.class);

			// Cũng là hàm void như ghi danh: thành công thì trả "null" hoặc rỗng.
			if (response != null && response.contains("\"exception\"")) {
				com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper()
						.readTree(response);
				System.err.println("[MoodleService] GỠ GHI DANH THẤT BẠI (" + node.path("errorcode").asText("")
						+ "): " + node.path("message").asText("") + " — user " + moodleUserId + ", khóa " + courseId);
				return false;
			}

			System.out.println("[MoodleService] Đã gỡ ghi danh: user " + moodleUserId + " khỏi khóa " + courseId);
			return true;

		} catch (Exception e) {
			System.err.println("[MoodleService] Lỗi gỡ ghi danh: " + e.getMessage());
			return false;
		}
	}

	@Override
	public String getMoodleAutoLoginUrl(String username, String password) {
		try {
			if (username == null) {
				return null;
			}

			String cleanUsername = username.trim().toLowerCase();

			// Gọi auth_userkey để lấy login URL dùng 1 lần
			// Token này là moodle.token trong application.properties
			String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken()
					+ "&wsfunction=auth_userkey_request_login_url" + "&moodlewsrestformat=json" + "&user[username]="
					+ cleanUsername;

			// Đường dẫn có kèm wstoken — đó là chìa khóa gọi mọi API Moodle, ai đọc
			// được log là dùng được. Chỉ ghi username.
			System.out.println("[MoodleService] Xin vé đăng nhập SSO cho: " + cleanUsername);

			String response = restTemplate.postForObject(url, null, String.class);

			// KHÔNG in cả response: khi thành công nó chứa loginurl kèm key đăng nhập
			// dùng một lần. Ai đọc được log trong vòng 5 phút là vào thẳng tài khoản
			// đó trên Moodle mà không cần mật khẩu.
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(response);

			if (node.has("exception")) {
				System.err.println("[MoodleService] Moodle từ chối (" + node.path("errorcode").asText("") + "): "
						+ node.path("message").asText(""));
				return null;
			}

			if (node.has("loginurl")) {
				System.out.println("[MoodleService] Đã cấp vé đăng nhập cho: " + cleanUsername);
				return node.get("loginurl").asText();
			}

			System.err.println("[MoodleService] Không có loginurl trong response!");
			return null;

		} catch (Exception e) {
			System.err.println("[MoodleService] Lỗi: " + e.getMessage());
			return null;
		}
	}
}