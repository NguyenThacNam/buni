package com.bkap.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.bkap.UserModule.backend.service.MoodleAuthService;
import com.bkap.UserModule.backend.service.MoodleAuthService.NguoiDungLms;

/**
 * Đăng nhập khu quản trị bằng tài khoản LMS.
 *
 * Trước đây đối chiếu với bảng users của buni và đòi cột role = ADMIN. Bỏ CSDL
 * thì cả mật khẩu lẫn vai trò đều do LMS giữ: ai là quản trị viên của
 * lms.buni.vn thì vào được khu quản trị buni, gỡ quyền bên đó là mất quyền ở
 * đây ngay lần đăng nhập sau.
 *
 * Học viên gõ đúng mật khẩu của mình vẫn KHÔNG vào được — bị chặn ở bước kiểm
 * tra cờ quản trị bên dưới, chứ không phải chỉ dựa vào hasRole trong
 * WebConfiguration.
 */
@Component
public class AdminMoodleAuthProvider implements AuthenticationProvider {

	private final MoodleAuthService moodleAuthService;

	/**
	 * @Lazy để cắt vòng phụ thuộc: chuỗi bảo mật cần provider này, mà service lại
	 *       nằm trong nhánh bean khác cũng tham chiếu ngược lại cấu hình.
	 */
	@Autowired
	public AdminMoodleAuthProvider(@Lazy MoodleAuthService moodleAuthService) {
		this.moodleAuthService = moodleAuthService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = String.valueOf(authentication.getPrincipal());
		String matKhau = String.valueOf(authentication.getCredentials());

		NguoiDungLms nguoiDung;
		try {
			nguoiDung = moodleAuthService.dangNhap(username, matKhau);
		} catch (IllegalArgumentException e) {
			throw new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu");
		} catch (Exception e) {
			System.err.println("[AdminAuth] Không hỏi được LMS: " + e.getMessage());
			throw new BadCredentialsException("Chưa kết nối được hệ thống LMS");
		}

		if (!nguoiDung.laAdmin()) {
			// Cùng một câu báo với sai mật khẩu: nói rõ "tài khoản này có thật nhưng
			// không đủ quyền" là giúp người dò biết họ đoán trúng tài khoản.
			throw new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu");
		}

		return new UsernamePasswordAuthenticationToken(nguoiDung.username(), null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
