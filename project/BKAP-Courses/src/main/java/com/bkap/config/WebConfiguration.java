package com.bkap.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebConfiguration {

	@Autowired
	private JwtAuthFilter jwtAuthFilter;

	/** Danh sách origin được phép gọi API, khai báo trong application.properties */
	@Value("${app.cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Autowired
	private AdminMoodleAuthProvider adminMoodleAuthProvider;

	/**
	 * Chuỗi bảo mật RIÊNG cho khu quản trị Thymeleaf, chạy trước chuỗi API.
	 *
	 * Vì sao phải tách làm hai: phần API xác thực bằng JWT và hoàn toàn STATELESS,
	 * nhưng trình duyệt gõ thẳng /admin/course thì không đính kèm header
	 * Authorization nào cả — nên JWT không dùng được cho trang render sẵn. Khu
	 * admin vì vậy cần form đăng nhập và session như web truyền thống.
	 *
	 * CSRF để BẬT ở đây (khác chuỗi API): đã có session thì phải chống giả mạo
	 * request. Thymeleaf tự chèn hidden input _csrf vào mọi form có th:action nên
	 * các form Lưu/Xóa không phải sửa gì.
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/admin/**")
				.authorizeHttpRequests(auth -> auth
						// Trang đăng nhập phải vào được khi chưa đăng nhập, nếu không sẽ
						// lặp vô hạn: chưa đăng nhập -> đá về /admin/login -> lại chặn.
						.requestMatchers("/admin/login").permitAll()
						// Toàn bộ phần còn lại: bắt buộc đăng nhập VÀ phải là ADMIN.
						// Học viên đăng nhập đúng mật khẩu vẫn nhận 403.
						.anyRequest().hasRole("ADMIN"))
				.formLogin(form -> form.loginPage("/admin/login").loginProcessingUrl("/admin/login")
						.defaultSuccessUrl("/admin", true).failureUrl("/admin/login?error").permitAll())
				.logout(logout -> logout.logoutUrl("/admin/logout").logoutSuccessUrl("/admin/login?logout")
						.invalidateHttpSession(true).deleteCookies("JSESSIONID"))
				// Xác thực bằng tài khoản LMS: mật khẩu và quyền quản trị đều do Moodle
				// giữ, buni không còn bảng người dùng. Học viên gõ đúng mật khẩu vẫn bị
				// chặn, vì provider đòi cờ quản trị site bên LMS.
					.authenticationProvider(adminMoodleAuthProvider);
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfigurationSource())).csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// Mặc định Spring Security gắn X-Frame-Options: DENY, tức cấm mọi
				// trang nhúng nội dung này vào iframe. Trang học lại phải nhúng PDF
				// bằng iframe để đọc tại chỗ, nên trình duyệt chặn thẳng — và báo là
				// "localhost đã từ chối kết nối", nghe như lỗi mạng chứ không lộ ra
				// rằng chính backend cấm.
				//
				// Thay bằng CSP frame-ancestors, cùng tác dụng chống clickjacking
				// nhưng nói rõ được ai mới có quyền nhúng. X-Frame-Options chỉ có
				// DENY hoặc SAMEORIGIN, không kê khai được origin cụ thể — mà lúc
				// chạy dev thì trang ở cổng 3001 còn file ở 8080, khác origin.
				// Trình duyệt gặp frame-ancestors thì bỏ qua X-Frame-Options.
				.headers(h -> h.frameOptions(f -> f.disable()).contentSecurityPolicy(
						csp -> csp.policyDirectives("frame-ancestors 'self' " + String.join(" ", allowedOrigins))))

				// Chưa xác thực thì trả 401, không phải 403.
				// Mặc định Spring Security trả 403 cho cả trường hợp thiếu token lẫn
				// token hết hạn — sai ngữ nghĩa, và quan trọng hơn là khiến frontend
				// không phân biệt được "token hết hạn, cần làm mới" với "đã đăng nhập
				// nhưng không đủ quyền". Interceptor chỉ được phép gọi /auth/refresh ở
				// trường hợp đầu; gặp trường hợp sau mà đi làm mới token là lặp vô ích.
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/login").permitAll()
						// Làm mới token: bản thân nó KHÔNG cần access token hợp lệ — gọi
						// endpoint này chính là vì access token đã hết hạn. Refresh token
						// gửi trong body mới là thứ được kiểm tra.
						.requestMatchers("/api/v1/auth/refresh").permitAll()
						// Tự đăng ký tài khoản đã tắt — admin cấp tài khoản cho học viên.
						// Xem ghi chú trong UserRegisterController.
						// .requestMatchers("/api/v1/usersRegister").permitAll()

						// /api/v1/moodle/** TỪNG được mở ở đây, không cần đăng nhập. Hậu quả:
						// ai gửi {"username": "<tên bất kỳ>"} tới /moodle/autologin-url là
						// nhận được link vào thẳng tài khoản LMS của người đó — xem điểm,
						// làm bài hộ, nộp bài thay. /moodle/enrol cũng vậy, ai cũng ghi danh
						// được ai vào khóa nào.
						//
						// Đã gỡ. Giờ nhánh này rơi xuống quy tắc chung /api/v1/** bên dưới,
						// tức bắt buộc đăng nhập. Đường vào LMS chuẩn là /learn/lms-url,
						// lấy danh tính từ token chứ không tin lời trình duyệt khai.
						// .requestMatchers("/api/v1/moodle/**").permitAll()

						.requestMatchers(HttpMethod.GET, "/api/v1/courses/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/courses/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/courses/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/courses/**").hasRole("ADMIN")

						// File tài liệu và video: KHÔNG dùng access token được, vì trình
						// duyệt tải nội dung của thẻ iframe/video bằng request thường,
						// không kèm header Authorization. Endpoint này tự bảo vệ bằng
						// đường dẫn đã ký có hạn 30 phút — xem JwtUtil.kyDuongDanFile.
						.requestMatchers(HttpMethod.GET, "/api/v1/learn/file").permitAll()

						// Mọi endpoint API còn lại đều bắt buộc có JWT hợp lệ
						.requestMatchers("/api/v1/**").authenticated()

						// /admin/** KHÔNG còn khai ở đây nữa — đã có chuỗi riêng
						// adminSecurityFilterChain ở trên xử lý, với form đăng nhập và
						// yêu cầu quyền ADMIN.

						// Phần còn lại là file tĩnh của bản build React (/, /static/**, /favicon.ico...)
						// và các route do React Router xử lý phía client → mở công khai.
						// Dữ liệu vẫn được bảo vệ vì mọi API đều nằm dưới /api/v1.
						.anyRequest().permitAll())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * Thuật toán băm mật khẩu dùng chung cho cả đăng ký lẫn đăng nhập. BCrypt tự
	 * sinh salt ngẫu nhiên và nhúng vào chuỗi kết quả, nên không cần cột salt
	 * riêng; đổi lại phải đối chiếu bằng matches() chứ không so sánh chuỗi.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
		config.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
