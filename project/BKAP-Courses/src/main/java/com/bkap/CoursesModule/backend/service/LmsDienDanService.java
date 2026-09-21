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
 * Diễn đàn của khóa học — chủ yếu là mục "Các thông báo" giáo viên nhắn cả lớp.
 *
 * buni cho ĐỌC ngay tại chỗ; viết bài và trả lời vẫn để bên LMS, vì đó là việc
 * hiếm và Moodle đã có sẵn trình soạn thảo, đính kèm, theo dõi bài.
 *
 * Gọi bằng TOKEN CỦA HỌC VIÊN: Moodle tự kiểm người này có quyền đọc diễn đàn
 * đó không, buni không phải tự dựng lại luật.
 */
@Service
public class LmsDienDanService {

	@Autowired
	private MoodleConfig moodleConfig;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Các bài đăng trong một diễn đàn, mới nhất trước.
	 *
	 * @param cmid mã hoạt động diễn đàn trong khóa
	 */
	public List<Map<String, Object>> baiDang(String tokenHv, int moodleCourseId, int cmid) {
		Integer forumId = timForumId(tokenHv, moodleCourseId, cmid);
		if (forumId == null) {
			return null;
		}

		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("forumid", String.valueOf(forumId));
		f.add("sortorder", "1"); // 1 = mới nhất trước
		JsonNode r = goi(tokenHv, "mod_forum_get_forum_discussions", f);

		List<Map<String, Object>> ds = new ArrayList<>();
		for (JsonNode d : r.path("discussions")) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", d.path("discussion").asInt());
			m.put("tieuDe", chuThuan(d.path("name").asText("")));
			m.put("nguoiDang", chuThuan(d.path("userfullname").asText("")));
			m.put("thoiGian", d.path("created").asLong());
			m.put("soTraLoi", d.path("numreplies").asInt(0));
			// Nội dung bài mở đầu có sẵn ở đây, khỏi phải gọi thêm để xem bài ngắn.
			m.put("noiDung", d.path("message").asText(""));
			ds.add(m);
		}
		return ds;
	}

	/** Toàn bộ bài trong một chủ đề, gồm bài mở đầu và các trả lời. */
	public List<Map<String, Object>> traLoi(String tokenHv, int discussionId) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("discussionid", String.valueOf(discussionId));
		f.add("sortdirection", "ASC");
		JsonNode r = goi(tokenHv, "mod_forum_get_discussion_posts", f);

		List<Map<String, Object>> ds = new ArrayList<>();
		for (JsonNode p : r.path("posts")) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", p.path("id").asInt());
			m.put("tieuDe", chuThuan(p.path("subject").asText("")));
			m.put("noiDung", p.path("message").asText(""));
			m.put("nguoiDang", chuThuan(p.path("author").path("fullname").asText("")));
			m.put("thoiGian", p.path("timecreated").asLong());
			m.put("laTraLoi", !p.path("parentid").isNull() && p.path("parentid").asInt(0) > 0);
			ds.add(m);
		}
		return ds;
	}

	/**
	 * Đổi mã hoạt động (cmid) sang mã diễn đàn — Moodle dùng hai mã khác nhau cho
	 * cùng một thứ, và hàm lấy bài đăng chỉ nhận mã diễn đàn.
	 */
	private Integer timForumId(String tokenHv, int moodleCourseId, int cmid) {
		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("courseids[0]", String.valueOf(moodleCourseId));
		JsonNode r = goi(tokenHv, "mod_forum_get_forums_by_courses", f);
		for (JsonNode d : r) {
			if (d.path("cmid").asInt() == cmid) {
				return d.path("id").asInt();
			}
		}
		return null;
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
