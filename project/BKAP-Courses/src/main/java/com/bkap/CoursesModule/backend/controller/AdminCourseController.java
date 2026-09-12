package com.bkap.CoursesModule.backend.controller;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bkap.config.FileStorageService;

import com.bkap.CoursesModule.backend.service.ICategoryService;
import com.bkap.CoursesModule.backend.service.ICourseService;
import com.bkap.CoursesModule.backend.service.LmsSyncService;
import com.bkap.CoursesModule.entity.Category;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.UserModule.backend.service.IUserService;
import com.bkap.UserModule.entity.User;

@Controller
@RequestMapping("/admin/course")
public class AdminCourseController {

	@Autowired
	private ICourseService courseService;

	@Autowired
	private ICategoryService categoryService;

	@Autowired
	private IUserService userService;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private LmsSyncService lmsSyncService;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Category.class, "category", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				if (text == null || text.isEmpty()) {
					setValue(null);
					return;
				}
				Category category = new Category();
				category.setId(Short.parseShort(text));
				setValue(category);
			}
		});

		binder.registerCustomEditor(User.class, "instructor", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				if (text == null || text.isEmpty()) {
					setValue(null);
					return;
				}
				User user = new User();
				user.setId(Short.parseShort(text));
				setValue(user);
			}
		});
	}

	/** Số dòng mỗi trang trong bảng danh sách. */
	private static final int SO_DONG_MOI_TRANG = 10;

	@GetMapping
	public String list(@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "page", defaultValue = "0") int page, Model model) {

		// Math.max chặn ?page=-1 gõ tay trên thanh địa chỉ, PageRequest sẽ ném lỗi
		// nếu nhận số âm.
		Page<Course> trang = courseService.timKiem(q,
				PageRequest.of(Math.max(page, 0), SO_DONG_MOI_TRANG, Sort.by("id")));

		model.addAttribute("courses", trang.getContent());
		model.addAttribute("trang", trang);
		model.addAttribute("q", q);
		return "admin/course/list";
	}

	// ✅ THÊM MỚI — bị thiếu trước đó
	@GetMapping("/create")
	public String create(Model model) {
		model.addAttribute("course", new Course());
		// Hiện tên theo cây ("Chuyển đổi số › Cho doanh nghiệp") và xếp cùng nhánh
		// đứng cạnh nhau — danh mục con trùng tên ở hai nhánh mới phân biệt được.
		java.util.Map<Short, String> tenDayDu = categoryService.tenDayDu();
		model.addAttribute("tenDayDu", tenDayDu);
		model.addAttribute("categories", categoryService.findAll().stream()
				.sorted(java.util.Comparator.comparing(c -> tenDayDu.getOrDefault(c.getId(), c.getName()))).toList());
		model.addAttribute("instructors", userService.findAll());
		return "admin/course/form";
	}

	@PostMapping("/save")
	public String save(@ModelAttribute Course course,
			@RequestParam(name = "thumbnailFile", required = false) MultipartFile thumbnailFile,
			RedirectAttributes ra) {
		boolean isNew = course.getId() == null;

		// Xử lý ảnh tải lên TRƯỚC khi lưu, và tách riêng khối try để thông báo lỗi
		// nói đúng chuyện — gộp chung sẽ báo nhầm thành "slug đã tồn tại".
		// Có chọn file thì file thắng, ghi đè ô URL. Không chọn thì giữ nguyên URL
		// admin đã gõ.
		try {
			String duongDanAnh = fileStorageService.luuAnh(thumbnailFile);
			if (duongDanAnh != null) {
				course.setThumbnailUrl(duongDanAnh);
			}
		} catch (IOException e) {
			ra.addFlashAttribute("flashErr", "Không tải được ảnh: " + e.getMessage());
			return "redirect:/admin/course";
		}

		try {
			courseService.save(course);
			ra.addFlashAttribute("flashOk",
					isNew ? "Đã thêm khóa học \"" + course.getTitle() + "\"."
							: "Đã cập nhật khóa học \"" + course.getTitle() + "\".");
		} catch (Exception e) {
			ra.addFlashAttribute("flashErr", "Không lưu được. Slug \"" + course.getSlug() + "\" có thể đã tồn tại.");
		}
		return "redirect:/admin/course";
	}
    
    

	@GetMapping("/edit/{id}")
	public String edit(@PathVariable Short id, Model model) {
		model.addAttribute("course", courseService.getCourseById(id));
		// Hiện tên theo cây ("Chuyển đổi số › Cho doanh nghiệp") và xếp cùng nhánh
		// đứng cạnh nhau — danh mục con trùng tên ở hai nhánh mới phân biệt được.
		java.util.Map<Short, String> tenDayDu = categoryService.tenDayDu();
		model.addAttribute("tenDayDu", tenDayDu);
		model.addAttribute("categories", categoryService.findAll().stream()
				.sorted(java.util.Comparator.comparing(c -> tenDayDu.getOrDefault(c.getId(), c.getName()))).toList());
		model.addAttribute("instructors", userService.findAll());
		return "admin/course/form";
	}

	/**
	 * Kéo danh mục và khóa học mới từ LMS về.
	 *
	 * Chỉ thêm cái chưa có, không đụng vào khóa đã tồn tại — xem ghi chú trong
	 * LmsSyncService về lý do không ghi đè.
	 */
	@PostMapping("/sync")
	public String dongBoTuLms(RedirectAttributes ra) {
		LmsSyncService.KetQua kq = lmsSyncService.dongBo();

		if (!kq.thanhCong()) {
			ra.addFlashAttribute("flashErr", "Không đồng bộ được từ LMS: " + kq.loi());
			return "redirect:/admin/course";
		}

		StringBuilder sb = new StringBuilder("Đồng bộ xong. ");
		sb.append("Thêm ").append(kq.khoaHocMoi()).append(" khóa học");
		if (kq.danhMucMoi() > 0) {
			sb.append(" và ").append(kq.danhMucMoi()).append(" danh mục");
		}
		sb.append(", bỏ qua ").append(kq.khoaHocBoQua()).append(" khóa đã có.");
		if (kq.khoaHocMoi() > 0) {
			sb.append(" Khóa mới đang ẩn — điền giá và danh mục rồi chuyển sang Hiện.");
		}

		ra.addFlashAttribute("flashOk", sb.toString());
		return "redirect:/admin/course";
	}

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable Short id, RedirectAttributes ra) {
		try {
			courseService.deleteCourse(id);
			ra.addFlashAttribute("flashOk", "Đã xóa khóa học.");
		} catch (Exception e) {
			ra.addFlashAttribute("flashErr",
					"Không xóa được: khóa học này đang có học viên ghi danh. Xóa các lượt ghi danh trước.");
		}
		return "redirect:/admin/course";
	}
	
}