package com.bkap.CoursesModule.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bkap.config.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lấy nội dung học của một khóa từ Moodle rồi dọn thành cấu trúc gọn cho
 * frontend.
 *
 * Frontend KHÔNG gọi thẳng Moodle. Lý do: mọi lời gọi Moodle đều phải kèm token
 * dịch vụ, mà token đó mở được toàn bộ API — nhét vào trang web là ai mở
 * DevTools cũng lấy được. Nên backend đứng giữa, giữ token cho riêng mình.
 */
@Service
public class LmsContentService {

	/** Bắt id video từ mọi kiểu URL YouTube thường gặp. */
	private static final Pattern YOUTUBE = Pattern
			.compile("(?:youtube\\.com/(?:watch\\?v=|embed/)|youtu\\.be/)([A-Za-z0-9_-]{6,})");

	/** Cắt lấy phần đường dẫn nằm sau /pluginfile.php trong link file của Moodle. */
	private static final Pattern DUONG_DAN_FILE = Pattern.compile("/pluginfile\\.php(/[^?]*)");

	@Autowired
	private MoodleConfig moodleConfig;

	@Value("${moodle.site-url}")
	private String moodleSiteUrl;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Trả về cấu trúc học của khóa: các chương, mỗi chương gồm các mục.
	 *
	 * @throws IllegalStateException khi Moodle từ chối
	 */
	public Map<String, Object> layNoiDung(int moodleCourseId, String tenKhoa) throws Exception {
		JsonNode chuong = goiMoodle("core_course_get_contents", "&courseid=" + moodleCourseId);
		if (!chuong.isArray()) {
			throw new IllegalStateException("Moodle không trả về nội dung khóa học");
		}

		// Nội dung trang (video, bài đọc) không nằm trong core_course_get_contents,
		// phải hỏi riêng rồi ghép vào theo coursemodule.
		Map<Integer, String> noiDungTrang = layNoiDungCacTrang(moodleCourseId);

		List<Map<String, Object>> dsChuong = new ArrayList<>();
		for (JsonNode c : chuong) {
			List<Map<String, Object>> dsMuc = new ArrayList<>();

			for (JsonNode m : c.path("modules")) {
				Map<String, Object> muc = dungMuc(m, noiDungTrang);
				if (muc != null) {
					dsMuc.add(muc);
				}
			}

			// Bỏ chương rỗng — Moodle hay để sẵn chương trống chưa soạn nội dung.
			if (dsMuc.isEmpty()) {
				continue;
			}

			Map<String, Object> mapChuong = new LinkedHashMap<>();
			mapChuong.put("name", c.path("name").asText(""));
			mapChuong.put("modules", dsMuc);
			dsChuong.add(mapChuong);
		}

		Map<String, Object> ketQua = new LinkedHashMap<>();
		ketQua.put("courseId", moodleCourseId);
		ketQua.put("title", tenKhoa);
		ketQua.put("moodleCourseId", moodleCourseId);
		ketQua.put("sections", dsChuong);
		return ketQua;
	}

	/** Dựng một mục học từ dữ liệu Moodle, hoặc null nếu không hỗ trợ loại đó. */
	private Map<String, Object> dungMuc(JsonNode m, Map<Integer, String> noiDungTrang) {
		String loai = m.path("modname").asText("");
		int cmid = m.path("id").asInt();

		Map<String, Object> muc = new LinkedHashMap<>();
		muc.put("type", loai);
		muc.put("name", m.path("name").asText(""));
		muc.put("cmid", cmid);

		switch (loai) {
		case "resource" -> {
			// Tài liệu: PDF, slide, hoặc file video tải lên Moodle.
			JsonNode file = m.path("contents").path(0);
			if (file.isMissingNode()) {
				return null;
			}
			String duongDan = catDuongDanFile(file.path("fileurl").asText(""));
			if (duongDan == null) {
				return null;
			}
			muc.put("filename", file.path("filename").asText(""));
			muc.put("mimetype", file.path("mimetype").asText(""));
			muc.put("filesize", file.path("filesize").asLong(0));
			muc.put("filePath", duongDan);
		}
		case "page" -> {
			// Trang nội dung: thường là video nhúng, có thể là bài đọc.
			String html = noiDungTrang.getOrDefault(cmid, "");
			muc.put("html", html);

			String youtube = timYoutube(html);
			if (youtube != null) {
				muc.put("youtubeId", youtube);
			}
		}
		case "quiz", "forum", "assign" -> {
			// Mấy loại này để Moodle lo. buni chỉ đưa đường dẫn để mở sang đó.
			muc.put("moodleUrl", moodleSiteUrl + "/mod/" + loai + "/view.php?id=" + cmid);
		}
		default -> {
			// Loại chưa hỗ trợ: vẫn hiện tên để người học biết có mục này, bấm vào
			// thì mở bên Moodle. Tốt hơn là giấu đi khiến họ tưởng thiếu bài.
			muc.put("moodleUrl", moodleSiteUrl + "/mod/" + loai + "/view.php?id=" + cmid);
		}
		}

		return muc;
	}

	/**
	 * Hoạt động cmid có nằm trong khóa moodleCourseId không.
	 *
	 * Dùng để chặn việc mượn cơ chế đăng nhập một lần của buni mà chui sang hoạt
	 * động của khóa khác: người học chỉ khai mã hoạt động, còn khóa thì backend
	 * tự tra từ tài khoản, nên phải đối chiếu hai thứ khớp nhau.
	 */
	public boolean cmidThuocKhoa(int moodleCourseId, int cmid) {
		try {
			JsonNode chuong = goiMoodle("core_course_get_contents", "&courseid=" + moodleCourseId);
			for (JsonNode c : chuong) {
				for (JsonNode m : c.path("modules")) {
					if (m.path("id").asInt() == cmid) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			// Không tra được thì coi như không khớp. Thà chặn nhầm còn hơn mở nhầm.
			System.err.println("[LmsContent] Không đối chiếu được cmid: " + e.getMessage());
		}
		return false;
	}

	/** Gọi mod_page_get_pages_by_courses, trả về map coursemodule -> nội dung HTML. */
	private Map<Integer, String> layNoiDungCacTrang(int moodleCourseId) {
		Map<Integer, String> ket = new LinkedHashMap<>();
		try {
			JsonNode r = goiMoodle("mod_page_get_pages_by_courses", "&courseids[0]=" + moodleCourseId);
			for (JsonNode p : r.path("pages")) {
				ket.put(p.path("coursemodule").asInt(), p.path("content").asText(""));
			}
		} catch (Exception e) {
			// Thiếu hàm hoặc thiếu quyền thì vẫn hiện được danh sách mục, chỉ là
			// trang nội dung trống. Không đáng để hỏng cả trang học.
			System.err.println("[LmsContent] Không lấy được nội dung trang: " + e.getMessage());
		}
		return ket;
	}

	private String timYoutube(String html) {
		if (html == null || html.isBlank()) {
			return null;
		}
		Matcher m = YOUTUBE.matcher(html);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Link file Moodle có dạng
	 * https://lms.buni.vn/webservice/pluginfile.php/47/mod_resource/content/1/x.pdf
	 * Chỉ giữ lại phần "/47/mod_resource/content/1/x.pdf".
	 *
	 * Trả phần này cho frontend thay vì URL đầy đủ, để frontend không có cách nào
	 * gọi thẳng sang Moodle — mọi file đều phải đi qua backend, nơi giữ token.
	 */
	private String catDuongDanFile(String fileurl) {
		if (fileurl == null || fileurl.isBlank()) {
			return null;
		}
		Matcher m = DUONG_DAN_FILE.matcher(fileurl);
		return m.find() ? m.group(1) : null;
	}

	private JsonNode goiMoodle(String wsfunction, String thamSo) throws Exception {
		String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken() + "&wsfunction=" + wsfunction
				+ "&moodlewsrestformat=json" + thamSo;

		JsonNode node = objectMapper.readTree(restTemplate.getForObject(url, String.class));
		if (node.has("exception")) {
			throw new IllegalStateException("Moodle từ chối " + wsfunction + " ("
					+ node.path("errorcode").asText("") + "): " + node.path("message").asText(""));
		}
		return node;
	}
}
