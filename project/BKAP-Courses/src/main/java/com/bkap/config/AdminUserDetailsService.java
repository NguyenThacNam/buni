package com.bkap.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bkap.UserModule.backend.repository.IUserRepository;
import com.bkap.UserModule.entity.User;

/**
 * Nguồn tài khoản cho màn hình đăng nhập của khu quản trị.
 *
 * Không tạo bảng tài khoản riêng — dùng thẳng bảng users sẵn có, nên phong quyền
 * ADMIN cho ai trong /admin/user là người đó đăng nhập được ngay.
 *
 * Mật khẩu trong DB đã là hash BCrypt nên trả về nguyên hash; Spring Security tự
 * đối chiếu bằng PasswordEncoder khai trong WebConfiguration.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

	private final IUserRepository userRepository;

	/**
	 * @Lazy để cắt vòng phụ thuộc: WebConfiguration cần service này, mà service lại
	 *       nằm trong chuỗi khởi tạo của Spring Security.
	 */
	@Autowired
	public AdminUserDetailsService(@Lazy IUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

		String role = user.getRole() == null ? "STUDENT" : user.getRole().name();

		// Trả về mọi tài khoản, kể cả STUDENT. Việc chặn quyền do
		// SecurityFilterChain lo bằng hasRole("ADMIN") — làm vậy thì học viên đăng
		// nhập nhầm vào /admin sẽ nhận 403 "không đủ quyền" thay vì "sai mật khẩu",
		// đúng bản chất vấn đề hơn.
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
	}
}
