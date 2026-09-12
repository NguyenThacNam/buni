package com.bkap.CoursesModule.backend.controller;

import java.beans.PropertyEditorSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bkap.CoursesModule.backend.service.ICourseService;
import com.bkap.CoursesModule.backend.service.IEnrollmentService;
import com.bkap.CoursesModule.backend.service.LmsSsoService;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.CoursesModule.entity.Enrollment;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.entity.User;

/**
 * Quản lý ghi danh — ai đang học khóa nào, tiến độ tới đâu.
 *
 * Bảng enrollment có sẵn trong CSDL từ đầu nhưng chưa từng có entity lẫn giao
 * diện, nên trang "Khóa học của tôi" bên frontend vẫn đang chạy dữ liệu giả.
 */
@Controller
@RequestMapping("/admin/enrollment")
public class AdminEnrollmentController {

	@Autowired
	private IEnrollmentService enrollmentService;

	@Autowired
	private ICourseService courseService;

	@Autowired
	private IUserService userService;

	@Autowired
	private LmsSsoService lmsSsoService;

	/**
	 * Select trên form gửi lên id dạng chuỗi, còn entity cần cả object User /
	 * Course. Hai editor này lo phần chuyển đổi đó.
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(User.class, "user", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				if (text == null || text.isEmpty()) {
					setValue(null);
					return;
				}
				setValue(userService.findById(Short.parseShort(text)));
			}
		});

		binder.registerCustomEditor(Course.class, "course", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				if (text == null || text.isEmpty()) {
					setValue(null);
					return;
				}
				setValue(courseService.getCourseById(Short.parseShort(text)));
			}
		});
	}

	/** Số dòng mỗi trang trong bảng danh sách. */
	private static final int SO_DONG_MOI_TRANG = 10;

	@GetMapping
	public String list(@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "page", defaultValue = "0") int page, Model model) {

		// Sắp xếp id giảm dần: lượt ghi danh mới nhất lên đầu, hợp với thói quen
		// theo dõi ai vừa đăng ký.
		Page<Enrollment> trang = enrollmentService.timKiem(q,
				PageRequest.of(Math.max(page, 0), SO_DONG_MOI_TRANG, Sort.by(Sort.Direction.DESC, "id")));

		model.addAttribute("enrollments", trang.getContent());
		model.addAttribute("trang", trang);
		model.addAttribute("q", q);
		return "admin/enrollment/list";
	}

	@GetMapping("/create")
	public String create(Model model) {
		model.addAttribute("enrollment", new Enrollment());
		napDuLieuChonLua(model);
		return "admin/enrollment/form";
	}

	@GetMapping("/edit/{id}")
	public String edit(@PathVariable Integer id, Model model, RedirectAttributes ra) {
		Enrollment enrollment = enrollmentService.findById(id);
		if (enrollment == null) {
			ra.addFlashAttribute("flashErr", "Không tìm thấy lượt ghi danh cần sửa.");
			return "redirect:/admin/enrollment";
		}
		model.addAttribute("enrollment", enrollment);
		napDuLieuChonLua(model);
		return "admin/enrollment/form";
	}

	@PostMapping("/save")
	public String save(@ModelAttribute Enrollment enrollment, RedirectAttributes ra) {
		if (enrollment.getUser() == null || enrollment.getCourse() == null) {
			ra.addFlashAttribute("flashErr", "Phải chọn cả học viên lẫn khóa học.");
			return "redirect:/admin/enrollment";
		}

		if (enrollmentService.isDuplicate(enrollment)) {
			ra.addFlashAttribute("flashErr", "Học viên này đã ghi danh khóa học đó rồi.");
			return "redirect:/admin/enrollment";
		}

		boolean isNew = enrollment.getId() == null;

		// Sửa một lượt ghi danh sang học viên khác hoặc khóa khác thì bên LMS phải
		// gỡ ở cặp cũ, không thì người cũ vẫn nằm trong lớp cũ bên đó.
		//
		// Chép tham chiếu ra biến riêng TRƯỚC khi lưu: findById trả về đúng đối
		// tượng JPA đang quản lý, lưu xong nó bị ghi đè bằng giá trị mới.
		User nguoiCu = null;
		Course khoaCu = null;
		if (!isNew) {
			Enrollment cu = enrollmentService.findById(enrollment.getId());
			if (cu != null) {
				nguoiCu = cu.getUser();
				khoaCu = cu.getCourse();
			}
		}
		boolean doiCap = nguoiCu != null && khoaCu != null
				&& (nguoiCu.getId() != enrollment.getUser().getId()
						|| !khoaCu.getId().equals(enrollment.getCourse().getId()));

		try {
			enrollmentService.save(enrollment);
		} catch (Exception e) {
			ra.addFlashAttribute("flashErr", "Không lưu được lượt ghi danh.");
			return "redirect:/admin/enrollment";
		}

		String thongBao = isNew ? "Đã thêm lượt ghi danh" : "Đã cập nhật lượt ghi danh";

		if (doiCap) {
			try {
				lmsSsoService.huyGhiDanhBenLms(nguoiCu, khoaCu);
			} catch (Exception e) {
				System.err.println("[AdminEnrollment] Chưa gỡ được ghi danh cũ bên LMS: " + e.getMessage());
				thongBao += " (chưa gỡ được lượt ghi danh cũ bên LMS — " + e.getMessage()
						+ "; cần gỡ tay bên đó)";
			}
		}

		// Đẩy ghi danh sang LMS ngay, để giáo viên bên đó thấy đủ danh sách lớp
		// từ ngày đầu, kể cả những bạn chưa làm bài nào.
		//
		// LMS lỗi thì KHÔNG hủy ghi danh bên buni: người học vẫn vào xem bài được,
		// và lần đầu họ bấm bài kiểm tra hệ thống sẽ tự ghi danh bù. Chỉ báo cho
		// admin biết là còn dang dở.
		Course course = enrollment.getCourse();
		if (course.getMoodleCourseId() == null) {
			ra.addFlashAttribute("flashWarn", thongBao + ", nhưng khóa này chưa nối với LMS nên chưa ghi danh "
					+ "bên đó. Nối khóa với LMS trước thì học viên mới làm được bài kiểm tra.");
			return "redirect:/admin/enrollment";
		}

		try {
			lmsSsoService.ghiDanhBenLms(enrollment.getUser(), course);
			ra.addFlashAttribute("flashOk", thongBao + " và đã ghi danh bên LMS.");
		} catch (Exception e) {
			System.err.println("[AdminEnrollment] Chưa đẩy được ghi danh sang LMS: " + e.getMessage());
			ra.addFlashAttribute("flashWarn", thongBao + " bên buni, nhưng chưa ghi danh được bên LMS ("
					+ e.getMessage() + "). Học viên vẫn xem bài được; lần đầu bấm bài kiểm tra hệ thống "
					+ "sẽ tự ghi danh bù.");
		}
		return "redirect:/admin/enrollment";
	}

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable Integer id, RedirectAttributes ra) {
		// Lấy học viên và khóa ra trước — xóa xong thì không còn biết phải gỡ ai
		// khỏi khóa nào bên LMS.
		Enrollment enrollment = enrollmentService.findById(id);
		if (enrollment == null) {
			ra.addFlashAttribute("flashErr", "Không tìm thấy lượt ghi danh.");
			return "redirect:/admin/enrollment";
		}
		User nguoiDung = enrollment.getUser();
		Course course = enrollment.getCourse();

		try {
			enrollmentService.deleteById(id);
		} catch (Exception e) {
			ra.addFlashAttribute("flashErr", "Không xóa được lượt ghi danh.");
			return "redirect:/admin/enrollment";
		}

		// Xóa ở buni trước, gỡ bên LMS sau. Có hỏng bước sau thì quyền học bên buni
		// vẫn đã bị thu — mà học viên chỉ vào được LMS qua buni, nên cửa đã đóng.
		// Chỉ còn danh sách lớp bên LMS lệch, báo admin gỡ tay.
		try {
			lmsSsoService.huyGhiDanhBenLms(nguoiDung, course);
			ra.addFlashAttribute("flashOk", course.getMoodleCourseId() == null ? "Đã xóa lượt ghi danh."
					: "Đã xóa lượt ghi danh và gỡ học viên khỏi khóa bên LMS.");
		} catch (Exception e) {
			System.err.println("[AdminEnrollment] Chưa gỡ được ghi danh bên LMS: " + e.getMessage());
			ra.addFlashAttribute("flashWarn", "Đã xóa lượt ghi danh bên buni, nhưng chưa gỡ được bên LMS ("
					+ e.getMessage() + "). Học viên không vào học được nữa, chỉ còn tên trong danh sách lớp "
					+ "bên LMS — cần gỡ tay bên đó.");
		}
		return "redirect:/admin/enrollment";
	}

	private void napDuLieuChonLua(Model model) {
		model.addAttribute("users", userService.findAll());
		model.addAttribute("courses", courseService.getAllCourses());
	}
    
}
 