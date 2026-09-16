package com.bkap.CoursesModule.backend.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
 * Tiến độ học và điểm bài kiểm tra, lấy từ LMS.
 *
 * Thay cho cột progress_percent trong bảng enrollment — cột đó do admin gõ tay,
 * gần như không ai cập nhật nên luôn bằng 0. Moodle thì tự ghi nhận: học viên
 * mở tài liệu, xem hết video, nộp bài kiểm tra là nó đánh dấu.
 *
 * Mọi hàm ở đây đều "hỏng thì thôi": không lấy được thì trả về rỗng chứ không
 * ném lỗi. Tiến độ chỉ là thông tin thêm, không đáng để làm hỏng cả trang học.
 */
@Service
public class LmsTienDoService {

	/** Moodle: 0 = chưa xong, 1 = xong, 2 = xong-đạt, 3 = xong-trượt. */
	private static final int CHUA_HOAN_THANH = 0;
	private static final int HOAN_THANH = 1;
	private static final int HOAN_THANH_DAT = 2;

	@Autowired
	private MoodleConfig moodleConfig;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Trạng thái hoàn thành của một hoạt động.
	 *
	 * @param xong      Moodle đã tính hoàn thành chưa
	 * @param tuDanhDau hoạt động kiểu "học viên tự đánh dấu" — giao diện hiện nút
	 * @param canXem    điều kiện có "phải xem hoạt động"
	 * @param daXem     điều kiện "phải xem" đã đạt chưa (bài kiểm tra có thể đã xem
	 *                  mà chưa xong vì còn chờ điểm)
	 */
	public record TrangThaiMuc(boolean xong, boolean tuDanhDau, boolean canXem, boolean daXem) {
	}

	/** Moodle: tracking 1 = học viên tự đánh dấu, 2 = tự động theo điều kiện. */
	private static final int THEO_DOI_THU_CONG = 1;

	/**
	 * Trạng thái hoàn thành từng hoạt động: cmid -> chi tiết.
	 *
	 * Chỉ tính những hoạt động có bật theo dõi hoàn thành bên Moodle. Khóa nào
	 * giáo viên chưa bật theo dõi thì trả về rỗng — và như vậy là đúng, thà không
	 * hiện tiến độ còn hơn hiện 0% cho người đã học xong.
	 */
	public Map<Integer, TrangThaiMuc> chiTietHoanThanh(int moodleCourseId, int moodleUserId) {
		Map<Integer, TrangThaiMuc> ket = new LinkedHashMap<>();
		try {
			JsonNode r = goi("core_completion_get_activities_completion_status",
					"&courseid=" + moodleCourseId + "&userid=" + moodleUserId);
			for (JsonNode s : r.path("statuses")) {
				if (!s.path("hascompletion").asBoolean(false)) {
					continue;
				}
				boolean canXem = false;
				boolean daXem = false;
				for (JsonNode d : s.path("details")) {
					if ("completionview".equals(d.path("rulename").asText())) {
						canXem = true;
						daXem = d.path("rulevalue").path("status").asInt(0) != CHUA_HOAN_THANH;
					}
				}
				ket.put(s.path("cmid").asInt(), new TrangThaiMuc(
						// Chỉ tính "xong" và "xong-đạt". "Xong-trượt" (3) — vd nộp bài kiểm tra
						// dưới điểm qua — Moodle KHÔNG coi là hoàn thành: điều kiện mở bài
						// sau vẫn khóa, nên buni cũng không được hiện dấu tích.
						laHoanThanh(s.path("state").asInt(CHUA_HOAN_THANH)),
						s.path("tracking").asInt() == THEO_DOI_THU_CONG, canXem, daXem));
			}
		} catch (Exception e) {
			// Hay gặp nhất: vai trò tài khoản dịch vụ thiếu quyền report/progress:view.
			System.err.println("[LmsTienDo] Không lấy được tiến độ khóa " + moodleCourseId + ": " + e.getMessage());
		}
		return ket;
	}

	private static boolean laHoanThanh(int trangThai) {
		return trangThai == HOAN_THANH || trangThai == HOAN_THANH_DAT;
	}

	/** Như chiTietHoanThanh nhưng chỉ lấy cờ xong/chưa: cmid -> đã xong chưa. */
	public Map<Integer, Boolean> trangThaiHoanThanh(int moodleCourseId, int moodleUserId) {
		Map<Integer, Boolean> ket = new LinkedHashMap<>();
		chiTietHoanThanh(moodleCourseId, moodleUserId).forEach((cmid, t) -> ket.put(cmid, t.xong()));
		return ket;
	}

	// ─────────────────────────────────────────────────────────────
	// GHI NHẬN TỪ BUNI
	// ─────────────────────────────────────────────────────────────

	/**
	 * Báo Moodle là học viên vừa mở một hoạt động trên buni.
	 *
	 * Học viên đọc PDF, xem video ở buni thì Moodle không hề biết — điều kiện
	 * "phải xem hoạt động" không bao giờ đạt. Gọi đúng hàm "view" của từng loại
	 * (chính hàm app Moodle trên điện thoại gọi) bằng TOKEN CỦA HỌC VIÊN, để Moodle
	 * ghi nhận dưới tên người đó và tự đánh dấu hoàn thành.
	 *
	 * @param hoatDong module lấy từ core_course_get_contents (cần modname, instance)
	 * @return false nếu loại hoạt động này chưa hỗ trợ
	 */
	public boolean baoDaXem(String tokenHv, JsonNode hoatDong) {
		String[] ham = switch (hoatDong.path("modname").asText("")) {
		case "resource" -> new String[] { "mod_resource_view_resource", "resourceid" };
		case "page" -> new String[] { "mod_page_view_page", "pageid" };
		case "url" -> new String[] { "mod_url_view_url", "urlid" };
		case "quiz" -> new String[] { "mod_quiz_view_quiz", "quizid" };
		default -> null;
		};
		if (ham == null) {
			return false;
		}
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add(ham[1], hoatDong.path("instance").asText());
		goiBangTokenHocVien(tokenHv, ham[0], f);
		return true;
	}

	/** Học viên tự đánh dấu (hoặc bỏ đánh dấu) đã học xong một hoạt động. */
	public void danhDauThuCong(String tokenHv, int cmid, boolean daXong) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("cmid", String.valueOf(cmid));
		f.add("completed", daXong ? "1" : "0");
		goiBangTokenHocVien(tokenHv, "core_completion_update_activity_completion_status_manually", f);
	}

	/** POST bằng token học viên — token không nằm trên URL để khỏi lọt vào log máy chủ web. */
	private JsonNode goiBangTokenHocVien(String tokenHv, String wsfunction, MultiValueMap<String, String> thamSo) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>(thamSo);
		f.add("wstoken", tokenHv);
		f.add("wsfunction", wsfunction);
		f.add("moodlewsrestformat", "json");
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		try {
			JsonNode node = objectMapper.readTree(
					restTemplate.postForObject(moodleConfig.getBaseUrl(), new HttpEntity<>(f, h), String.class));
			if (node.has("exception")) {
				throw new IllegalStateException(wsfunction + " -> " + node.path("errorcode").asText("") + ": "
						+ node.path("message").asText(""));
			}
			return node;
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(wsfunction + " -> không kết nối được LMS: " + e.getMessage());
		}
	}

	/**
	 * Phần trăm hoàn thành, hoặc null nếu khóa không bật theo dõi hoàn thành.
	 *
	 * Trả null chứ không trả 0: giao diện phân biệt được "chưa học gì" với "khóa
	 * này không đo được tiến độ", và ẩn hẳn thanh tiến độ ở trường hợp sau.
	 */
	public Integer phanTramHoanThanh(int moodleCourseId, int moodleUserId) {
		Map<Integer, Boolean> trangThai = trangThaiHoanThanh(moodleCourseId, moodleUserId);
		if (trangThai.isEmpty()) {
			return null;
		}
		long xong = trangThai.values().stream().filter(Boolean::booleanValue).count();
		return (int) Math.round(xong * 100.0 / trangThai.size());
	}

	/**
	 * Điểm cao nhất của người học ở từng bài kiểm tra trong khóa: cmid -> điểm.
	 *
	 * Moodle tách hai khái niệm: cmid là "hoạt động trong khóa", còn quiz id là
	 * "bài kiểm tra". Hàm điểm nhận quiz id, nên phải lấy danh sách quiz để bắc
	 * cầu giữa hai mã.
	 */
	public Map<Integer, String> diemBaiKiemTra(int moodleCourseId, int moodleUserId) {
		Map<Integer, String> ket = new HashMap<>();
		try {
			JsonNode ds = goi("mod_quiz_get_quizzes_by_courses", "&courseids[0]=" + moodleCourseId);
			for (JsonNode q : ds.path("quizzes")) {
				int quizId = q.path("id").asInt();
				int cmid = q.path("coursemodule").asInt();
				double diemToiDa = q.path("grade").asDouble(0);

				JsonNode g = goi("mod_quiz_get_user_best_grade", "&quizid=" + quizId + "&userid=" + moodleUserId);
				if (!g.path("hasgrade").asBoolean(false)) {
					continue;
				}
				String diem = boSoKhongThua(g.path("grade").asDouble(0));
				ket.put(cmid, diemToiDa > 0 ? diem + "/" + boSoKhongThua(diemToiDa) : diem);
			}
		} catch (Exception e) {
			System.err.println("[LmsTienDo] Không lấy được điểm khóa " + moodleCourseId + ": " + e.getMessage());
		}
		return ket;
	}

	/** 8.0 -> "8", 7.5 -> "7.5". Điểm tròn thì đừng hiện phần thập phân. */
	private String boSoKhongThua(double d) {
		return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	private JsonNode goi(String wsfunction, String thamSo) throws Exception {
		String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken() + "&wsfunction=" + wsfunction
				+ "&moodlewsrestformat=json" + thamSo;
		JsonNode node = objectMapper.readTree(restTemplate.getForObject(url, String.class));
		if (node.has("exception")) {
			throw new IllegalStateException(wsfunction + " -> " + node.path("errorcode").asText("") + ": "
					+ node.path("message").asText(""));
		}
		return node;
	}
}
