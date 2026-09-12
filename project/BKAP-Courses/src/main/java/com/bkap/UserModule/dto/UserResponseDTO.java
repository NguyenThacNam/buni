package com.bkap.UserModule.dto;

public class UserResponseDTO {
	private String fullname;
	private String username;
	private String phone;
	private String email;
	private String role;
	private String birthday;
	// Nếu giao diện profile cần thêm địa chỉ và giới thiệu bản thân, bạn giữ lại:
//	private String address;
//	private String bio;

	public UserResponseDTO() {
		super();
		// TODO Auto-generated constructor stub
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

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getBirthday() {
		return birthday;
	}

	public void setBirthday(String birthday) {
		this.birthday = birthday;
	}

}
