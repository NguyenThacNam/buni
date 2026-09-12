package com.bkap.config;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	/**
	 * TUYỆT ĐỐI KHÔNG in access token ra log.
	 *
	 * Bản trước ghi nguyên header Authorization vào out.log, mỗi request một dòng.
	 * File log nằm trên đĩa máy chủ, ai đọc được là mạo danh được người dùng đó cho
	 * tới khi token hết hạn — kể cả tài khoản quản trị. Token là thông tin xác
	 * thực, đối xử với nó như mật khẩu.
	 *
	 * Chỉ ghi ở mức DEBUG và chỉ ghi username, để môi trường chạy thật (mức INFO)
	 * không sinh ra gì cả. Bot quét web cũng liên tục gọi /wp-login.php, /robots.txt
	 * — ghi mọi request vào log chỉ làm file phình vô ích.
	 */
	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

	@Autowired
	private JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);

			if (jwtUtil.validateToken(token)) {
				String username = jwtUtil.getUsernameFromToken(token);
				String role = jwtUtil.getRoleFromToken(token);

				log.debug("Xác thực thành công: {} ({}) cho {}", username, role, request.getRequestURI());

				SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username,
						null, List.of(authority));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} else {
				// Không kèm token vào thông báo: token hết hạn vẫn là token, và đây là
				// nhánh hay bị bot gọi nhất.
				log.debug("Token không hợp lệ hoặc đã hết hạn cho {}", request.getRequestURI());
			}
		}

		filterChain.doFilter(request, response);
	}
}
