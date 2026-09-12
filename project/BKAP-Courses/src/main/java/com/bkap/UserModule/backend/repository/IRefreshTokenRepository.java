package com.bkap.UserModule.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bkap.UserModule.entity.RefreshToken;

public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Thu hồi mọi token còn hiệu lực của một người — dùng khi đăng xuất. */
	@Modifying
	@Query("UPDATE RefreshToken t SET t.revokedAt = :thoiDiem WHERE t.user.id = :userId AND t.revokedAt IS NULL")
	int thuHoiTatCa(@Param("userId") short userId, @Param("thoiDiem") LocalDateTime thoiDiem);

	/**
	 * Dọn các token đã hết hạn hoặc đã thu hồi từ lâu. Gọi mỗi lần cấp token mới
	 * cho người đó, đủ để bảng không phình vô hạn mà không cần job chạy nền.
	 */
	@Modifying
	@Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId AND (t.expiresAt < :moc OR t.revokedAt < :moc)")
	int donRac(@Param("userId") short userId, @Param("moc") LocalDateTime moc);
}
