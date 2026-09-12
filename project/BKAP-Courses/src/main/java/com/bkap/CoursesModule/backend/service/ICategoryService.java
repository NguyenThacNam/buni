package com.bkap.CoursesModule.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bkap.CoursesModule.entity.Category;

public interface ICategoryService {

	List<Category> findAll();

	/** Tìm kiếm có phân trang cho danh sách admin. q = "" nghĩa là lấy tất cả. */
	Page<Category> timKiem(String q, Pageable pageable);

	Category findById(Short id);

	/**
	 * Lưu từ form admin. Sửa danh mục có sẵn thì CHỈ chép các trường trên form,
	 * giữ nguyên mối nối LMS (moodleCategoryId).
	 *
	 * @throws IllegalArgumentException khi chọn danh mục cha tạo thành vòng
	 */
	Category save(Category category);

	/**
	 * Tên đầy đủ theo cây của mọi danh mục, ví dụ "Chuyển đổi số › Cho doanh
	 * nghiệp". Dùng cho ô chọn và cột "Thuộc danh mục" ở trang admin.
	 */
	java.util.Map<Short, String> tenDayDu();

	/** Các danh mục được phép làm cha của danh mục id — loại chính nó và con cháu của nó. */
	List<Category> chaHopLe(Short id);

	/**
	 * Danh mục hiện ngoài trang người dùng: đang hiện VÀ mọi danh mục cha của nó
	 * cũng đang hiện — ẩn danh mục cha là ẩn luôn cả nhánh. Xếp theo độ ưu tiên.
	 */
	List<Category> danhMucCongKhai();

	/** Id của danh mục có slug này cùng mọi con cháu đang hiện; rỗng nếu slug không có. */
	java.util.Set<Short> idVaConChauCongKhai(String slug);

	void deleteById(Short id);
}
