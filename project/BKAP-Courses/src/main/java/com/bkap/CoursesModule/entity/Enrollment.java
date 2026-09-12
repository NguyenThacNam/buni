package com.bkap.CoursesModule.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bkap.UserModule.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Một lượt học viên ghi danh vào khóa học, kèm tiến độ học.
 *
 * Bảng enrollment đã có sẵn trong bkap-ai-courses.sql từ đầu nhưng chưa bao giờ
 * được map sang Java, nên trang "Khóa học của tôi" bên frontend phải chạy bằng
 * dữ liệu giả.
 *
 * Khóa chính dùng Integer chứ không phải Short như Course/Category: số lượt ghi
 * danh tăng theo số học viên nhân số khóa học, vượt trần 32767 của Short rất
 * nhanh.
 */
@Entity
@Table(name = "enrollment")
public class Enrollment {

	/** Trạng thái hợp lệ, khớp với ràng buộc CHECK của cột status trong DB. */
	public static final String STATUS_ENROLLED = "ENROLLED";
	public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
	public static final String STATUS_COMPLETED = "COMPLETED";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Column(nullable = false, length = 20)
	private String status = STATUS_ENROLLED;

	@Column(name = "progress_percent")
	private Integer progressPercent = 0;

	@Column(name = "enrolled_at", updatable = false)
	private LocalDateTime enrolledAt = LocalDateTime.now();

	@Column(name = "last_studied_at")
	private LocalDateTime lastStudiedAt = LocalDateTime.now();

	@PreUpdate
	protected void onUpdate() {
		lastStudiedAt = LocalDateTime.now();
	}

	public Enrollment() {
		super();
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

	public Course getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getProgressPercent() {
		return progressPercent;
	}

	public void setProgressPercent(Integer progressPercent) {
		this.progressPercent = progressPercent;
	}

	public LocalDateTime getEnrolledAt() {
		return enrolledAt;
	}

	public void setEnrolledAt(LocalDateTime enrolledAt) {
		this.enrolledAt = enrolledAt;
	}

	public LocalDateTime getLastStudiedAt() {
		return lastStudiedAt;
	}

	public void setLastStudiedAt(LocalDateTime lastStudiedAt) {
		this.lastStudiedAt = lastStudiedAt;
	}

	/**
	 * Ngày tháng định dạng sẵn cho template.
	 *
	 * Thymeleaf có #temporals cho kiểu java.time, nhưng nó nằm ở thư viện phụ
	 * thymeleaf-extras-java8time chứ không phải lõi. Định dạng ngay tại đây thì
	 * template không phụ thuộc vào thư viện đó nữa.
	 */
	@JsonIgnore
	public String getEnrolledAtText() {
		return enrolledAt == null ? "—" : enrolledAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
	}

	@JsonIgnore
	public String getLastStudiedAtText() {
		return lastStudiedAt == null ? "—" : lastStudiedAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
	}

	/** Nhãn tiếng Việt để hiển thị, tránh nhét chuỗi cứng vào template. */
	@JsonIgnore
	public String getStatusLabel() {
		if (STATUS_COMPLETED.equals(status)) {
			return "Hoàn thành";
		}
		if (STATUS_IN_PROGRESS.equals(status)) {
			return "Đang học";
		}
		return "Mới ghi danh";
	}
}
