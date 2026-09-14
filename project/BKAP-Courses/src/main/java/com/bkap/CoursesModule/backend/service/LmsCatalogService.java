package com.bkap.CoursesModule.backend.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bkap.config.JwtUtil;
import com.bkap.config.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Danh mục và khóa học, lấy thẳng từ LMS.
 *
 * Đây là bước đầu của việc chuyển sang "LMS là nguồn dữ liệu duy nhất": bảng
 * course và category bên buni thôi được dùng cho trang công khai. Hai nơi cùng
 * giữ một sự thật thì kiểu gì cũng lệch — đã xảy ra rồi: một khóa bên buni bị
 * nối nhầm sang khóa LMS khác, và form sửa danh mục từng ghi đè mất mã LMS.
 *
 * Hình dạng dữ liệu trả ra giữ y như cũ (title, slug, thumbnailUrl, category…)
 * để frontend không phải sửa gì. Những trường Moodle không có — giá, đánh giá,
 * trình độ — trả về rỗng.
 */
@Service
public class LmsCatalogService {

	/**
	 * Khóa id=1 bên Moodle là "khóa trang chủ", một khóa ảo Moodle luôn tạo sẵn
	 * để chứa nội dung trang chủ. Không phải khóa học thật.
	 */
	private static final int ID_KHOA_TRANG_CHU_MOODLE = 1;

	/** Cắt phần đường dẫn sau /pluginfile.php trong link file của Moodle. */
	private static final Pattern DUONG_DAN_FILE = Pattern.compile("/pluginfile\\.php(/[^?]*)");

	@Autowired
	private MoodleConfig moodleConfig;

	@Autowired
	private JwtUtil jwtUtil;

	/**
	 * Thời gian giữ dữ liệu trong bộ nhớ đệm.
	 *
	 * Bỏ CSDL nghĩa là mỗi lượt xem trang đều phải hỏi Moodle, mỗi lần mất
	 * 0,3–1 giây. Không có bộ đệm thì trang danh sách khóa học sẽ ì, và Moodle
	 * lãnh trọn lưu lượng của buni. Đổi lại: sửa khóa học bên LMS thì buni chậm
	 * hơn chừng này giây mới thấy.
	 */
	@Value("${app.lms.cache-seconds:120}")
	private long giayGiuDem;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final ODem<List<Map<String, Object>>> demKhoaHoc = new ODem<>();
	private final ODem<List<Map<String, Object>>> demDanhMuc = new ODem<>();

	// ─────────────────────────────────────────────────────────────
	// KHÓA HỌC
	// ─────────────────────────────────────────────────────────────

	/** Mọi khóa đang hiện trên LMS, trừ khóa trang chủ. */
	public List<Map<String, Object>> danhSachKhoa() throws Exception {
		return demKhoaHoc.lay(giayGiuDem * 1000, this::napKhoaHoc);
	}

	/** Một khóa theo id Moodle, hoặc null nếu không có / đang ẩn. */
	public Map<String, Object> chiTietKhoa(int id) throws Exception {
		// Lấy từ danh sách đã đệm thay vì gọi Moodle lần nữa: người xem thường
		// bấm từ trang danh sách sang, dữ liệu vừa nạp xong còn nóng.
		for (Map<String, Object> khoa : danhSachKhoa()) {
			if (Integer.valueOf(id).equals(khoa.get("id"))) {
				return khoa;
			}
		}
		return null;
	}

	/** Khóa thuộc danh mục này, tính cả các danh mục con của nó. */
	public List<Map<String, Object>> khoaTheoDanhMuc(String slug) throws Exception {
		Set<Integer> ids = idDanhMucVaConChau(slug);
		if (ids.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> ket = new ArrayList<>();
		for (Map<String, Object> khoa : danhSachKhoa()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> danhMuc = (Map<String, Object>) khoa.get("category");
			if (danhMuc != null && ids.contains(danhMuc.get("id"))) {
				ket.add(khoa);
			}
		}
		return ket;
	}

	private List<Map<String, Object>> napKhoaHoc() throws Exception {
		JsonNode r = goiMoodle("core_course_get_courses_by_field", "");
		Map<Integer, Map<String, Object>> danhMucTheoId = danhMucTheoId();

		List<Map<String, Object>> ds = new ArrayList<>();
		for (JsonNode c : r.path("courses")) {
			int id = c.path("id").asInt();
			if (id == ID_KHOA_TRANG_CHU_MOODLE || c.path("visible").asInt(1) == 0) {
				continue;
			}
			ds.add(dungKhoa(c, danhMucTheoId));
		}
		return ds;
	}

	private Map<String, Object> dungKhoa(JsonNode c, Map<Integer, Map<String, Object>> danhMucTheoId) {
		String ten = giaiMa(c.path("fullname").asText(""));

		Map<String, Object> k = new LinkedHashMap<>();
		k.put("id", c.path("id").asInt());
		// Giữ lại tên trường cũ: frontend dựa vào nó để biết khóa đã mở trên LMS
		// chưa. Giờ mọi khóa đều từ LMS mà ra nên nó luôn có giá trị.
		k.put("moodleCourseId", c.path("id").asInt());
		k.put("title", ten);
		k.put("slug", taoSlug(ten));
		k.put("subtitle", null);
		k.put("description", c.path("summary").asText(""));
		k.put("thumbnailUrl", anhBia(c));
		k.put("category", danhMucTheoId.get(c.path("categoryid").asInt()));
		k.put("instructor", giangVien(c));
		k.put("status", "active");

		// Moodle không có mấy thứ này. Trung tâm đã chốt ẩn giá và đánh giá, nên
		// để rỗng. Cần lại thì tạo "custom course field" bên Moodle rồi đọc ở đây,
		// đừng thêm cột vào CSDL buni — sẽ lệch trở lại.
		k.put("priceType", "CONTACT");
		k.put("price", null);
		k.put("originalPrice", null);
		k.put("rating", 0);
		k.put("ratingCount", 0);
		k.put("studentCount", 0);
		k.put("isPro", 0);
		k.put("level", null);
		k.put("durationText", null);
		k.put("previewVideoUrl", null);
		return k;
	}

	/**
	 * Ảnh bìa khóa học, trả về dưới dạng đường dẫn đã ký của buni.
	 *
	 * Link Moodle cho sẵn đòi token dịch vụ mới tải được, mà token đó mở toàn bộ
	 * API nên không thể để lộ ra trình duyệt. Đi qua proxy /learn/file như PDF và
	 * video của trang học.
	 */
	private String anhBia(JsonNode c) {
		String url = c.path("overviewfiles").path(0).path("fileurl").asText("");
		if (url.isBlank()) {
			return null;
		}
		Matcher m = DUONG_DAN_FILE.matcher(url);
		return m.find() ? "/api/v1/learn/file?t=" + jwtUtil.kyDuongDanFile(m.group(1)) : null;
	}

	/** Giáo viên đứng lớp, lấy từ danh sách liên hệ của khóa bên Moodle. */
	private Map<String, Object> giangVien(JsonNode c) {
		JsonNode nguoi = c.path("contacts").path(0);
		if (nguoi.isMissingNode()) {
			return null;
		}
		Map<String, Object> gv = new LinkedHashMap<>();
		gv.put("fullname", giaiMa(nguoi.path("fullname").asText("")));
		return gv;
	}

	// ─────────────────────────────────────────────────────────────
	// DANH MỤC
	// ─────────────────────────────────────────────────────────────

	/** Danh mục để hiện ngoài trang, kèm parentId để frontend dựng cây. */
	public List<Map<String, Object>> danhMucCongKhai() throws Exception {
		return demDanhMuc.lay(giayGiuDem * 1000, this::napDanhMuc);
	}

	private List<Map<String, Object>> napDanhMuc() throws Exception {
		JsonNode r = goiMoodle("core_course_get_categories", "");
		List<Map<String, Object>> ds = new ArrayList<>();
		for (JsonNode c : r) {
			if (c.path("visible").asInt(1) == 0) {
				continue;
			}
			String ten = giaiMa(c.path("name").asText(""));
			int cha = c.path("parent").asInt(0);

			Map<String, Object> d = new LinkedHashMap<>();
			d.put("id", c.path("id").asInt());
			d.put("moodleCategoryId", c.path("id").asInt());
			d.put("name", ten);
			d.put("slug", taoSlug(ten));
			// Moodle dùng 0 cho "không có cha"; frontend chờ null.
			d.put("parentId", cha == 0 ? null : cha);
			d.put("prioty", c.path("sortorder").asInt(0));
			d.put("status", "active");
			ds.add(d);
		}
		return ds;
	}

	/** Id của danh mục có slug này, cộng mọi danh mục con cháu của nó. */
	private Set<Integer> idDanhMucVaConChau(String slug) throws Exception {
		List<Map<String, Object>> tatCa = danhMucCongKhai();
		Set<Integer> ket = new HashSet<>();
		for (Map<String, Object> d : tatCa) {
			if (slug.equals(d.get("slug"))) {
				ket.add((Integer) d.get("id"));
			}
		}
		boolean themDuoc = !ket.isEmpty();
		while (themDuoc) {
			themDuoc = false;
			for (Map<String, Object> d : tatCa) {
				Object cha = d.get("parentId");
				if (cha != null && ket.contains(cha) && ket.add((Integer) d.get("id"))) {
					themDuoc = true;
				}
			}
		}
		return ket;
	}

	private Map<Integer, Map<String, Object>> danhMucTheoId() throws Exception {
		Map<Integer, Map<String, Object>> theoId = new LinkedHashMap<>();
		for (Map<String, Object> d : danhMucCongKhai()) {
			// Chỉ đưa sang frontend những gì thẻ khóa học cần, khỏi lồng cả cây.
			Map<String, Object> gon = new LinkedHashMap<>();
			gon.put("id", d.get("id"));
			gon.put("name", d.get("name"));
			gon.put("slug", d.get("slug"));
			theoId.put((Integer) d.get("id"), gon);
		}
		return theoId;
	}

	// ─────────────────────────────────────────────────────────────
	// TIỆN ÍCH
	// ─────────────────────────────────────────────────────────────

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

	/** Moodle trả tên có mã hóa HTML: "Trí tuệ nhân tạo &amp; Dữ liệu". */
	private String giaiMa(String s) {
		return s == null ? "" : s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
				.replace("&quot;", "\"").replace("&#039;", "'").replace("&nbsp;", " ").trim();
	}

	/** "Chuyển đổi số" -> "chuyen-doi-so", dùng cho đường dẫn và bộ lọc danh mục. */
	private String taoSlug(String ten) {
		String s = Normalizer.normalize(ten == null ? "" : ten, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.replace('đ', 'd').replace('Đ', 'D').toLowerCase();
		s = s.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
		return s.isEmpty() ? "khoa-hoc" : s;
	}

	// ─────────────────────────────────────────────────────────────
	// BỘ NHỚ ĐỆM
	// ─────────────────────────────────────────────────────────────

	private interface NapDuLieu<T> {
		T nap() throws Exception;
	}

	/**
	 * Một ô nhớ đệm có hạn dùng.
	 *
	 * Tự viết thay vì thêm thư viện: chỉ cần đúng một ô có hạn, mà thêm thư viện
	 * thì máy phải tải về được — dự án đang biên dịch ở chế độ offline.
	 *
	 * Hết hạn thì lần gọi đầu tiên đi nạp lại, các lời gọi khác chờ (synchronized)
	 * rồi dùng chung kết quả — tránh cả chục request cùng đâm sang Moodle một lúc.
	 * Nạp hỏng thì ném lỗi lên và KHÔNG ghi đè dữ liệu cũ.
	 */
	private static final class ODem<T> {
		private T giaTri;
		private long hetHanLuc;

		synchronized T lay(long hanMs, NapDuLieu<T> nap) throws Exception {
			long bayGio = System.currentTimeMillis();
			if (giaTri == null || bayGio >= hetHanLuc) {
				giaTri = nap.nap();
				hetHanLuc = bayGio + hanMs;
			}
			return giaTri;
		}
	}
}
