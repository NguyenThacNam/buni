package com.bkap.CoursesModule.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bkap.CoursesModule.backend.repository.IEnrollmentRepository;
import com.bkap.CoursesModule.entity.Enrollment;

@Service
public class EnrollmentService implements IEnrollmentService {

	@Autowired
	private IEnrollmentRepository enrollmentRepository;

	@Override
	public List<Enrollment> findAll() {
		return enrollmentRepository.findAll();
	}

	@Override
	public Page<Enrollment> timKiem(String q, Pageable pageable) {
		return enrollmentRepository.timKiem(q == null ? "" : q.trim(), pageable);
	}

	@Override
	public Enrollment findById(Integer id) {
		return enrollmentRepository.findById(id).orElse(null);
	}

	@Override
	public List<Enrollment> findByUsername(String username) {
		return enrollmentRepository.findByUser_Username(username);
	}

	/**
	 * Cùng cách làm như CourseService.save: form admin không gửi hết các trường,
	 * nên khi sửa phải nạp bản ghi cũ rồi chép đè từng trường, tránh ghi mất
	 * enrolledAt bằng giá trị mặc định của object mới.
	 */
	@Override
	public Enrollment save(Enrollment enrollment) {
		if (enrollment.getId() == null) {
			return enrollmentRepository.save(enrollment);
		}

		Optional<Enrollment> optional = enrollmentRepository.findById(enrollment.getId());
		if (optional.isEmpty()) {
			return enrollmentRepository.save(enrollment);
		}

		Enrollment existing = optional.get();
		existing.setUser(enrollment.getUser());
		existing.setCourse(enrollment.getCourse());
		existing.setStatus(enrollment.getStatus());
		if (enrollment.getProgressPercent() != null) {
			existing.setProgressPercent(enrollment.getProgressPercent());
		}
		// enrolledAt: giữ nguyên ngày ghi danh gốc, không cho form ghi đè.
		return enrollmentRepository.save(existing);
	}

	@Override
	public void deleteById(Integer id) {
		enrollmentRepository.deleteById(id);
	}

	/**
	 * Một học viên chỉ được ghi danh một lần vào mỗi khóa. DB đã có UNIQUE
	 * (user_id, course_id) chặn từ dưới, nhưng kiểm tra ở đây để báo lỗi tử tế
	 * thay vì để bung stack trace ra trang trắng.
	 */
	@Override
	public boolean isDuplicate(Enrollment enrollment) {
		if (enrollment.getUser() == null || enrollment.getCourse() == null) {
			return false;
		}
		Enrollment existing = enrollment.getId() == null ? null : findById(enrollment.getId());
		// Khi sửa mà không đổi cặp user/khóa học thì không tính là trùng.
		if (existing != null && existing.getUser().getId() == enrollment.getUser().getId()
				&& existing.getCourse().getId().equals(enrollment.getCourse().getId())) {
			return false;
		}
		return enrollmentRepository.existsByUser_IdAndCourse_Id(enrollment.getUser().getId(),
				enrollment.getCourse().getId());
	}

	@Override
	public long count() {
		return enrollmentRepository.count();
	}

	@Override
	public long countByStatus(String status) {
		return enrollmentRepository.countByStatus(status);
	}

	@Override
	public boolean daGhiDanh(String username, Short courseId) {
		if (username == null || courseId == null) {
			return false;
		}
		return enrollmentRepository.existsByUser_UsernameAndCourse_Id(username, courseId);
	}

	@Override
	public List<com.bkap.CoursesModule.entity.Course> khoaCuaNguoiDung(short userId) {
		return enrollmentRepository.timKhoaCuaNguoiDung(userId);
	}
}
