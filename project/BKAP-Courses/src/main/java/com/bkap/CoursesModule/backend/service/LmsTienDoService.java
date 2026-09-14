package com.bkap.CoursesModule.backend.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

	@Autowired
	private MoodleConfig moodleConfig;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Trạng thái hoàn thành từng hoạt động: cmid -> đã xong chưa.
	 *
	 * Chỉ tính những hoạt động có bật theo dõi hoàn thành bên Moodle. Khóa nào
	 * giáo viên chưa bật theo dõi thì trả về rỗng — và như vậy là đúng, thà không
	 * hiện tiến độ còn hơn hiện 0% cho người đã học xong.
	 */
	public Map<Integer, Boolean> trangThaiHoanThanh(int moodleCourseId, int moodleUserId) {
		Map<Integer, Boolean> ket = new LinkedHashMap<>();
		try {
			JsonNode r = goi("core_completion_get_activities_completion_status",
					"&courseid=" + moodleCourseId + "&userid=" + moodleUserId);
			for (JsonNode s : r.path("statuses")) {
				if (!s.path("hascompletion").asBoolean(false)) {
					continue;
				}
				ket.put(s.path("cmid").asInt(), s.path("state").asInt(CHUA_HOAN_THANH) != CHUA_HOAN_THANH);
			}
		} catch (Exception e) {
			// Hay gặp nhất: vai trò tài khoản dịch vụ thiếu quyền report/progress:view.
			System.err.println("[LmsTienDo] Không lấy được tiến độ khóa " + moodleCourseId + ": " + e.getMessage());
		}
		return ket;
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
