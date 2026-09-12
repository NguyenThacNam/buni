package com.bkap.CoursesModule.backend.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.bkap.CoursesModule.backend.repository.ICategoryRepository;
import com.bkap.CoursesModule.backend.repository.ICourseRepository;
import com.bkap.CoursesModule.entity.Category;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.UserModule.backend.repository.IUserRepository;
import com.bkap.UserModule.entity.User;
import com.bkap.UserModule.entity.UserRole;
import com.bkap.config.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kéo danh mục và khóa học từ Moodle về buni.
 *
 * Nguyên tắc xuyên suốt: **chỉ tạo mới, không ghi đè**.
 *
 * Khóa đã có bên buni thì giữ nguyên, kể cả khi bên Moodle đổi tên. Nếu đồng bộ
 * mà ghi đè thì admin sửa tên khóa cho hợp bán hàng xong, lần sau bấm đồng bộ là
 * mất trắng — mà lại khó phát hiện vì không có thông báo gì.
 *
 * Muốn lấy lại thông tin mới từ Moodle cho một khóa cụ thể thì làm bằng thao tác
 * riêng, có chủ đích, chứ không lẫn vào đây.
 */
@Service
public class LmsSyncService {

	/**
	 * Moodle luôn có một bản ghi khóa học id = 1, đó là TRANG CHỦ của site chứ
	 * không phải khóa học. Không bỏ qua thì trang chủ buni mọc ra một "khóa học"
	 * tên "BUNI LMS".
	 */
	private static final int ID_KHOA_TRANG_CHU_MOODLE = 1;

	@Autowired
	private MoodleConfig moodleConfig;

	@Autowired
	private ICategoryRepository categoryRepository;

	@Autowired
	private ICourseRepository courseRepository;

	@Autowired
	private IUserRepository userRepository;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/** Tóm tắt một lần đồng bộ, để hiện lên giao diện admin. */
	public record KetQua(int danhMucMoi, int khoaHocMoi, int khoaHocBoQua, String loi) {
		public boolean thanhCong() {
			return loi == null;
		}
	}

	@Transactional
	public KetQua dongBo() {
		try {
			// Danh mục phải xong trước: khóa học cần category_id, mà cột đó NOT NULL.
			int danhMucMoi = dongBoDanhMuc();
			int[] khoa = dongBoKhoaHoc();
			return new KetQua(danhMucMoi, khoa[0], khoa[1], null);
		} catch (Exception e) {
			System.err.println("[LmsSync] Lỗi đồng bộ: " + e.getMessage());
			return new KetQua(0, 0, 0, e.getMessage());
		}
	}

	// ─────────────────────────────────────────────────────────────
	// DANH MỤC
	// ─────────────────────────────────────────────────────────────

	private int dongBoDanhMuc() throws Exception {
		JsonNode ds = goiMoodle("core_course_get_categories");
		if (!ds.isArray()) {
			throw new IllegalStateException("Moodle không trả về danh sách danh mục");
		}

		int soMoi = 0;

		// Lượt 1: tạo những danh mục chưa có. Chưa gán cha vội, vì danh mục cha có
		// thể nằm sau trong danh sách và lúc đó chưa tồn tại bên buni.
		for (JsonNode n : ds) {
			int moodleId = n.path("id").asInt();
			if (categoryRepository.findByMoodleCategoryId(moodleId).isPresent()) {
				continue;
			}

			Category c = new Category();
			c.setName(giaiMaHtml(n.path("name").asText("")));
			c.setSlug(slugDuyNhat(c.getName(), true));
			c.setMoodleCategoryId(moodleId);
			c.setStatus("active");
			c.setPrioty(n.path("depth").asInt(1) * 100 + n.path("id").asInt());
			categoryRepository.save(c);
			soMoi++;
		}

		// Lượt 2: nối cha con, giờ mọi danh mục đều đã tồn tại.
		Map<Integer, Short> moodleToLocal = new HashMap<>();
		for (Category c : categoryRepository.findAll()) {
			if (c.getMoodleCategoryId() != null) {
				moodleToLocal.put(c.getMoodleCategoryId(), c.getId());
			}
		}

		for (JsonNode n : ds) {
			int moodleId = n.path("id").asInt();
			int moodleParent = n.path("parent").asInt(0);

			Optional<Category> opt = categoryRepository.findByMoodleCategoryId(moodleId);
			if (opt.isEmpty()) {
				continue;
			}
			Category c = opt.get();

			// parent = 0 bên Moodle nghĩa là danh mục gốc
			Short chaMoi = moodleParent == 0 ? null : moodleToLocal.get(moodleParent);
			if (chaMoi != null && !chaMoi.equals(c.getParentId())) {
				c.setParentId(chaMoi);
				categoryRepository.save(c);
			}
		}

		return soMoi;
	}

	// ─────────────────────────────────────────────────────────────
	// KHÓA HỌC
	// ─────────────────────────────────────────────────────────────

	/** @return [số khóa tạo mới, số khóa bỏ qua vì đã có] */
	private int[] dongBoKhoaHoc() throws Exception {
		JsonNode goi = goiMoodle("core_course_get_courses_by_field");
		JsonNode ds = goi.path("courses");
		if (!ds.isArray()) {
			throw new IllegalStateException("Moodle không trả về danh sách khóa học");
		}

		User giangVienMacDinh = timGiangVienMacDinh();
		int moi = 0, boQua = 0;

		for (JsonNode n : ds) {
			int moodleId = n.path("id").asInt();
			if (moodleId == ID_KHOA_TRANG_CHU_MOODLE) {
				continue;
			}

			if (courseRepository.findByMoodleCourseId(moodleId).isPresent()) {
				boQua++;
				continue;
			}

			Course c = new Course();
			c.setMoodleCourseId(moodleId);
			c.setTitle(giaiMaHtml(n.path("fullname").asText("Khóa học " + moodleId)));
			c.setSlug(slugDuyNhat(c.getTitle(), false));
			c.setDescription(n.path("summary").asText(""));

			String anh = n.path("courseimage").asText("");
			if (!anh.isBlank()) {
				c.setThumbnailUrl(anh);
			}

			c.setCategory(timDanhMuc(n.path("categoryid").asInt(0)));
			c.setInstructor(giangVienMacDinh);

			// Giá trị mặc định an toàn — admin vào chỉnh sau.
			c.setLevel("ALL");
			c.setPriceType("CONTACT");
			c.setPrice(BigDecimal.ZERO);
			c.setOriginalPrice(BigDecimal.ZERO);

			// Ẩn cho tới khi admin điền xong và bật lên.
			c.setStatus("inactive");

			courseRepository.save(c);
			moi++;
		}

		return new int[] { moi, boQua };
	}

	/**
	 * Khóa học bắt buộc phải có danh mục (cột NOT NULL). Nếu khóa bên Moodle không
	 * thuộc danh mục nào, hoặc danh mục đó chưa kéo về, thì lấy tạm danh mục bất
	 * kỳ để bản ghi còn lưu được — admin đổi lại sau.
	 */
	private Category timDanhMuc(int moodleCategoryId) {
		if (moodleCategoryId > 0) {
			Optional<Category> opt = categoryRepository.findByMoodleCategoryId(moodleCategoryId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		List<Category> tatCa = categoryRepository.findAll();
		if (tatCa.isEmpty()) {
			throw new IllegalStateException("Chưa có danh mục nào để gán cho khóa học");
		}
		return tatCa.get(0);
	}

	/**
	 * Tương tự, instructor_id cũng NOT NULL. Moodle không có khái niệm "một giảng
	 * viên của khóa" để lấy thẳng, nên gán tạm một tài khoản quản trị.
	 */
	private User timGiangVienMacDinh() {
		for (User u : userRepository.findAll()) {
			if (u.getRole() == UserRole.ADMIN) {
				return u;
			}
		}
		List<User> tatCa = userRepository.findAll();
		if (tatCa.isEmpty()) {
			throw new IllegalStateException("Chưa có người dùng nào để gán làm giảng viên");
		}
		return tatCa.get(0);
	}

	// ─────────────────────────────────────────────────────────────
	// TIỆN ÍCH
	// ─────────────────────────────────────────────────────────────

	private JsonNode goiMoodle(String wsfunction) throws Exception {
		String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken() + "&wsfunction=" + wsfunction
				+ "&moodlewsrestformat=json";

		String phanHoi = restTemplate.getForObject(url, String.class);
		JsonNode node = objectMapper.readTree(phanHoi);

		// Moodle trả object lỗi thay vì mảng khi có chuyện. Không kiểm tra ở đây thì
		// lỗi trôi xuống dưới rồi vỡ ở chỗ khó hiểu hơn nhiều.
		if (node.has("exception")) {
			throw new IllegalStateException(
					"Moodle từ chối " + wsfunction + " (" + node.path("errorcode").asText("") + "): "
							+ node.path("message").asText(""));
		}
		return node;
	}

	/**
	 * Moodle trả tên đã mã hoá HTML: "Trí tuệ nhân tạo &amp; Dữ liệu". Không giải
	 * mã thì trang web hiện nguyên chữ "&amp;".
	 */
	private String giaiMaHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
				.replace("&#039;", "'").replace("&#39;", "'").replace("&nbsp;", " ").trim();
	}

	/** Sinh slug từ tên tiếng Việt, và thêm hậu tố nếu đã có ai dùng. */
	private String slugDuyNhat(String ten, boolean laDanhMuc) {
		String goc = taoSlug(ten);
		if (goc.isBlank()) {
			goc = "muc";
		}
		int gioiHan = laDanhMuc ? 100 : 200;
		if (goc.length() > gioiHan - 6) {
			goc = goc.substring(0, gioiHan - 6);
		}

		String slug = goc;
		int dem = 2;
		while (laDanhMuc ? categoryRepository.existsBySlug(slug) : courseRepository.existsBySlug(slug)) {
			slug = goc + "-" + dem++;
		}
		return slug;
	}

	private String taoSlug(String ten) {
		if (ten == null) {
			return "";
		}
		// NFD tách chữ thành ký tự gốc + dấu, rồi bỏ phần dấu đi.
		// Đ/đ không nằm trong quy tắc đó nên phải thay riêng.
		String s = Normalizer.normalize(ten, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace("Đ", "D")
				.replace("đ", "d").toLowerCase(Locale.ROOT);
		return s.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
	}
}
