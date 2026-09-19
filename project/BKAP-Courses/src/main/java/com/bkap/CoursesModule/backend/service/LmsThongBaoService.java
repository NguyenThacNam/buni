package com.bkap.CoursesModule.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Thông báo của học viên, lấy từ chuông thông báo bên LMS.
 *
 * Tất cả gọi bằng TOKEN CỦA HỌC VIÊN: Moodle tự giới hạn chỉ trả thông báo của
 * chính người cầm token, nên không có cách nào xem trộm của người khác.
 */
@Service
public class LmsThongBaoService {

	/**
	 * Loại thông báo bỏ qua.
	 *
	 * "newlogin" là thông báo "Một lượt đăng nhập mới vào tài khoản của bạn" —
	 * chính buni sinh ra nó: mỗi lần học viên đăng nhập buni, buni đăng nhập hộ
	 * sang Moodle để lấy token. Hiện lại cho học viên thì vừa nhiễu vừa khó hiểu.
	 */
	private static final Set<String> BO_QUA = Set.of("newlogin");

	/** Số thông báo quét về mỗi lần, đủ để đếm chưa đọc mà không nặng. */
	private static final int SO_LUONG_QUET = 50;

	@Autowired
	private MoodleConfig moodleConfig;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Danh sách thông báo mới nhất.
	 *
	 * @param soLuong số thông báo lấy về (đã trừ phần bỏ qua nên xin dư một ít)
	 */
	public Map<String, Object> danhSach(String tokenHv, int moodleUserId, int soLuong) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("useridto", String.valueOf(moodleUserId));
		f.add("newestfirst", "1");
		f.add("limit", String.valueOf(SO_LUONG_QUET));
		JsonNode r = goi(tokenHv, "message_popup_get_popup_notifications", f);

		List<Map<String, Object>> ds = new ArrayList<>();
		int chuaDoc = 0;
		for (JsonNode n : r.path("notifications")) {
			if (BO_QUA.contains(n.path("eventtype").asText(""))) {
				continue;
			}
			if (!n.path("read").asBoolean(false)) {
				chuaDoc++;
			}
			if (ds.size() >= soLuong) {
				continue;
			}
			Map<String, Object> t = new LinkedHashMap<>();
			t.put("id", n.path("id").asInt());
			t.put("tieuDe", chuThuan(n.path("subject").asText("")));
			t.put("tomTat", chuThuan(n.path("smallmessage").asText("")));
			t.put("thoiGian", n.path("timecreated").asLong());
			t.put("daDoc", n.path("read").asBoolean(false));
			// Đường dẫn tới chỗ liên quan bên LMS (bài kiểm tra, bài viết diễn đàn...).
			// Có thể rỗng — thông báo chung chung thì Moodle không kèm link.
			t.put("duongDanLms", giaiMaUrl(n.path("contexturl").asText("")));
			t.put("tenDuongDan", chuThuan(n.path("contexturlname").asText("")));
			ds.add(t);
		}

		Map<String, Object> ket = new LinkedHashMap<>();
		ket.put("thongBao", ds);
		// Đếm trên danh sách ĐÃ LỌC, không hỏi Moodle: Moodle đếm cả loại buni bỏ
		// qua, nên con số đỏ sẽ to hơn số dòng người học thấy.
		ket.put("chuaDoc", chuaDoc);
		return ket;
	}

	/**
	 * Số thông báo chưa đọc, cho con số đỏ trên chuông.
	 *
	 * Đếm lại trên danh sách đã lọc thay vì dùng
	 * message_popup_get_unread_popup_notification_count: hàm đó đếm cả loại buni
	 * bỏ qua (đăng nhập mới), nên chuông báo 13 mà mở ra chỉ có 7 dòng.
	 */
	public int demChuaDoc(String tokenHv, int moodleUserId) {
		return (int) danhSach(tokenHv, moodleUserId, 0).get("chuaDoc");
	}

	/**
	 * Đánh dấu một thông báo đã đọc. Không làm bước này thì học viên đọc trên buni
	 * xong, sang LMS vẫn thấy báo chưa đọc.
	 */
	public void danhDauDaDoc(String tokenHv, int thongBaoId) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("notificationid", String.valueOf(thongBaoId));
		goi(tokenHv, "core_message_mark_notification_read", f);
	}

	/**
	 * Các việc sắp đến hạn: bài kiểm tra sắp đóng, bài tập sắp hết hạn, buổi học
	 * sắp tới — chính là danh sách "Sắp đến hạn" trên trang chủ Moodle.
	 */
	public List<Map<String, Object>> sapDenHan(String tokenHv, int soLuong) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("timesortfrom", String.valueOf(System.currentTimeMillis() / 1000));
		f.add("limitnum", String.valueOf(soLuong));
		JsonNode r = goi(tokenHv, "core_calendar_get_action_events_by_timesort", f);

		List<Map<String, Object>> ds = new ArrayList<>();
		for (JsonNode e : r.path("events")) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", e.path("id").asInt());
			m.put("ten", chuThuan(e.path("name").asText("")));
			m.put("hanChot", e.path("timesort").asLong());
			m.put("loai", e.path("modulename").asText(""));
			m.put("khoaId", e.path("course").path("id").asInt());
			m.put("tenKhoa", chuThuan(e.path("course").path("fullname").asText("")));
			m.put("cmid", e.path("instance").asInt());
			ds.add(m);
		}
		return ds;
	}

	/**
	 * Moodle trả đường dẫn đã mã hóa HTML ("&amp;" thay cho "&"). Để nguyên thì
	 * link mở ra thiếu tham số, vào nhầm trang.
	 */
	private String giaiMaUrl(String url) {
		return url == null ? "" : url.replace("&amp;", "&").replace("&#038;", "&").trim();
	}

	private String chuThuan(String html) {
		return html == null ? ""
				: html.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
						.replace("&quot;", "\"").replace("&#039;", "'").replaceAll("\\s+", " ").trim();
	}

	/** POST bằng token học viên — token không nằm trên URL để khỏi lọt vào log máy chủ web. */
	private JsonNode goi(String tokenHv, String wsfunction, MultiValueMap<String, String> thamSo) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>(thamSo);
		f.add("wstoken", tokenHv);
		f.add("wsfunction", wsfunction);
		f.add("moodlewsrestformat", "json");
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		try {
			JsonNode node = objectMapper.readTree(
					restTemplate.postForObject(moodleConfig.getBaseUrl(), new HttpEntity<>(f, h), String.class));
			if (node != null && node.has("exception")) {
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
}
