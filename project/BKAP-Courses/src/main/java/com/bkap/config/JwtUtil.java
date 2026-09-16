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
		return generateToken(username, role, null);
	}

	/**
	 * Access token kèm token LMS của chính người dùng (đã mã hóa).
	 *
	 * Làm bài kiểm tra ngay trên buni cần token Moodle CỦA HỌC VIÊN: các hàm làm
	 * bài chạy dưới danh nghĩa chủ token, dùng token dịch vụ thì mọi lượt làm đều
	 * ghi sang tên tài khoản dịch vụ. Token đó lấy được lúc đăng nhập; không có
	 * CSDL để cất, nên gói vào vé phiên — nhưng MÃ HÓA, vì JWT chỉ ký chứ không
	 * giấu nội dung: ai mở DevTools cũng giải base64 đọc được.
	 */
	public String generateToken(String username, String role, String tokenLmsMaHoa) {
		long hetHan = System.currentTimeMillis() + soPhutHieuLuc * 60 * 1000;
		io.jsonwebtoken.JwtBuilder b = Jwts.builder().setSubject(username).claim("role", role).setIssuedAt(new Date())
				.setExpiration(new Date(hetHan));
		if (tokenLmsMaHoa != null) {
			b.claim("lms", tokenLmsMaHoa);
		}
		return b.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	/**
	 * Hạn của refresh token.
	 *
	 * Trước đây refresh token là chuỗi ngẫu nhiên lưu trong bảng refresh_token,
	 * nên đăng xuất hay đổi mật khẩu là thu hồi được ngay. Bỏ CSDL thì không còn
	 * chỗ ghi "vé này đã hủy", nên nó thành JWT tự chứng minh — đổi lại KHÔNG thu
	 * hồi được: vé lọt ra ngoài vẫn dùng được tới khi hết hạn.
	 *
	 * Vì vậy hạn nên để ngắn hơn thời CSDL. Đổi ở app.jwt.refresh-token-days.
	 */
	@Value("${app.jwt.refresh-token-days}")
	private long soNgayLamMoi;

	/**
	 * Vé để xin access token mới. Chỉ mang tên đăng nhập, KHÔNG mang vai trò —
	 * vai trò phải hỏi lại LMS mỗi lần làm mới, không thì gỡ quyền quản trị bên
	 * LMS xong người ta vẫn cầm vé cũ vào được cả tuần.
	 */
	public String taoRefreshToken(String username, boolean laAdmin) {
		return taoRefreshToken(username, laAdmin, null);
	}

	/** Vé làm mới mang theo token LMS đã mã hóa, để access token cấp lại vẫn làm bài được. */
	public String taoRefreshToken(String username, boolean laAdmin, String tokenLmsMaHoa) {
		long hetHan = System.currentTimeMillis() + soNgayLamMoi * 24 * 60 * 60 * 1000;
		io.jsonwebtoken.JwtBuilder b = Jwts.builder().setSubject(username).claim("muc_dich", "lam_moi")
				.claim("role", laAdmin ? "ADMIN" : "STUDENT").setIssuedAt(new Date()).setExpiration(new Date(hetHan));
		if (tokenLmsMaHoa != null) {
			b.claim("lms", tokenLmsMaHoa);
		}
		return b.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	/** Phần token LMS (vẫn đang mã hóa) nằm trong một vé, để chuyển sang vé mới. */
	public String docTokenLmsMaHoa(String jwt) {
		try {
			return getClaims(jwt).get("lms", String.class);
		} catch (Exception e) {
			return null;
		}
	}

	/** Token LMS đã giải mã từ access token hoặc vé làm mới; null nếu không có hoặc hỏng. */
	public String docTokenLms(String jwt) {
		return giaiMaTokenLms(docTokenLmsMaHoa(jwt));
	}

	// ─── Mã hóa token LMS ────────────────────────────────────────────────────
	// AES-256-GCM: vừa giấu nội dung, vừa phát hiện bị sửa. Khóa dẫn xuất từ
	// app.jwt.secret nên không phải cấu hình thêm bí mật mới; đổi khóa ký JWT
	// là token LMS cũ cũng mất hiệu lực theo — đúng ý muốn.

	private javax.crypto.spec.SecretKeySpec khoaMaHoaTokenLms() throws Exception {
		byte[] bam = java.security.MessageDigest.getInstance("SHA-256")
				.digest(("buni-lms-token:" + secret).getBytes(StandardCharsets.UTF_8));
		return new javax.crypto.spec.SecretKeySpec(bam, "AES");
	}

	public String maHoaTokenLms(String tokenLms) {
		if (tokenLms == null || tokenLms.isBlank()) {
			return null;
		}
		try {
			byte[] iv = new byte[12];
			new java.security.SecureRandom().nextBytes(iv);
			javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
			c.init(javax.crypto.Cipher.ENCRYPT_MODE, khoaMaHoaTokenLms(), new javax.crypto.spec.GCMParameterSpec(128, iv));
			byte[] ma = c.doFinal(tokenLms.getBytes(StandardCharsets.UTF_8));
			byte[] goi = new byte[iv.length + ma.length];
			System.arraycopy(iv, 0, goi, 0, iv.length);
			System.arraycopy(ma, 0, goi, iv.length, ma.length);
			return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(goi);
		} catch (Exception e) {
			throw new IllegalStateException("Không mã hóa được token LMS", e);
		}
	}

	private String giaiMaTokenLms(String goiBase64) {
		if (goiBase64 == null || goiBase64.isBlank()) {
			return null;
		}
		try {
			byte[] goi = java.util.Base64.getUrlDecoder().decode(goiBase64);
			javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
			c.init(javax.crypto.Cipher.DECRYPT_MODE, khoaMaHoaTokenLms(),
					new javax.crypto.spec.GCMParameterSpec(128, goi, 0, 12));
			return new String(c.doFinal(goi, 12, goi.length - 12), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Vai trò ghi trong vé làm mới.
	 *
	 * Phải mang theo vì cờ "quản trị site" chỉ hỏi được bằng token của chính
	 * người dùng, mà lúc làm mới thì không có mật khẩu để xin token đó. Hệ quả:
	 * gỡ quyền quản trị bên LMS chỉ ăn sau khi vé hết hạn, hoặc khi người đó đăng
	 * nhập lại. Muốn ăn ngay thì phải chuyển sang xác định admin bằng nhóm
	 * (cohort) bên Moodle — tra được bằng token dịch vụ bất cứ lúc nào.
	 */
	public String docVaiTroTuRefreshToken(String token) {
		try {
			Claims claims = getClaims(token);
			if (!"lam_moi".equals(claims.get("muc_dich", String.class))) {
				return "STUDENT";
			}
			return "ADMIN".equals(claims.get("role", String.class)) ? "ADMIN" : "STUDENT";
		} catch (Exception e) {
			return "STUDENT";
		}
	}

	/** Tên đăng nhập trong vé làm mới, hoặc null nếu vé hỏng, hết hạn, hay sai loại. */
	public String docRefreshToken(String token) {
		try {
			Claims claims = getClaims(token);
			if (!"lam_moi".equals(claims.get("muc_dich", String.class))) {
				return null;
			}
			return claims.getSubject();
		} catch (Exception e) {
			return null;
		}
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
