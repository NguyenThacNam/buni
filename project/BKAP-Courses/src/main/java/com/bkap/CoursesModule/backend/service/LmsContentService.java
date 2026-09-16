package com.bkap.CoursesModule.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
	 * Danh sách chương/mục hỏi bằng TOKEN CỦA HỌC VIÊN, không phải token dịch vụ:
	 * token dịch vụ là quyền quản trị, thấy hết mọi bài. Hỏi bằng token học viên
	 * thì Moodle tự áp "Hạn chế truy cập" (vd học xong Bài 1 mới mở Bài 3) — mục
	 * chưa đủ điều kiện trả về uservisible=false kèm lý do, mục bị giấu hẳn thì
	 * không trả về. buni hiện ổ khóa và KHÔNG phát link file cho những mục đó.
	 *
	 * @param tokenHv token Moodle của người đang học (lấy từ vé phiên)
	 * @throws IllegalStateException khi Moodle từ chối
	 */
	public Map<String, Object> layNoiDung(int moodleCourseId, String tenKhoa, String tokenHv) throws Exception {
		MultiValueMap<String, String> thamSo = new LinkedMultiValueMap<>();
		thamSo.add("courseid", String.valueOf(moodleCourseId));
		JsonNode chuong = goiBangToken(tokenHv, "core_course_get_contents", thamSo);
		if (!chuong.isArray()) {
			throw new IllegalStateException("Moodle không trả về nội dung khóa học");
		}

		// Nội dung trang (video, bài đọc) không nằm trong core_course_get_contents,
		// phải hỏi riêng rồi ghép vào theo coursemodule.
		Map<Integer, String> noiDungTrang = layNoiDungCacTrang(moodleCourseId);

		List<Map<String, Object>> dsChuong = new ArrayList<>();
		for (JsonNode c : chuong) {
			List<Map<String, Object>> dsMuc = new ArrayList<>();

			// Chương bị hạn chế: mọi mục bên trong cùng khóa, lý do nằm ở chương.
			boolean chuongBiKhoa = !c.path("uservisible").asBoolean(true);
			String lyDoChuong = lamSachLyDo(c.path("availabilityinfo").asText(""));

			for (JsonNode m : c.path("modules")) {
				// Ngân hàng câu hỏi (Moodle 5) không phải bài học. Học viên không thấy
				// loại này, nhưng token quản trị thì có — bỏ đi để admin xem đúng như
				// học viên.
				if ("qbank".equals(m.path("modname").asText())) {
					continue;
				}
				Map<String, Object> muc;
				if (chuongBiKhoa || !m.path("uservisible").asBoolean(true)) {
					String lyDo = lamSachLyDo(m.path("availabilityinfo").asText(""));
					muc = dungMucBiKhoa(m, lyDo.isEmpty() ? lyDoChuong : lyDo);
				} else {
					muc = dungMuc(m, noiDungTrang);
				}
				if (muc != null) {
					dsMuc.add(muc);
				}
			}

			// Bỏ chương rỗng — Moodle hay để sẵn chương trống chưa soạn nội dung.
			// Trừ chương bị khóa: Moodle không trả danh sách bài bên trong chương
			// khóa, nhưng vẫn phải hiện chương đó kèm lý do để học viên biết còn bài.
			if (dsMuc.isEmpty() && !chuongBiKhoa) {
				continue;
			}

			Map<String, Object> mapChuong = new LinkedHashMap<>();
			mapChuong.put("name", c.path("name").asText(""));
			if (chuongBiKhoa) {
				mapChuong.put("khoa", true);
				mapChuong.put("lyDoKhoa", lyDoChuong.isEmpty() ? "Chương này chưa mở cho bạn." : lyDoChuong);
			}
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

	/**
	 * Mục chưa đủ điều kiện mở: chỉ có tên, loại và lý do — không file, không nội
	 * dung, không đường dẫn sang LMS.
	 */
	private Map<String, Object> dungMucBiKhoa(JsonNode m, String lyDo) {
		Map<String, Object> muc = new LinkedHashMap<>();
		muc.put("type", m.path("modname").asText(""));
		muc.put("name", m.path("name").asText(""));
		muc.put("cmid", m.path("id").asInt());
		muc.put("khoa", true);
		muc.put("lyDoKhoa", lyDo.isEmpty() ? "Mục này chưa mở cho bạn." : lyDo);
		return muc;
	}

	/**
	 * Các mẫu điều kiện hay gặp trong "Hạn chế truy cập" của Moodle, và câu tiếng
	 * Việt tương ứng. {0} là tên hoạt động/ngày tháng bắt được.
	 *
	 * Vì sao phải tự dịch: gói tiếng Việt của Moodle dịch chưa đủ — phần khung
	 * ("Không hiện hữu trừ khi:") có, nhưng phần điều kiện ("The activity X is
	 * marked complete") vẫn tiếng Anh, ghép lại thành câu nửa Việt nửa Anh.
	 * Mẫu nào không khớp thì giữ nguyên chữ Moodle trả về.
	 */
	private static final String[][] MAU_DIEU_KIEN = {
			{ "(?i)^the activity (.+?) is complete and passed$", "Hoàn thành và đạt bài \u201C{0}\u201D" },
			{ "(?i)^the activity (.+?) is complete and failed$", "Làm bài \u201C{0}\u201D (chưa đạt)" },
			{ "(?i)^the activity (.+?) is not marked complete$", "Chưa hoàn thành bài \u201C{0}\u201D" },
			{ "(?i)^the activity (.+?) is marked complete$", "Hoàn thành bài \u201C{0}\u201D" },
			{ "(?i)^the previous activity is (?:marked )?complete(?:d)?$", "Hoàn thành bài liền trước" },
			{ "(?i)^you (?:get|achieve) a (?:certain|required) score in (.+)$", "Đạt điểm yêu cầu ở bài \u201C{0}\u201D" },
			{ "(?i)^from (.+)$", "Mở từ {0}" },
			{ "(?i)^until (.+)$", "Chỉ mở trước {0}" }, };

	/**
	 * Đổi lý do khóa Moodle trả về (HTML, lẫn Anh–Việt) thành một câu tiếng Việt
	 * gọn, kiểu "Hoàn thành bài “Bài luyện tập 1”". Nhiều điều kiện thì nối bằng
	 * "và"/"hoặc" theo đúng cài đặt bên Moodle.
	 */
	private String lamSachLyDo(String html) {
		if (html == null || html.isBlank()) {
			return "";
		}
		String chu = html.replaceAll("(?i)<li[^>]*>", "\n").replaceAll("<[^>]+>", " ");
		chu = chu.replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'")
				.replace("&lt;", "<").replace("&gt;", ">");

		// Phần mở đầu: "Không hiện hữu trừ khi:", "Not available unless any of:"...
		// Có "một trong" / "any of" thì chỉ cần đạt một điều kiện.
		String dau = chu.contains(":") ? chu.substring(0, chu.indexOf(':')).toLowerCase() : "";
		boolean chiCanMot = dau.contains("any of") || dau.contains("một trong");
		if (dau.contains("unless") || dau.contains("trừ khi") || dau.contains("không")) {
			chu = chu.substring(chu.indexOf(':') + 1);
		}

		List<String> dieuKien = new ArrayList<>();
		for (String dong : chu.split("\n")) {
			String sach = dong.replaceAll("\\s+", " ").trim();
			if (!sach.isEmpty()) {
				dieuKien.add(dichDieuKien(sach));
			}
		}
		return String.join(chiCanMot ? " hoặc " : " và ", dieuKien);
	}

	private String dichDieuKien(String dieuKien) {
		for (String[] mau : MAU_DIEU_KIEN) {
			Matcher m = Pattern.compile(mau[0]).matcher(dieuKien);
			if (m.matches()) {
				return m.groupCount() > 0 ? mau[1].replace("{0}", m.group(1).trim()) : mau[1];
			}
		}
		return dieuKien;
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
		return timHoatDong(moodleCourseId, cmid) != null;
	}

	/**
	 * Hoạt động cmid trong khóa moodleCourseId (kèm modname, instance...), hoặc
	 * null nếu không thuộc khóa này hay không tra được.
	 */
	public JsonNode timHoatDong(int moodleCourseId, int cmid) {
		try {
			JsonNode chuong = goiMoodle("core_course_get_contents", "&courseid=" + moodleCourseId);
			for (JsonNode c : chuong) {
				for (JsonNode m : c.path("modules")) {
					if (m.path("id").asInt() == cmid) {
						return m;
					}
				}
			}
		} catch (Exception e) {
			// Không tra được thì coi như không khớp. Thà chặn nhầm còn hơn mở nhầm.
			System.err.println("[LmsContent] Không đối chiếu được cmid: " + e.getMessage());
		}
		return null;
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

	/** POST bằng token của học viên — token không nằm trên URL để khỏi lọt vào log máy chủ web. */
	private JsonNode goiBangToken(String token, String wsfunction, MultiValueMap<String, String> thamSo)
			throws Exception {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>(thamSo);
		f.add("wstoken", token);
		f.add("wsfunction", wsfunction);
		f.add("moodlewsrestformat", "json");
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		JsonNode node = objectMapper
				.readTree(restTemplate.postForObject(moodleConfig.getBaseUrl(), new HttpEntity<>(f, h), String.class));
		if (node.has("exception")) {
			throw new IllegalStateException("Moodle từ chối " + wsfunction + " ("
					+ node.path("errorcode").asText("") + "): " + node.path("message").asText(""));
		}
		return node;
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
