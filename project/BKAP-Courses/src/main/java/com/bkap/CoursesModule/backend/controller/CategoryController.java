package com.bkap.CoursesModule.backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bkap.CoursesModule.backend.service.LmsCatalogService;

/**
 * Danh mục khóa học cho trang công khai — đọc từ LMS.
 *
 * Mỗi danh mục kèm parentId để frontend dựng cây cha–con. Ẩn/hiện do bên LMS
 * quyết; ẩn danh mục cha là cả nhánh con biến mất theo.
 */
@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

	@Autowired
	private LmsCatalogService lmsCatalogService;

	@GetMapping()
	public ResponseEntity<?> getAllCategories() {
		try {
			return ResponseEntity.ok(lmsCatalogService.danhMucCongKhai());
		} catch (Exception e) {
			System.err.println("[Category] Không lấy được danh mục: " + e.getMessage());
			return ResponseEntity.status(502).body(Map.of("error", "Chưa lấy được danh mục từ hệ thống LMS."));
		}
	}
}
