package com.bkap.CoursesModule.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
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
 * Điểm danh của học viên, lấy từ plugin Attendance bên LMS.
 *
 * Giáo viên vẫn điểm danh trên Moodle như cũ; buni chỉ ĐỌC lại để học viên xem
 * buổi nào mình có mặt, vắng, muộn, và tỷ lệ chuyên cần.
 *
 * Moodle trả danh sách buổi kèm điểm danh của CẢ LỚP (hàm dành cho giáo viên),
 * nên gọi bằng token dịch vụ rồi lọc đúng dòng của người đang xem ở đây — không
 * bao giờ gửi dữ liệu của người khác xuống trình duyệt.
 */
@Service
public class LmsDiemDanhService {

	@Autowired
	private MoodleConfig moodleConfig;

	/**
	 * Bấm điểm danh trong chừng này phút đầu buổi thì ghi "Có mặt", sau đó ghi
	 * "Đi muộn". API của plugin không cho biết trạng thái nào học viên được chọn
	 * vào lúc nào, nên buni tự đặt luật này.
	 */
	@org.springframework.beans.factory.annotation.Value("${app.diem-danh.phut-tinh-muon:15}")
	private int phutTinhMuon;

	/** Lỗi có câu chữ để hiện thẳng cho học viên. */
	public static class LoiDiemDanh extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public LoiDiemDanh(String thongDiep) {
			super(thongDiep);
		}
	}

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Bảng điểm danh của một học viên trong một hoạt động điểm danh.
	 *
	 * @param attendanceId  mã hoạt động điểm danh (instance, KHÔNG phải cmid)
	 * @param moodleUserId  người đang xem
	 */
	public Map<String, Object> cuaHocVien(int attendanceId, int moodleUserId) throws Exception {
		DsBuoi ds = layBuoi(attendanceId);
		JsonNode dsBuoi = ds.buoi();
		long bayGio = ds.gioMoodle();

		List<Map<String, Object>> buoi = new ArrayList<>();
		double tongDiem = 0;
		double tongToiDa = 0;
		Map<String, Integer> demTheoTrangThai = new LinkedHashMap<>();

		for (JsonNode s : dsBuoi) {
			// Buổi dành cho nhóm khác (lớp chia nhóm) thì học viên không có tên trong
			// danh sách — bỏ qua, không tính vào tỷ lệ của họ.
			if (!coTrongBuoi(s, moodleUserId)) {
				continue;
			}

			Map<String, JsonNode> trangThaiTheoId = new LinkedHashMap<>();
			double diemToiDa = 0;
			for (JsonNode st : s.path("statuses")) {
				if (st.path("deleted").asInt(0) == 1) {
					continue;
				}
				trangThaiTheoId.put(st.path("id").asText(), st);
				diemToiDa = Math.max(diemToiDa, st.path("grade").asDouble(0));
			}

			JsonNode ghi = null;
			for (JsonNode l : s.path("attendance_log")) {
				if (l.path("studentid").asInt() == moodleUserId) {
					ghi = l;
					break;
				}
			}

			Map<String, Object> b = new LinkedHashMap<>();
			b.put("id", s.path("id").asInt());
			b.put("batDau", s.path("sessdate").asLong());
			b.put("thoiLuongPhut", s.path("duration").asLong(0) / 60);
			b.put("moTa", chuThuan(s.path("description").asText("")));
			// Tự điểm danh: chỉ bật khi giáo viên cho phép, đang trong giờ mở, và
			// học viên chưa có trạng thái. Buổi kèm mã QR thì phải sang LMS nhập mật
			// khẩu / quét mã — buni không kiểm được mật khẩu nên không ghi hộ.
			b.put("coTheDiemDanh", ghi == null && dangMoTuDiemDanh(s, bayGio));
			b.put("canQr", s.path("includeqrcode").asInt(0) == 1);

			JsonNode trangThai = ghi == null ? null : trangThaiTheoId.get(ghi.path("statusid").asText());
			if (trangThai != null) {
				double diem = trangThai.path("grade").asDouble(0);
				String ten = tenTiengViet(chuThuan(trangThai.path("description").asText("")));
				Map<String, Object> t = new LinkedHashMap<>();
				t.put("kyHieu", trangThai.path("acronym").asText(""));
				t.put("ten", ten);
				t.put("diem", diem);
				t.put("diemToiDa", diemToiDa);
				b.put("trangThai", t);
				b.put("ghiChu", chuThuan(ghi.path("remarks").asText("")));

				tongDiem += diem;
				tongToiDa += diemToiDa;
				demTheoTrangThai.merge(ten, 1, Integer::sum);
			} else {
				// Chưa tới buổi, hoặc giáo viên chưa điểm danh buổi này.
				b.put("trangThai", null);
			}
			buoi.add(b);
		}

		buoi.sort(Comparator.comparingLong(x -> (Long) x.get("batDau")));

		Map<String, Object> ket = new LinkedHashMap<>();
		ket.put("buoi", buoi);
		ket.put("soBuoi", buoi.size());
		ket.put("soBuoiDaDiemDanh", buoi.stream().filter(x -> x.get("trangThai") != null).count());
		ket.put("demTheoTrangThai", demTheoTrangThai);
		// Tỷ lệ chuyên cần tính theo điểm của từng trạng thái, đúng cách Moodle tính:
		// có mặt đủ điểm, muộn được một phần, vắng 0 điểm.
		ket.put("tyLe", tongToiDa > 0 ? Math.round(tongDiem * 100 / tongToiDa) : null);
		return ket;
	}

	/**
	 * Học viên tự điểm danh một buổi ngay trên buni.
	 *
	 * Ghi bằng TOKEN CỦA HỌC VIÊN qua mod_attendance_update_user_status: Moodle tự
	 * kiểm buổi có đang cho tự điểm danh không, và chỉ cho ghi cho chính người đó.
	 * buni kiểm thêm trước khi gọi để báo lỗi rõ ràng, và để chặn buổi kèm mã QR —
	 * API không nhận mật khẩu, ghi hộ ở đây là bỏ qua bước quét mã.
	 */
	public Map<String, Object> tuDiemDanh(String tokenHv, int attendanceId, int sessionId, int moodleUserId)
			throws Exception {
		DsBuoi ds = layBuoi(attendanceId);
		long bayGio = ds.gioMoodle();
		JsonNode buoi = null;
		for (JsonNode s : ds.buoi()) {
			if (s.path("id").asInt() == sessionId) {
				buoi = s;
				break;
			}
		}
		if (buoi == null || !coTrongBuoi(buoi, moodleUserId)) {
			throw new LoiDiemDanh("Không tìm thấy buổi học này.");
		}
		for (JsonNode l : buoi.path("attendance_log")) {
			if (l.path("studentid").asInt() == moodleUserId) {
				throw new LoiDiemDanh("Bạn đã được điểm danh buổi này rồi.");
			}
		}
		if (buoi.path("includeqrcode").asInt(0) == 1) {
			throw new LoiDiemDanh("Buổi này cần quét mã QR. Vui lòng điểm danh trên hệ thống LMS.");
		}
		if (!dangMoTuDiemDanh(buoi, bayGio)) {
			throw new LoiDiemDanh("Buổi này hiện không mở cho học viên tự điểm danh.");
		}

		JsonNode trangThai = chonTrangThai(buoi, bayGio);
		if (trangThai == null) {
			throw new LoiDiemDanh("Buổi học chưa cài trạng thái điểm danh.");
		}

		MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
		f.add("sessionid", String.valueOf(sessionId));
		f.add("studentid", String.valueOf(moodleUserId));
		f.add("takenbyid", String.valueOf(moodleUserId));
		f.add("statusid", trangThai.path("id").asText());
		f.add("statusset", buoi.path("statusset").asText("0"));
		goiBangTokenHocVien(tokenHv, "mod_attendance_update_user_status", f);

		return cuaHocVien(attendanceId, moodleUserId);
	}

	/**
	 * Buổi đang mở cho tự điểm danh: giáo viên bật "Học viên tự điểm danh", và bây
	 * giờ nằm trong khoảng [giờ bắt đầu - thời gian mở sớm, giờ kết thúc] — đúng
	 * khoảng plugin cho phép.
	 */
	private boolean dangMoTuDiemDanh(JsonNode buoi, long bayGio) {
		if (buoi.path("studentscanmark").asInt(0) != 1) {
			return false;
		}
		long batDau = buoi.path("sessdate").asLong();
		long moSom = buoi.path("studentsearlyopentime").asLong(0);
		long ketThuc = batDau + buoi.path("duration").asLong(0);
		return bayGio >= batDau - moSom && bayGio <= ketThuc;
	}

	/**
	 * Trạng thái ghi cho học viên: trong {@link #phutTinhMuon} phút đầu là trạng
	 * thái điểm cao nhất (Có mặt); muộn hơn thì là "Late"/"Đi muộn" nếu buổi có.
	 */
	private JsonNode chonTrangThai(JsonNode buoi, long bayGio) {
		JsonNode caoNhat = null;
		JsonNode muon = null;
		for (JsonNode st : buoi.path("statuses")) {
			if (st.path("deleted").asInt(0) == 1 || st.path("visible").asInt(1) == 0) {
				continue;
			}
			if (caoNhat == null || st.path("grade").asDouble(0) > caoNhat.path("grade").asDouble(0)) {
				caoNhat = st;
			}
			String ten = st.path("description").asText("").toLowerCase();
			String kyHieu = st.path("acronym").asText("").toLowerCase();
			if (ten.contains("late") || ten.contains("muộn") || kyHieu.equals("l") || kyHieu.equals("m")) {
				muon = st;
			}
		}
		long daQuaPhut = (bayGio - buoi.path("sessdate").asLong()) / 60;
		return daQuaPhut > phutTinhMuon && muon != null ? muon : caoNhat;
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
			if (node != null && node.has("exception")) {
				System.err.println("[DiemDanh] " + wsfunction + " -> " + node.path("errorcode").asText("") + ": "
						+ node.path("message").asText(""));
				throw new LoiDiemDanh("Hệ thống LMS chưa ghi nhận điểm danh: " + node.path("message").asText(""));
			}
			return node;
		} catch (LoiDiemDanh e) {
			throw e;
		} catch (Exception e) {
			throw new LoiDiemDanh("Chưa kết nối được hệ thống LMS. Vui lòng thử lại.");
		}
	}

	/**
	 * Plugin điểm danh tạo sẵn 4 trạng thái bằng tiếng Anh. Đổi sang tiếng Việt
	 * cho học viên; trạng thái nào trung tâm đã tự đặt tên thì giữ nguyên.
	 */
	private static final Map<String, String> TEN_MAC_DINH = Map.of(
			"present", "Có mặt",
			"late", "Đi muộn",
			"excused", "Vắng có phép",
			"absent", "Vắng");

	private String tenTiengViet(String ten) {
		return TEN_MAC_DINH.getOrDefault(ten.trim().toLowerCase(), ten);
	}

	private boolean coTrongBuoi(JsonNode buoi, int moodleUserId) {
		JsonNode ds = buoi.path("users");
		if (!ds.isArray() || ds.size() == 0) {
			// Moodle không kèm danh sách thì coi như buổi chung của cả lớp.
			return true;
		}
		for (JsonNode u : ds) {
			if (u.path("id").asInt() == moodleUserId) {
				return true;
			}
		}
		return false;
	}

	private String chuThuan(String html) {
		return html == null ? ""
				: html.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
						.replaceAll("\\s+", " ").trim();
	}

	/** Danh sách buổi, kèm giờ hiện tại theo máy chủ Moodle (giây). */
	private record DsBuoi(JsonNode buoi, long gioMoodle) {
	}

	/**
	 * Lấy danh sách buổi và giờ của chính máy chủ Moodle (header Date của phản hồi).
	 *
	 * Mở/đóng điểm danh phải tính theo đồng hồ Moodle, vì Moodle mới là bên quyết
	 * định cho ghi hay không. Đo thực tế máy chạy buni lệch Moodle hơn 7 phút —
	 * dùng giờ máy mình thì nút hiện ra mà bấm bị từ chối, hoặc ngược lại.
	 */
	private DsBuoi layBuoi(int attendanceId) throws Exception {
		String url = moodleConfig.getBaseUrl() + "?wstoken=" + moodleConfig.getToken()
				+ "&wsfunction=mod_attendance_get_sessions&moodlewsrestformat=json&attendanceid=" + attendanceId;
		org.springframework.http.ResponseEntity<String> phanHoi = restTemplate.getForEntity(url, String.class);
		JsonNode node = objectMapper.readTree(phanHoi.getBody());
		if (node.has("exception")) {
			throw new IllegalStateException("mod_attendance_get_sessions -> " + node.path("errorcode").asText("")
					+ ": " + node.path("message").asText(""));
		}
		long ngay = phanHoi.getHeaders().getDate();
		return new DsBuoi(node, ngay > 0 ? ngay / 1000 : System.currentTimeMillis() / 1000);
	}

}
