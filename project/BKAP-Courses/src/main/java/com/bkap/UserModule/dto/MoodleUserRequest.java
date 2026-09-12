package com.bkap.UserModule.dto;

/**
 * DTO chứa thông tin user gửi lên Moodle API. Đã cấu trúc sẵn các trường dữ
 * liệu mặc định để tránh null pointer.
 */
public class MoodleUserRequest {

	private String username;
	private String password;
	private String firstname;
	private String lastname;
	private String email;

	/** Phương thức xác thực trên Moodle. Mặc định luôn là "manual" */
	private String auth = "manual";

	// --- Getters & Setters ---

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}
}