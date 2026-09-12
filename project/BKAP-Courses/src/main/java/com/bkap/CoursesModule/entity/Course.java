package com.bkap.CoursesModule.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bkap.UserModule.entity.User;

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

@Entity
@Table(name = "course")
public class Course {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Short id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "instructor_id", nullable = false)
	private User instructor;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, unique = true, length = 200)
	private String slug;

	@Column(length = 300)
	private String subtitle;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "thumbnail_url", length = 500)
	private String thumbnailUrl;

	@Column(name = "preview_video_url", length = 500)
	private String previewVideoUrl;

	@Column(length = 20)
	private String level = "ALL";

	@Column(name = "is_pro")
	private Integer isPro = 0;

	private BigDecimal price = BigDecimal.ZERO;

	@Column(name = "original_price")
	private BigDecimal originalPrice = BigDecimal.ZERO;

	@Column(name = "price_type", length = 20)
	private String priceType = "PAID";

	private Double rating = 0.0;

	@Column(name = "rating_count")
	private Integer ratingCount = 0;

	@Column(name = "student_count")
	private Integer studentCount = 0;

	@Column(name = "lesson_count")
	private Integer lessonCount = 0;

	@Column(name = "duration_text", length = 50)
	private String durationText;

	/**
	 * Khóa học tương ứng bên Moodle, lấy từ URL /course/view.php?id=...
	 *
	 * Đây là mắt xích nối hai hệ thống: có nó thì mới ghi danh được người học vào
	 * đúng khóa và đẩy họ tới đúng trang.
	 *
	 * Cố ý lưu ID số thay vì đối chiếu slug với idnumber bên Moodle — slug sửa
	 * được trong trang admin, đổi một cái là liên kết đứt mà không báo gì.
	 *
	 * NULL nghĩa là khóa chưa mở trên LMS; giao diện hiện "Sắp khai giảng".
	 */
	@Column(name = "moodle_course_id")
	private Integer moodleCourseId;

	/**
	 * "active" thì hiện ngoài trang khách, "inactive" thì chỉ admin thấy.
	 *
	 * Khóa đồng bộ từ LMS về luôn ở trạng thái ẩn: lúc đó mới có tên, mô tả và
	 * ảnh bìa, còn giá và danh mục thì chưa. Hiện ngay ra trang chủ là khách gặp
	 * một khóa học nham nhở.
	 */
	@Column(nullable = false, length = 20)
	private String status = "active";

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt = LocalDateTime.now();

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Course() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Short getId() {
		return id;
	}

	public void setId(Short id) {
		this.id = id;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public User getInstructor() {
		return instructor;
	}

	public void setInstructor(User instructor) {
		this.instructor = instructor;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getPreviewVideoUrl() {
		return previewVideoUrl;
	}

	public void setPreviewVideoUrl(String previewVideoUrl) {
		this.previewVideoUrl = previewVideoUrl;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Integer getIsPro() {
		return isPro;
	}

	public void setIsPro(Integer isPro) {
		this.isPro = isPro;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(BigDecimal originalPrice) {
		this.originalPrice = originalPrice;
	}

	public String getPriceType() {
		return priceType;
	}

	public void setPriceType(String priceType) {
		this.priceType = priceType;
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public Integer getRatingCount() {
		return ratingCount;
	}

	public void setRatingCount(Integer ratingCount) {
		this.ratingCount = ratingCount;
	}

	public Integer getStudentCount() {
		return studentCount;
	}

	public void setStudentCount(Integer studentCount) {
		this.studentCount = studentCount;
	}

	public Integer getLessonCount() {
		return lessonCount;
	}

	public void setLessonCount(Integer lessonCount) {
		this.lessonCount = lessonCount;
	}

	public String getDurationText() {
		return durationText;
	}

	public void setDurationText(String durationText) {
		this.durationText = durationText;
	}

	public Integer getMoodleCourseId() {
		return moodleCourseId;
	}

	public void setMoodleCourseId(Integer moodleCourseId) {
		this.moodleCourseId = moodleCourseId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
