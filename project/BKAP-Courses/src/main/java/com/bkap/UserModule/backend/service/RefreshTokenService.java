package com.bkap.UserModule.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bkap.UserModule.backend.repository.IRefreshTokenRepository;
import com.bkap.UserModule.entity.RefreshToken;
import com.bkap.UserModule.entity.User;

/**
 * Cấp, xác minh và thu hồi refresh token.
 */
@Service
public class RefreshTokenService implements IRefreshTokenService {

	@Autowired
	private IRefreshTokenRepository refreshTokenRepository;

	@Value("${app.jwt.refresh-token-days}")
	private long soNgayHieuLuc;

	private final SecureRandom random = new SecureRandom();

	/**
	 * Kết quả của một lần làm mới. Vì bật xoay vòng nên mỗi lần dùng refresh token
	 * là nhận về một token mới, token cũ bị thu hồi ngay.
	 */
	public record KetQuaLamMoi(User user, String refreshTokenMoi) {
	}

	@Override
	@Transactional
	public String cap(User user) {
		// Dọn token cũ của chính người này trước khi cấp mới, để bảng không phình
		// vô hạn mà không cần job chạy nền.
		refreshTokenRepository.donRac(user.getId(), LocalDateTime.now());

		// 32 byte ngẫu nhiên từ SecureRandom — không đoán được, khác hẳn Random
		// thường vốn suy ngược ra được chuỗi tiếp theo.
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String tokenTho = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		RefreshToken banGhi = new RefreshToken();
		banGhi.setUser(user);
		banGhi.setTokenHash(bam(tokenTho));
		banGhi.setExpiresAt(LocalDateTime.now().plusDays(soNgayHieuLuc));
		refreshTokenRepository.save(banGhi);

		// Token thô chỉ tồn tại ở đây và trên máy người dùng. DB chỉ có hash.
		return tokenTho;
	}

	@Override
	@Transactional
	public Optional<KetQuaLamMoi> xacMinhVaXoay(String tokenTho) {
		if (tokenTho == null || tokenTho.isBlank()) {
			return Optional.empty();
		}

		Optional<RefreshToken> optional = refreshTokenRepository.findByTokenHash(bam(tokenTho));
		if (optional.isEmpty()) {
			return Optional.empty();
		}

		RefreshToken banGhi = optional.get();
		if (!banGhi.conHieuLuc()) {
			return Optional.empty();
		}

		// Xoay vòng: thu hồi token vừa dùng rồi cấp cái mới. Nhờ vậy mỗi refresh
		// token chỉ dùng được đúng một lần — kẻ trộm được token cũ mà nạn nhân đã
		// dùng qua rồi thì cũng vô ích.
		banGhi.setRevokedAt(LocalDateTime.now());
		refreshTokenRepository.save(banGhi);

		User user = banGhi.getUser();
		return Optional.of(new KetQuaLamMoi(user, cap(user)));
	}

	@Override
	@Transactional
	public void thuHoiTatCa(User user) {
		if (user == null) {
			return;
		}
		refreshTokenRepository.thuHoiTatCa(user.getId(), LocalDateTime.now());
	}

	/**
	 * SHA-256, trả về chuỗi hex 64 ký tự.
	 *
	 * Ở đây dùng SHA-256 chứ không phải BCrypt như mật khẩu, và đó là chủ ý:
	 * BCrypt cố tình chậm để chống dò mật khẩu do người đặt (vốn dễ đoán). Refresh
	 * token là 32 byte ngẫu nhiên, không có gì để dò, nên chỉ cần hàm băm nhanh —
	 * dùng BCrypt ở đây chỉ làm mỗi request refresh chậm đi vô ích.
	 */
	private String bam(String giaTri) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(md.digest(giaTri.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 là thuật toán bắt buộc có trong mọi bản Java, nhánh này không
			// bao giờ chạy tới.
			throw new IllegalStateException("Máy ảo Java không hỗ trợ SHA-256", e);
		}
	}
}
