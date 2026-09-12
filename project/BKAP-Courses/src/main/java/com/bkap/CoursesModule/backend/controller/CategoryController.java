package com.bkap.CoursesModule.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.repository.ICategoryRepository;
import com.bkap.CoursesModule.backend.service.ICategoryService;
import com.bkap.CoursesModule.entity.Category;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

	@Autowired
	private ICategoryRepository categoryRepository;

	@Autowired
	private ICategoryService categoryService;

	// API: GET http://localhost:8080/api/v1/categories
	@GetMapping()
	public ResponseEntity<?> getAllCategories() {
		try {
			// Chỉ danh mục đang hiện (ẩn cha là ẩn cả nhánh), xếp theo độ ưu tiên.
			// Bản trước trả toàn bộ bảng, nên admin bấm "ẩn" mà ngoài trang vẫn hiện.
			// Mỗi danh mục kèm parentId để frontend tự dựng cây.
			List<Category> list = categoryService.danhMucCongKhai();
			return ResponseEntity.status(200).body(list);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
		}
	}
}
