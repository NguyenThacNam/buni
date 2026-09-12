package com.bkap.UserModule.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bkap.UserModule.dto.UserResponseDTO;
import com.bkap.UserModule.entity.User;
import com.bkap.UserModule.entity.UserRole;
import com.bkap.UserModule.form.UserFormForRegister;

public interface IUserService {

	User login(String username, String password);

	boolean existsByPhone(String phone);

	boolean existsByEmail(String email);

	User createCustomer(UserFormForRegister form);

	boolean existsByUsername(String username);

	User findByUsername(String username);

	/**
	 * Học viên tự đổi mật khẩu. Bắt nhập đúng mật khẩu hiện tại — nếu không, ai
	 * nhặt được máy đang đăng nhập sẵn là đổi luôn được, chiếm hẳn tài khoản.
	 *
	 * @throws IllegalArgumentException kèm câu báo lỗi cho người dùng
	 */
	User doiMatKhau(String username, String matKhauCu, String matKhauMoi);

	void updateLastLogin(String username);

	void updateLastLogout(String username);

	// Lấy thông tin user dựa vào username (hoặc lấy từ JWT token/Principal)
	UserResponseDTO getUserByUsername(String username);

	UserResponseDTO updateUserProfile(String username, UserResponseDTO request);

	// ===== Thêm mới cho module Admin (Thymeleaf) =====
	List<User> findAll();

	User findById(Short id);

	/**
	 * Lưu người dùng từ form admin.
	 *
	 * @param rawPassword mật khẩu thô admin gõ vào. Để trống nghĩa là "không đổi
	 *                    mật khẩu" — khi đó giữ nguyên hash cũ trong DB.
	 */
	User saveFromAdmin(User form, String rawPassword);

	void deleteById(Short id);

	long count();

	long countByRole(UserRole role);

	/** Tìm kiếm có phân trang cho danh sách admin. q = "" nghĩa là lấy tất cả. */
	Page<User> timKiem(String q, Pageable pageable);
}
