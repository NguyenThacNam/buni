package com.bkap.CoursesModule.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bkap.CoursesModule.backend.service.ICategoryService;
import com.bkap.CoursesModule.entity.Category;

@Controller
@RequestMapping("/admin/category")
public class AdminCategoryController {

	@Autowired
	private ICategoryService categoryService;

	/** Số dòng mỗi trang trong bảng danh sách. */
	private static final int SO_DONG_MOI_TRANG = 10;

	// Hiển thị danh sách
	@GetMapping
	public String list(@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "page", defaultValue = "0") int page, Model model) {

		Page<Category> trang = categoryService.timKiem(q,
				PageRequest.of(Math.max(page, 0), SO_DONG_MOI_TRANG, Sort.by("prioty")));

		model.addAttribute("categories", trang.getContent());
		// Tên đầy đủ theo cây, để cột "Thuộc danh mục" hiện được tên cha.
		model.addAttribute("tenDayDu", categoryService.tenDayDu());
		model.addAttribute("trang", trang);
		model.addAttribute("q", q);
		return "admin/category/list";
	}

	// Hiển thị form thêm mới
	@GetMapping("/create")
	public String create(Model model) {
		model.addAttribute("category", new Category());
		napChonCha(model, null);
		return "admin/category/form";
	}

	// Lưu category (thêm mới hoặc cập nhật)
	@PostMapping("/save")
	public String save(@ModelAttribute Category category, RedirectAttributes ra) {
		boolean isNew = category.getId() == null;
		try {
			categoryService.save(category);
			ra.addFlashAttribute("flashOk",
					isNew ? "Đã thêm danh mục \"" + category.getName() + "\"."
							: "Đã cập nhật danh mục \"" + category.getName() + "\".");
		} catch (IllegalArgumentException e) {
			// Chọn cha tạo thành vòng (chính nó, hoặc con cháu của nó).
			ra.addFlashAttribute("flashErr", e.getMessage());
		} catch (Exception e) {
			// Hay gặp nhất là trùng slug — cột này có ràng buộc UNIQUE
			ra.addFlashAttribute("flashErr", "Không lưu được. Slug \"" + category.getSlug() + "\" có thể đã tồn tại.");
		}
		return "redirect:/admin/category";
	}

	// Hiển thị form sửa
	@GetMapping("/edit/{id}")
	public String edit(@PathVariable Short id, Model model) {
		Category category = categoryService.findById(id);
		model.addAttribute("category", category);
		napChonCha(model, id);
		return "admin/category/form";
	}

	// Xóa category
	@PostMapping("/delete/{id}")
	public String delete(@PathVariable Short id, RedirectAttributes ra) {
		try {
			categoryService.deleteById(id);
			ra.addFlashAttribute("flashOk", "Đã xóa danh mục.");
		} catch (Exception e) {
			// Khóa ngoại từ bảng course chặn lại nếu danh mục còn khóa học
			ra.addFlashAttribute("flashErr",
					"Không xóa được: danh mục này vẫn còn khóa học. Chuyển các khóa học sang danh mục khác trước.");
		}
		return "redirect:/admin/category";
	}

	/**
	 * Danh sách được chọn làm "danh mục cha": loại chính nó và con cháu của nó,
	 * sắp theo tên đầy đủ để các danh mục cùng nhánh đứng cạnh nhau.
	 */
	private void napChonCha(Model model, Short id) {
		java.util.Map<Short, String> ten = categoryService.tenDayDu();
		model.addAttribute("tenDayDu", ten);
		model.addAttribute("chaHopLe", categoryService.chaHopLe(id).stream()
				.sorted(java.util.Comparator.comparing(c -> ten.getOrDefault(c.getId(), c.getName()))).toList());
	}
}
