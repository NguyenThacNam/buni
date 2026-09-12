package com.bkap.UserModule.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.bkap.CoursesModule.backend.service.ICourseService;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.UserModule.backend.service.NhapTaiKhoanService;
import com.bkap.UserModule.backend.service.NhapTaiKhoanService.DongKetQua;
import com.bkap.UserModule.backend.service.NhapTaiKhoanService.TrangThai;

import jakarta.servlet.http.HttpSession;

/**
 * Nhập tài khoản học viên hàng loạt từ Excel — /admin/user/import.
 *
 * Nằm dưới /admin/** nên tự động được chuỗi bảo mật admin che: phải đăng nhập,
 * phải là ADMIN, và mọi form POST đều có CSRF.
 */
@Controller
@RequestMapping("/admin/user/import")
public class AdminUserImportController {

	/** Khóa lưu kết quả lần nhập gần nhất trong session, để tải danh sách tài khoản. */
	private static final String KET_QUA_SESSION = "nhapTaiKhoan.ketQua";

	private static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	@Autowired
	private NhapTaiKhoanService nhapTaiKhoanService;

	@Autowired
	private ICourseService courseService;

	@GetMapping
	public String form(Model model) {
		model.addAttribute("courses", courseService.getAllCourses());
		return "admin/user/import";
	}

	@GetMapping("/mau.xlsx")
	public ResponseEntity<byte[]> fileMau() throws Exception {
		return taiVe(nhapTaiKhoanService.taoFileMau(), "mau-nhap-hoc-vien.xlsx");
	}

	@PostMapping
	public String nhap(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "courseId", required = false) Short courseId, Model model, HttpSession session) {
		model.addAttribute("courses", courseService.getAllCourses());
		model.addAttribute("courseIdDaChon", courseId);

		if (file == null || file.isEmpty()) {
			model.addAttribute("flashErr", "Chưa chọn file Excel.");
			return "admin/user/import";
		}

		Course khoa = null;
		if (courseId != null) {
			khoa = courseService.getCourseById(courseId);
			if (khoa == null) {
				model.addAttribute("flashErr", "Không tìm thấy khóa học đã chọn.");
				return "admin/user/import";
			}
		}

		List<DongKetQua> ketQua;
		try {
			ketQua = nhapTaiKhoanService.nhap(file.getInputStream(), khoa);
		} catch (IllegalArgumentException e) {
			model.addAttribute("flashErr", e.getMessage());
			return "admin/user/import";
		} catch (Exception e) {
			System.err.println("[NhapTaiKhoan] Lỗi: " + e.getMessage());
			model.addAttribute("flashErr", "Không xử lý được file: " + e.getMessage());
			return "admin/user/import";
		}

		long soTao = ketQua.stream().filter(k -> k.trangThai == TrangThai.DA_TAO).count();
		long soCo = ketQua.stream().filter(k -> k.trangThai == TrangThai.DA_CO).count();
		long soLoi = ketQua.stream().filter(k -> k.trangThai == TrangThai.LOI).count();

		// Mật khẩu tự sinh chỉ nằm trong session này, KHÔNG lưu vào CSDL (CSDL chỉ
		// giữ bản băm). Hết phiên là mất — admin cần tải danh sách về ngay.
		session.setAttribute(KET_QUA_SESSION, ketQua);

		model.addAttribute("ketQua", ketQua);
		model.addAttribute("soTao", soTao);
		model.addAttribute("soCo", soCo);
		model.addAttribute("soLoi", soLoi);
		model.addAttribute("khoa", khoa);

		String tomTat = "Đã xử lý " + ketQua.size() + " dòng: tạo mới " + soTao + ", đã có sẵn " + soCo + ", lỗi "
				+ soLoi + ".";
		model.addAttribute(soLoi > 0 ? "flashWarn" : "flashOk", tomTat);
		return "admin/user/import";
	}

	@GetMapping("/tai-khoan.xlsx")
	@SuppressWarnings("unchecked")
	public ResponseEntity<byte[]> taiDanhSach(HttpSession session) throws Exception {
		Object luu = session.getAttribute(KET_QUA_SESSION);
		if (!(luu instanceof List<?>)) {
			return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN)
					.body("Không còn kết quả nhập nào trong phiên này. Hãy nhập lại file."
							.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
		return taiVe(nhapTaiKhoanService.xuatTaiKhoan((List<DongKetQua>) luu), "tai-khoan-hoc-vien.xlsx");
	}

	private ResponseEntity<byte[]> taiVe(byte[] noiDung, String tenFile) {
		return ResponseEntity.ok().contentType(XLSX)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tenFile + "\"")
				// Danh sách có mật khẩu: không cho trình duyệt hay proxy giữ bản sao.
				.header(HttpHeaders.CACHE_CONTROL, "no-store").body(noiDung);
	}
}
