package com.bkap.config;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	/**
	 * Khóa ký token, đọc từ application.properties thay vì viết cứng trong mã
	 * nguồn — để đổi khóa trên máy chủ không phải build lại jar.
	 *
	 * Lưu ý: đổi khóa là mọi access token đang lưu hành mất hiệu lực ngay, người
	 * dùng bị đăng xuất hàng loạt. Refresh token thì không sao vì nó nằm trong DB
	 * chứ không phải JWT.
	 */
	@Value("${app.jwt.secret}")
	private String secret;

	/**
	 * Access token cố ý để hạn RẤT ngắn.
	 *
	 * JWT là stateless: server không lưu gì, cứ chữ ký đúng và còn hạn là chấp
	 * nhận — nghĩa là không thu hồi được. Token lọt ra ngoài thì cách duy nhất
	 * giới hạn thiệt hại là cho nó chết nhanh. Người dùng không bị phiền vì
	 * frontend tự dùng refresh token xin cái mới.
	 */
	@Value("${app.jwt.access-token-minutes}")
	private long soPhutHieuLuc;

	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	// Tạo access token từ username + role
	public String generateToken(String username, String role) {
		long hetHan = System.currentTimeMillis() + soPhutHieuLuc * 60 * 1000;
		return Jwts.builder().setSubject(username).claim("role", role).setIssuedAt(new Date())
				.setExpiration(new Date(hetHan)).signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	/**
	 * Ký một đường dẫn file để nhúng thẳng vào thẻ img, iframe hay video.
	 *
	 * Vì sao cần: trình duyệt tải nội dung của mấy thẻ đó bằng request thường,
	 * KHÔNG kèm header Authorization. Nên không thể bảo vệ file bằng access token
	 * như các API khác.
	 *
	 * Cách làm: backend ký sẵn đường dẫn kèm hạn ngắn rồi nhúng vào URL. Ai copy
	 * link gửi cho người khác thì chỉ dùng được trong 30 phút, và chỉ đúng file
	 * đó — không mở được file khác.
	 */
	public String kyDuongDanFile(String duongDan) {
		long hetHan = System.currentTimeMillis() + 30 * 60 * 1000;
		return Jwts.builder().setSubject(duongDan).claim("muc_dich", "file").setIssuedAt(new Date())
				.setExpiration(new Date(hetHan)).signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	/**
	 * Kiểm tra chữ ký rồi trả lại đường dẫn đã ký. Trả null nếu token hỏng, hết
	 * hạn, hoặc là token đăng nhập bị đem dùng nhầm chỗ.
	 */
	public String docDuongDanFile(String token) {
		try {
			Claims claims = getClaims(token);
			if (!"file".equals(claims.get("muc_dich", String.class))) {
				return null;
			}
			return claims.getSubject();
		} catch (Exception e) {
			return null;
		}
	}

	// Lấy username từ token
	public String getUsernameFromToken(String token) {
		return getClaims(token).getSubject();
	}

	// Lấy role từ token
	public String getRoleFromToken(String token) {
		return getClaims(token).get("role", String.class);
	}

	// Kiểm tra token còn hạn không
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// Helper dùng nội bộ
	private Claims getClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
	}

}
