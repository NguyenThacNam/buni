package com.bkap.UserModule.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Refresh token — vé dài hạn để xin access token mới.
 *
 * Access token là JWT stateless: server không lưu gì, cứ chữ ký đúng và còn hạn
 * là chấp nhận, nên KHÔNG thu hồi được. Đó là lý do access token chỉ sống 15
 * phút, còn refresh token thì lưu xuống bảng này để đăng xuất có thể thu hồi
 * thật.
 *
 * Cột token_hash lưu SHA-256 của token, không lưu token thô — đọc được bảng này
 * mà token còn nguyên dạng thì mạo danh được bất kỳ ai.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@JsonIgnore
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	/** Khác null nghĩa là đã bị thu hồi, không dùng được nữa. */
	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public RefreshToken() {
		super();
	}

	/** Còn dùng được: chưa thu hồi và chưa hết hạn. */
	public boolean conHieuLuc() {
		return revokedAt == null && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
