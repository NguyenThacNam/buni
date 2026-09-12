package com.bkap.UserModule.entity;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
// Bảng tên "users" chứ không phải "User": USER là từ khóa dành riêng của
// PostgreSQL, để nguyên sẽ lỗi cú pháp. Bỏ catalog vì trong PostgreSQL catalog
// chính là database, không thể truy vấn chéo — tên database nằm trong JDBC URL.
@Table(name = "users")
public class User {
	@Column(name = "id")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private short id;

	@Column(name = "fullname", length = 100, nullable = false)
	private String fullname;

	// ─────────────────────────────────────────────────────────────────────────
	// Entity này bị Jackson serialize gián tiếp qua Course.instructor, mà
	// GET /api/v1/courses là endpoint public không cần token. Nếu không chặn,
	// toàn bộ mật khẩu và thông tin cá nhân của giảng viên sẽ lộ ra ngoài.
	//
	// Chỉ id và fullname được phép xuất hiện trong JSON — đúng bằng những gì
	// frontend cần (CourseCard, CourseDetail dùng instructor.fullname).
	//
	// Các API trả thông tin người dùng thật sự (login, register, /user/profile)
	// đều tự dựng Map hoặc UserResponseDTO nên không bị ảnh hưởng.
	// ─────────────────────────────────────────────────────────────────────────

	@JsonIgnore
	@Column(name = "username", length = 50, nullable = false, unique = true)
	private String username;

	@JsonIgnore
	@Column(name = "phone", length = 10, nullable = false, unique = true)
	private String phone;

	@JsonIgnore
	@Column(name = "email", length = 150, nullable = false, unique = true)
	private String email;

	// length nới lên 255 cho khớp cột trong DB: hash BCrypt luôn dài 60 ký tự,
	// giới hạn 50 cũ sẽ cắt cụt hash và làm login không bao giờ khớp.
	@JsonIgnore
	@Column(name = "password", length = 255, nullable = false)
	private String password;

	@JsonIgnore
	@Column(name = "birthday", length = 12, nullable = false)
	private String birthday;

	@JsonIgnore
	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole role;

	@JsonIgnore
	@Column(name = "createDate")
	@Temporal(TemporalType.TIMESTAMP)
	@CreationTimestamp
	private Date createDate;

	@JsonIgnore
	@Column(name = "last_login", nullable = true)
	private Date lastLogin;

	@JsonIgnore
	@Column(name = "last_logout", nullable = true)
	private Date lastLogout;

	@JsonIgnore
	@Column(name = "is_online", nullable = true)
	private boolean isOnline;

	public User() {
		super();
		// TODO Auto-generated constructor stub
	}

	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

	public String getBirthday() {
		return birthday;
	}

	public void setBirthday(String birthday) {
		this.birthday = birthday;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public Date getLastLogout() {
		return lastLogout;
	}

	public void setLastLogout(Date lastLogout) {
		this.lastLogout = lastLogout;
	}

	// @JsonIgnore phải đặt cả ở đây: field tên isOnline nhưng getter isOnline()
	// khiến Jackson suy ra property tên "online" — khác tên field nên annotation
	// trên field không có tác dụng với property này.
	@JsonIgnore
	public boolean isOnline() {
		return isOnline;
	}

	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}

}
