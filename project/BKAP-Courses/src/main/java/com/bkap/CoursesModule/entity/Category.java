package com.bkap.CoursesModule.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "category")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Short id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 100)
	private String slug;

	@Column(length = 20)
	private String status = "active";

	@Column(name = "prioty")
	private Integer prioty = 0;

	/**
	 * Danh mục cha. Để trống nghĩa là danh mục gốc.
	 *
	 * Moodle xếp danh mục thành cây — "Chuyển đổi số cho cơ quan Đảng" nằm trong
	 * "Chuyển đổi số". Giữ lại quan hệ đó để sau này lọc theo nhóm lớn vẫn ra
	 * được khóa của cả các nhánh con.
	 *
	 * Chỉ lưu id chứ không map @ManyToOne, để tránh tải dây chuyền cả cây mỗi
	 * lần đọc một danh mục.
	 */
	@Column(name = "parent_id")
	private Short parentId;

	/** Đối chiếu với danh mục bên Moodle khi đồng bộ. */
	@Column(name = "moodle_category_id")
	private Integer moodleCategoryId;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt = LocalDateTime.now();

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Category() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Short getId() {
		return id;
	}

	public void setId(Short id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getPrioty() {
		return prioty;
	}

	public void setPrioty(Integer prioty) {
		this.prioty = prioty;
	}

	public Short getParentId() {
		return parentId;
	}

	public void setParentId(Short parentId) {
		this.parentId = parentId;
	}

	public Integer getMoodleCategoryId() {
		return moodleCategoryId;
	}

	public void setMoodleCategoryId(Integer moodleCategoryId) {
		this.moodleCategoryId = moodleCategoryId;
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