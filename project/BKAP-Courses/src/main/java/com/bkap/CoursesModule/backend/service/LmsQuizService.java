package com.bkap.CoursesModule.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Làm bài kiểm tra ngay trên buni, chấm điểm bên LMS.
 *
 * buni chỉ là giao diện. Bắt đầu lượt, lưu nháp, nộp bài, chấm điểm, đếm số lần
 * làm, khóa khi hết giờ — tất cả do Moodle quyết qua bộ API làm bài (chính bộ mà
 * app Moodle trên điện thoại dùng). Đáp án đúng cũng nằm bên Moodle, buni không
 * bao giờ biết trước, nên không mở ra đường gian lận.
 *
 * MỌI lời gọi ở đây dùng TOKEN CỦA HỌC VIÊN, không phải token dịch vụ: các hàm
 * làm bài ghi lượt làm dưới tên chủ token. Moodle cũng tự kiểm tra lượt làm có
 * đúng của người đó không (notyourattempt), học viên có được ghi danh không.
 */
@Service
public class LmsQuizService {

	@Autowired
	private MoodleConfig moodleConfig;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/** Moodle báo lỗi có câu chữ dành cho người dùng — đưa nguyên câu đó lên giao diện. */
	public static class LoiLms extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public final String maLoi;

		public LoiLms(String maLoi, String thongDiep) {
			super(thongDiep);
			this.maLoi = maLoi;
		}
	}

	// ─────────────────────────────────────────────────────────────
	// THÔNG TIN BÀI
	// ─────────────────────────────────────────────────────────────

	/**
	 * Thông tin một bài kiểm tra theo cmid: cài đặt, còn làm được không, các lượt
	 * đã làm, điểm cao nhất, lượt đang dở (nếu có).
	 *
	 * @return null nếu cmid không phải bài kiểm tra của khóa này
	 */
	public Map<String, Object> thongTinBai(String tokenHv, int courseId, int cmid) {
		JsonNode bai = null;
		for (JsonNode q : goi(tokenHv, "mod_quiz_get_quizzes_by_courses", thamSo("courseids[0]", courseId))
				.path("quizzes")) {
			if (q.path("coursemodule").asInt() == cmid) {
				bai = q;
				break;
			}
		}
		if (bai == null) {
			return null;
		}
		int quizId = bai.path("id").asInt();

		JsonNode quyen = goi(tokenHv, "mod_quiz_get_quiz_access_information", thamSo("quizid", quizId));
		JsonNode luot = goi(tokenHv, "mod_quiz_get_user_quiz_attempts",
				thamSo("quizid", quizId, "status", "all", "includepreviews", 1));
		JsonNode diem = goi(tokenHv, "mod_quiz_get_user_best_grade", thamSo("quizid", quizId));

		List<Map<String, Object>> dsLuot = new ArrayList<>();
		Integer luotDangDo = null;
		for (JsonNode a : luot.path("attempts")) {
			String trangThai = a.path("state").asText("");
			if ("inprogress".equals(trangThai) || "overdue".equals(trangThai)) {
				luotDangDo = a.path("id").asInt();
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", a.path("id").asInt());
			m.put("lanThu", a.path("attempt").asInt());
			m.put("trangThai", trangThai);
			m.put("batDau", a.path("timestart").asLong());
			m.put("ketThuc", a.path("timefinish").asLong());
			m.put("diem", a.path("sumgrades").isNull() ? null : a.path("sumgrades").asDouble());
			m.put("xemTruoc", a.path("preview").asInt() == 1);
			dsLuot.add(m);
		}

		List<String> lyDoChan = new ArrayList<>();
		for (JsonNode r : quyen.path("preventaccessreasons")) {
			lyDoChan.add(r.asText());
		}
		List<String> quyTac = new ArrayList<>();
		for (JsonNode r : quyen.path("accessrules")) {
			quyTac.add(r.asText());
		}

		Map<String, Object> kq = new LinkedHashMap<>();
		kq.put("quizId", quizId);
		kq.put("cmid", cmid);
		kq.put("ten", bai.path("name").asText(""));
		kq.put("gioiThieu", bai.path("intro").asText(""));
		kq.put("thoiGianGiay", bai.path("timelimit").asInt(0));
		kq.put("soLanToiDa", bai.path("attempts").asInt(0)); // 0 = không giới hạn
		kq.put("diemToiDa", bai.path("grade").asDouble(0));
		kq.put("tongDiemCauHoi", bai.path("sumgrades").asDouble(0));
		kq.put("duocLam", quyen.path("canattempt").asBoolean(false) || quyen.path("canpreview").asBoolean(false));
		kq.put("xemLaiDuoc", quyen.path("canreviewmyattempts").asBoolean(false));
		kq.put("lyDoChan", lyDoChan);
		kq.put("quyTac", quyTac);
		kq.put("cacLuot", dsLuot);
		kq.put("luotDangDo", luotDangDo);
		kq.put("diemCaoNhat", diem.path("hasgrade").asBoolean(false) ? diem.path("grade").asDouble() : null);
		kq.put("diemDat", diem.path("gradetopass").isMissingNode() ? null : diem.path("gradetopass").asDouble());
		return kq;
	}

	// ─────────────────────────────────────────────────────────────
	// LÀM BÀI
	// ─────────────────────────────────────────────────────────────

	/**
	 * Bắt đầu một lượt mới, hoặc trả lại lượt đang dở — Moodle không cho mở hai
	 * lượt song song, gọi start khi còn lượt dở sẽ bị từ chối.
	 */
	public int batDau(String tokenHv, int courseId, int cmid) {
		Map<String, Object> bai = thongTinBai(tokenHv, courseId, cmid);
		if (bai == null) {
			throw new LoiLms("khongtimthay", "Không tìm thấy bài kiểm tra này trong khóa học");
		}
		if (bai.get("luotDangDo") != null) {
			return (Integer) bai.get("luotDangDo");
		}
		JsonNode r = goi(tokenHv, "mod_quiz_start_attempt", thamSo("quizid", bai.get("quizId")));
		return r.path("attempt").path("id").asInt();
	}

	/**
	 * Toàn bộ câu hỏi của một lượt, gộp mọi trang.
	 *
	 * HTML câu hỏi do Moodle dựng sẵn — giữ nguyên, giao diện buni tự bóc phần đề
	 * bài và các lựa chọn ra để vẽ lại. Kèm theo mỗi câu là sequencecheck: Moodle
	 * bắt gửi lại đúng số này khi lưu, để phát hiện hai tab cùng sửa một câu.
	 */
	public Map<String, Object> cauHoi(String tokenHv, int attemptId) {
		List<Map<String, Object>> cacCau = new ArrayList<>();
		JsonNode luot = null;
		int trang = 0;
		for (int vong = 0; vong < 200 && trang >= 0; vong++) {
			JsonNode r = goi(tokenHv, "mod_quiz_get_attempt_data", thamSo("attemptid", attemptId, "page", trang));
			luot = r.path("attempt");
			for (JsonNode q : r.path("questions")) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("slot", q.path("slot").asInt());
				m.put("soThuTu", q.path("number").asText(""));
				m.put("loai", q.path("type").asText(""));
				m.put("trang", q.path("page").asInt());
				m.put("html", q.path("html").asText(""));
				m.put("sequencecheck", q.path("sequencecheck").asInt());
				m.put("trangThai", q.path("status").asText(""));
				m.put("danhDau", q.path("flagged").asBoolean(false));
				m.put("diemToiDa", q.path("maxmark").asDouble(0));
				cacCau.add(m);
			}
			trang = r.path("nextpage").asInt(-1);
		}

		// Ghi nhận "đã xem lượt làm" cho báo cáo của giáo viên bên LMS. Hỏng thì
		// thôi, không đáng làm hỏng việc làm bài.
		try {
			goi(tokenHv, "mod_quiz_view_attempt", thamSo("attemptid", attemptId, "page", 0));
		} catch (Exception ignored) {
		}

		long[] gioMoodle = new long[1];
		JsonNode truyCap = goi(tokenHv, "mod_quiz_get_attempt_access_information",
				thamSo("quizid", luot.path("quiz").asInt(), "attemptid", attemptId), gioMoodle);

		Map<String, Object> kq = new LinkedHashMap<>();
		kq.put("attemptId", attemptId);
		kq.put("quizId", luot.path("quiz").asInt());
		kq.put("trangThai", luot.path("state").asText(""));
		kq.put("batDau", luot.path("timestart").asLong());
		// endtime = mốc Moodle sẽ khóa bài. 0 nghĩa là không giới hạn thời gian.
		kq.put("hetGioLuc", truyCap.path("endtime").asLong(0));
		// Giờ hiện tại THEO MÁY CHỦ MOODLE, lấy từ header Date của chính phản hồi.
		// Không dùng giờ máy chạy buni: đo thực tế hai máy lệch nhau hơn 7 phút, bài
		// 20 phút vừa mở đã báo còn 27 phút. Giao diện tính giờ còn lại bằng hiệu
		// hetGioLuc - gioMayChu (cùng một đồng hồ), rồi tự đếm lùi từ đó.
		kq.put("gioMayChu", gioMoodle[0] > 0 ? gioMoodle[0] : System.currentTimeMillis() / 1000);
		kq.put("cauHoi", cacCau);
		return kq;
	}

	/**
	 * Lưu nháp các câu đã chọn, chưa nộp.
	 *
	 * @return sequencecheck hiện tại của từng câu (slot → số). Lúc nộp mà gửi số
	 *         đã cũ, Moodle coi là "bài bị sửa ở nơi khác" và từ chối cả lượt gửi.
	 *         Đo thực tế thì lưu nháp (autosave) không làm tăng số này — vẫn trả
	 *         về để giao diện luôn cầm số đúng, phòng khi bài làm được mở ở tab khác.
	 */
	public Map<Integer, Integer> luuNhap(String tokenHv, int attemptId, List<Map<String, String>> duLieu) {
		MultiValueMap<String, String> f = thamSo("attemptid", attemptId);
		ganDuLieu(f, duLieu);
		goi(tokenHv, "mod_quiz_save_attempt", f);

		Map<Integer, Integer> moi = new LinkedHashMap<>();
		int trang = 0;
		for (int vong = 0; vong < 200 && trang >= 0; vong++) {
			JsonNode r = goi(tokenHv, "mod_quiz_get_attempt_data", thamSo("attemptid", attemptId, "page", trang));
			for (JsonNode q : r.path("questions")) {
				moi.put(q.path("slot").asInt(), q.path("sequencecheck").asInt());
			}
			trang = r.path("nextpage").asInt(-1);
		}
		return moi;
	}

	/**
	 * Nộp bài. Moodle chấm ngay; hết giờ thì Moodle tự khóa phía máy chủ nên buni
	 * không phải tin đồng hồ của trình duyệt.
	 *
	 * @param hetGio true khi giao diện tự nộp vì đồng hồ về 0
	 */
	public String nopBai(String tokenHv, int attemptId, List<Map<String, String>> duLieu, boolean hetGio) {
		MultiValueMap<String, String> f = thamSo("attemptid", attemptId, "finishattempt", 1, "timeup", hetGio ? 1 : 0);
		ganDuLieu(f, duLieu);
		return goi(tokenHv, "mod_quiz_process_attempt", f).path("state").asText("");
	}

	/**
	 * Kết quả một lượt đã nộp. Moodle chỉ trả điểm và đáp án đúng trong phạm vi
	 * cài đặt "xem lại" của bài — buni hiện đúng những gì nhận được.
	 */
	public Map<String, Object> ketQua(String tokenHv, int attemptId) {
		JsonNode r = goi(tokenHv, "mod_quiz_get_attempt_review", thamSo("attemptid", attemptId, "page", -1));

		List<Map<String, Object>> cacCau = new ArrayList<>();
		for (JsonNode q : r.path("questions")) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("slot", q.path("slot").asInt());
			m.put("soThuTu", q.path("number").asText(""));
			m.put("html", q.path("html").asText(""));
			m.put("trangThai", q.path("state").asText(""));
			m.put("diem", q.path("mark").asText(""));
			m.put("diemToiDa", q.path("maxmark").asDouble(0));
			cacCau.add(m);
		}
		List<Map<String, String>> tomTat = new ArrayList<>();
		for (JsonNode d : r.path("additionaldata")) {
			tomTat.add(Map.of("ma", d.path("id").asText(""), "tieuDe", d.path("title").asText(""),
					"noiDung", d.path("content").asText("")));
		}

		Map<String, Object> kq = new LinkedHashMap<>();
		kq.put("attemptId", attemptId);
		kq.put("quizId", r.path("attempt").path("quiz").asInt());
		// Moodle trả điểm thô kiểu "0.6666666666666666" — làm tròn 2 chữ số cho người xem.
		String diemTho = r.path("grade").asText("");
		try {
			double d = Double.parseDouble(diemTho);
			diemTho = d == Math.rint(d) ? String.valueOf((long) d) : String.format(java.util.Locale.US, "%.2f", d);
		} catch (NumberFormatException ignored) {
		}
		kq.put("diem", diemTho);
		kq.put("trangThai", r.path("attempt").path("state").asText(""));
		kq.put("tomTat", tomTat);
		kq.put("cauHoi", cacCau);
		return kq;
	}

	// ─────────────────────────────────────────────────────────────
	// TIỆN ÍCH
	// ─────────────────────────────────────────────────────────────

	/**
	 * Chỉ nhận các trường thuộc câu hỏi (qXX:YY_...) và danh sách slot. Chặn sẵn
	 * để trình duyệt không nhét được tham số lạ vào lời gọi Moodle.
	 */
	private void ganDuLieu(MultiValueMap<String, String> f, List<Map<String, String>> duLieu) {
		if (duLieu == null) {
			return;
		}
		int i = 0;
		for (Map<String, String> muc : duLieu) {
			String ten = muc.get("name");
			if (ten == null || !(ten.matches("^q\\d+:\\d+_[\\w:\\-]+$") || "slots".equals(ten))) {
				continue;
			}
			f.add("data[" + i + "][name]", ten);
			f.add("data[" + i + "][value]", muc.getOrDefault("value", ""));
			i++;
		}
	}

	private MultiValueMap<String, String> thamSo(Object... cap) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		for (int i = 0; i + 1 < cap.length; i += 2) {
			f.add(String.valueOf(cap[i]), String.valueOf(cap[i + 1]));
		}
		return f;
	}

	private JsonNode goi(String tokenHv, String wsfunction, MultiValueMap<String, String> thamSo) {
		return goi(tokenHv, wsfunction, thamSo, null);
	}

	/**
	 * Gửi POST — token học viên không được nằm trên URL (lọt vào log máy chủ web).
	 *
	 * @param gioMoodle nếu khác null, ô [0] nhận giờ hiện tại của máy chủ Moodle (giây)
	 */
	private JsonNode goi(String tokenHv, String wsfunction, MultiValueMap<String, String> thamSo, long[] gioMoodle) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>(thamSo);
		f.add("wstoken", tokenHv);
		f.add("wsfunction", wsfunction);
		f.add("moodlewsrestformat", "json");

		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		try {
			org.springframework.http.ResponseEntity<String> phanHoi = restTemplate
					.postForEntity(moodleConfig.getBaseUrl(), new HttpEntity<>(f, h), String.class);
			if (gioMoodle != null && phanHoi.getHeaders().getDate() > 0) {
				gioMoodle[0] = phanHoi.getHeaders().getDate() / 1000;
			}
			JsonNode node = objectMapper.readTree(phanHoi.getBody());
			if (node.has("exception")) {
				String ma = node.path("errorcode").asText("");
				System.err.println("[LmsQuiz] " + wsfunction + " -> " + ma + ": " + node.path("message").asText(""));
				throw new LoiLms(ma, node.path("message").asText("Hệ thống LMS từ chối yêu cầu"));
			}
			return node;
		} catch (LoiLms e) {
			throw e;
		} catch (Exception e) {
			throw new LoiLms("ketnoi", "Chưa kết nối được hệ thống LMS: " + e.getMessage());
		}
	}
}
