package com.bkap.UserModule.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bkap.UserModule.backend.repository.IUserRepository;
import com.bkap.UserModule.dto.UserResponseDTO;
import com.bkap.UserModule.entity.User;
import com.bkap.UserModule.entity.UserRole;
import com.bkap.UserModule.form.UserFormForRegister;

@Service
public class UserService implements IUserService {

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public User login(String username, String password) {
		// 1. Tìm customer theo username
		Optional<User> optional = userRepository.findByUsername(username);

		// 2. Đối chiếu mật khẩu người dùng nhập với hash BCrypt lưu trong DB.
		// Không so sánh bằng equals() được nữa: mỗi hash BCrypt chứa salt ngẫu
		// nhiên riêng nên cùng một mật khẩu vẫn cho ra chuỗi khác nhau.
		if (optional.isPresent() && passwordEncoder.matches(password, optional.get().getPassword())) {
			return optional.get();
		}
		return null; // Trả về null nếu sai thông tin
	}

	@Override
	public boolean existsByPhone(String phone) {
		return userRepository.existsByPhone(phone);
	}

	@Override
	public boolean existsByEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	@Override
	public User createCustomer(UserFormForRegister form) {
		User user = new User();
		user.setFullname(form.getFullname());
		user.setUsername(form.getUsername());
		user.setPhone(form.getPhone());
		user.setEmail(form.getEmail());
		// Chỉ hash mới được lưu xuống DB. Mật khẩu thô vẫn nằm trong form để
		// UserRegisterController đồng bộ sang Moodle — Moodle cần bản gốc.
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setRole(UserRole.STUDENT);
		user.setBirthday(form.getBirthday());

		return userRepository.save(user);
	}

	@Override
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	public User findByUsername(String username) {
		return userRepository.findByUsername(username).orElse(null);
	}

	@Override
	public void updateLastLogin(String username) {
		Optional<User> optional = userRepository.findByUsername(username);
		if (optional.isPresent()) {
			User user = optional.get();
			user.setLastLogin(new java.util.Date());
			user.setOnline(true); // ← Đánh dấu online
			userRepository.save(user);
		}
	}

	@Override
	public void updateLastLogout(String username) {
		Optional<User> optional = userRepository.findByUsername(username);
		if (optional.isPresent()) {
			User user = optional.get();
			user.setLastLogout(new java.util.Date());
			user.setOnline(false); // ← Đánh dấu offline
			userRepository.save(user);
		}
	}

	@Override
	public UserResponseDTO getUserByUsername(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy: " + username));

		UserResponseDTO dto = new UserResponseDTO();
		dto.setFullname(user.getFullname());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setPhone(user.getPhone());
		// dto.setAddress(user.getAddress());
		// dto.setBio(user.getBio());
		dto.setRole(user.getRole() != null ? user.getRole().name() : null);

		// ✅ birthday là String → gán thẳng, không cần format
		dto.setBirthday(user.getBirthday());

		return dto;
	}

	@Override
	public UserResponseDTO updateUserProfile(String username, UserResponseDTO request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy: " + username));

		// Chỉ cập nhật các field được phép thay đổi
		if (request.getFullname() != null) {
			user.setFullname(request.getFullname());
		}
		if (request.getPhone() != null) {
			user.setPhone(request.getPhone());
		}
		if (request.getBirthday() != null) {
			user.setBirthday(request.getBirthday());
		}
		// if (request.getAddress() != null) {
		// user.setAddress(request.getAddress());
		// }
		// if (request.getBio() != null) {
		// user.setBio(request.getBio());
		// }

		userRepository.save(user);

		// Trả về data mới nhất
		return getUserByUsername(username);

	}

	@Override
	public List<User> findAll() {
		return userRepository.findAll();
	}

	@Override
	public User findById(Short id) {
		return userRepository.findById(id).orElse(null);
	}

	/**
	 * Form admin không có ô nào cho lastLogin, lastLogout, isOnline hay createDate,
	 * nên nếu lưu thẳng object Spring dựng từ form thì lịch sử đăng nhập của người
	 * dùng bị xóa trắng — đúng lỗi đã gặp ở CourseService. Vì vậy khi sửa thì nạp
	 * bản ghi cũ rồi chỉ chép đè những trường form thực sự gửi lên.
	 *
	 * Mật khẩu để trống nghĩa là không đổi: giữ nguyên hash đang có. Có nhập thì
	 * băm bằng BCrypt, tuyệt đối không ghi mật khẩu thô xuống DB.
	 */
	/** Độ dài tối thiểu, khớp với kiểm tra phía frontend. */
	private static final int DO_DAI_MAT_KHAU_TOI_THIEU = 6;

	@Override
	public User doiMatKhau(String username, String matKhauCu, String matKhauMoi) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

		if (matKhauCu == null || !passwordEncoder.matches(matKhauCu, user.getPassword())) {
			throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
		}
		if (matKhauMoi == null || matKhauMoi.trim().length() < DO_DAI_MAT_KHAU_TOI_THIEU) {
			throw new IllegalArgumentException(
					"Mật khẩu mới phải có ít nhất " + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự");
		}
		if (passwordEncoder.matches(matKhauMoi.trim(), user.getPassword())) {
			throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
		}

		user.setPassword(passwordEncoder.encode(matKhauMoi.trim()));
		return userRepository.save(user);
	}

	@Override
	public User saveFromAdmin(User form, String rawPassword) {
		boolean hasNewPassword = rawPassword != null && !rawPassword.isBlank();

		if (form.getId() == 0) {
			form.setPassword(hasNewPassword ? passwordEncoder.encode(rawPassword.trim()) : "");
			return userRepository.save(form);
		}

		User existing = userRepository.findById(form.getId())
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng id = " + form.getId()));

		existing.setFullname(form.getFullname());
		existing.setUsername(form.getUsername());
		existing.setEmail(form.getEmail());
		existing.setPhone(form.getPhone());
		existing.setBirthday(form.getBirthday());
		existing.setRole(form.getRole());

		if (hasNewPassword) {
			existing.setPassword(passwordEncoder.encode(rawPassword.trim()));
		}

		// lastLogin, lastLogout, isOnline, createDate: giữ nguyên, không đụng tới.
		return userRepository.save(existing);
	}

	@Override
	public void deleteById(Short id) {
		userRepository.deleteById(id);
	}

	@Override
	public long count() {
		return userRepository.count();
	}

	@Override
	public long countByRole(UserRole role) {
		return userRepository.countByRole(role);
	}

	@Override
	public Page<User> timKiem(String q, Pageable pageable) {
		return userRepository.timKiem(q == null ? "" : q.trim(), pageable);
	}
}
