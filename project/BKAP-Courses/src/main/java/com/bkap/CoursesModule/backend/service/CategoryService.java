package com.bkap.CoursesModule.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bkap.CoursesModule.backend.repository.ICategoryRepository;
import com.bkap.CoursesModule.entity.Category;

@Service
public class CategoryService implements ICategoryService {

	@Autowired
	private ICategoryRepository categoryRepository;

	@Override
	public List<Category> findAll() {
		return categoryRepository.findAll();
	}

	@Override
	public Page<Category> timKiem(String q, Pageable pageable) {
		return categoryRepository.timKiem(q == null ? "" : q.trim(), pageable);
	}

	@Override
	public Category findById(Short id) {
		return categoryRepository.findById(id).orElse(null);
	}

	@Override
	public Category save(Category category) {
		kiemTraKhongTaoVong(category.getId(), category.getParentId());

		if (category.getId() == null) {
			return categoryRepository.save(category);
		}

		// Sửa: nạp bản đang có rồi chỉ chép các trường trên form.
		//
		// Bản trước lưu thẳng đối tượng dựng từ form. Form không có ô mã danh mục
		// bên LMS, nên mỗi lần sửa — kể cả chỉ đổi độ ưu tiên — mối nối đó bị ghi
		// đè thành NULL, và lần đồng bộ sau lại sinh thêm một danh mục trùng tên.
		Category cu = categoryRepository.findById(category.getId())
				.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
		cu.setName(category.getName());
		cu.setSlug(category.getSlug());
		cu.setStatus(category.getStatus());
		cu.setPrioty(category.getPrioty());
		cu.setParentId(category.getParentId());
		return categoryRepository.save(cu);
	}

	/**
	 * Cha không được là chính nó hay một danh mục con cháu của nó — nếu không, cây
	 * thành vòng tròn và mọi chỗ đi ngược lên gốc sẽ lặp vô tận.
	 */
	private void kiemTraKhongTaoVong(Short id, Short chaId) {
		if (id == null || chaId == null) {
			return;
		}
		java.util.Map<Short, Short> chaCua = new java.util.HashMap<>();
		for (Category c : categoryRepository.findAll()) {
			chaCua.put(c.getId(), c.getParentId());
		}
		Short dangXet = chaId;
		for (int buoc = 0; dangXet != null && buoc < 100; buoc++) {
			if (dangXet.equals(id)) {
				throw new IllegalArgumentException("Không thể chọn chính nó hoặc danh mục con của nó làm danh mục cha");
			}
			dangXet = chaCua.get(dangXet);
		}
	}

	@Override
	public java.util.Map<Short, String> tenDayDu() {
		java.util.Map<Short, Category> theoId = new java.util.HashMap<>();
		for (Category c : categoryRepository.findAll()) {
			theoId.put(c.getId(), c);
		}
		java.util.Map<Short, String> ket = new java.util.HashMap<>();
		for (Category c : theoId.values()) {
			java.util.Deque<String> ten = new java.util.ArrayDeque<>();
			Category dangXet = c;
			// Giới hạn số bước phòng dữ liệu cũ lỡ có vòng — thà cắt ngắn tên còn
			// hơn treo cả trang.
			for (int buoc = 0; dangXet != null && buoc < 10; buoc++) {
				ten.addFirst(dangXet.getName());
				dangXet = dangXet.getParentId() == null ? null : theoId.get(dangXet.getParentId());
			}
			ket.put(c.getId(), String.join(" › ", ten));
		}
		return ket;
	}

	@Override
	public List<Category> chaHopLe(Short id) {
		List<Category> tatCa = categoryRepository.findAll();
		if (id == null) {
			return tatCa;
		}
		// Gom id của chính nó và mọi con cháu, rồi loại ra.
		java.util.Set<Short> loaiRa = new java.util.HashSet<>();
		loaiRa.add(id);
		boolean themDuoc = true;
		while (themDuoc) {
			themDuoc = false;
			for (Category c : tatCa) {
				if (c.getParentId() != null && loaiRa.contains(c.getParentId()) && loaiRa.add(c.getId())) {
					themDuoc = true;
				}
			}
		}
		return tatCa.stream().filter(c -> !loaiRa.contains(c.getId())).toList();
	}

	@Override
	public void deleteById(Short id) {
		categoryRepository.deleteById(id);
	}


	@Override
	public List<Category> danhMucCongKhai() {
		List<Category> tatCa = categoryRepository.findAll();
		java.util.Map<Short, Category> theoId = new java.util.HashMap<>();
		for (Category c : tatCa) {
			theoId.put(c.getId(), c);
		}
		return tatCa.stream().filter(c -> caNhanhDangHien(c, theoId))
				.sorted(java.util.Comparator
						.comparing((Category c) -> c.getPrioty() == null ? Integer.MAX_VALUE : c.getPrioty())
						.thenComparing(Category::getName))
				.toList();
	}

	/** Chính nó và mọi cha của nó đều đang hiện. */
	private boolean caNhanhDangHien(Category c, java.util.Map<Short, Category> theoId) {
		Category dangXet = c;
		for (int buoc = 0; dangXet != null && buoc < 10; buoc++) {
			if (!"active".equals(dangXet.getStatus())) {
				return false;
			}
			dangXet = dangXet.getParentId() == null ? null : theoId.get(dangXet.getParentId());
		}
		return true;
	}

	@Override
	public java.util.Set<Short> idVaConChauCongKhai(String slug) {
		List<Category> congKhai = danhMucCongKhai();
		java.util.Set<Short> ket = new java.util.HashSet<>();
		congKhai.stream().filter(c -> c.getSlug().equals(slug)).findFirst().ifPresent(c -> ket.add(c.getId()));
		boolean themDuoc = !ket.isEmpty();
		while (themDuoc) {
			themDuoc = false;
			for (Category c : congKhai) {
				if (c.getParentId() != null && ket.contains(c.getParentId()) && ket.add(c.getId())) {
					themDuoc = true;
				}
			}
		}
		return ket;
	}
}
