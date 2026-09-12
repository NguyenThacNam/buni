package com.bkap.CoursesModule.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bkap.CoursesModule.backend.repository.ICategoryRepository;
import com.bkap.CoursesModule.backend.repository.ICourseRepository;
import com.bkap.CoursesModule.dto.CourseDTO;
import com.bkap.CoursesModule.entity.Category;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.UserModule.backend.repository.IUserRepository;
import com.bkap.UserModule.entity.User;

@Service
public class CourseService implements ICourseService {

	@Autowired
	private ICourseRepository courseRepository;

	@Autowired
	private IUserRepository userRepository; // Inject để tìm thông tin giảng viên liên kết

	@Autowired
	private ICategoryRepository categoryRepository; // Inject để tìm thông tin danh mục liên kết

	@Autowired
	private ICategoryService categoryService;

	@Override
	public Page<Course> timKiem(String q, Pageable pageable) {
		return courseRepository.timKiem(q == null ? "" : q.trim(), pageable);
	}

	@Override
	public List<Course> getAllCourses() {
		// Trả về toàn bộ danh sách khóa học trong database
		return courseRepository.findAll();
	}

	@Override
	public List<Course> getPublicCourses() {
		return courseRepository.findByStatus("active");
	}

	@Override
	public List<Course> getPublicCoursesByCategory(String slug) {
		// Chọn danh mục cha thì hiện cả khóa của các danh mục con. Người xem bấm
		// "Chuyển đổi số" là muốn thấy mọi khóa thuộc nhánh đó, không phải chỉ
		// những khóa gắn thẳng vào đúng danh mục cha (thường là không có khóa nào).
		java.util.Set<Short> ids = categoryService.idVaConChauCongKhai(slug);
		if (ids.isEmpty()) {
			return List.of();
		}
		return courseRepository.findByCategory_IdInAndStatus(ids, "active");
	}

	@Override
	public List<Course> getCoursesByCategory(String slug) {
		// Trả về danh sách khóa học được lọc theo slug của danh mục
		return courseRepository.findByCategory_Slug(slug);
	}

	@Override
	public Course getCourseById(Short id) {
		// Tìm khóa học theo ID sử dụng Optional giống như cách bạn tìm User
		Optional<Course> optional = courseRepository.findById(id);
		if (optional.isPresent()) {
			return optional.get();
		}
		return null; // Trả về null nếu không tìm thấy giống login/findByUsername bên UserService
	}

	@Override
	public Course createCourse(CourseDTO courseDTO) {
		// Khởi tạo thực thể mới bằng từ khóa new giống createCustomer
		Course course = new Course();

		// Set từng trường dữ liệu trực tiếp từ DTO gửi lên
		course.setTitle(courseDTO.getTitle());
		course.setSlug(courseDTO.getSlug());
		course.setSubtitle(courseDTO.getSubtitle());
		course.setDescription(courseDTO.getDescription());
		course.setThumbnailUrl(courseDTO.getThumbnailUrl());
		course.setPreviewVideoUrl(courseDTO.getPreviewVideoUrl());
		course.setLevel(courseDTO.getLevel());
		course.setIsPro(courseDTO.getIsPro());
		course.setPrice(courseDTO.getPrice());
		course.setOriginalPrice(courseDTO.getOriginalPrice());
		course.setPriceType(courseDTO.getPriceType());
		course.setRating(courseDTO.getRating());
		course.setRatingCount(courseDTO.getRatingCount());
		course.setStudentCount(courseDTO.getStudentCount());
		course.setLessonCount(courseDTO.getLessonCount());
		course.setDurationText(courseDTO.getDurationText());

		// 1. Tìm và gán Danh mục (Category) liên kết thông qua ID ngắn (Short)
		if (courseDTO.getCategoryId() != null) {
			Optional<Category> catOpt = categoryRepository.findById(courseDTO.getCategoryId());
			if (catOpt.isPresent()) {
				course.setCategory(catOpt.get());
			}
		}

		// 2. Tìm và gán Giảng viên (User) liên kết thông qua ID
		if (courseDTO.getInstructorId() != null) {
			// Ép kiểu ID của User nếu cần, ở đây sử dụng phương thức findById chuẩn của JPA
			Optional<User> userOpt = userRepository.findById(courseDTO.getInstructorId());
			if (userOpt.isPresent()) {
				course.setInstructor(userOpt.get());
			}
		}

		// Lưu xuống database và trả về thực thể vừa tạo giống UserRepository.save(user)
		return courseRepository.save(course);
	}

	@Override
	public Course updateCourse(Short id, CourseDTO courseDTO) {
		// 1. Tìm khóa học cũ đang có trong Database
		Optional<Course> optional = courseRepository.findById(id);

		if (optional.isPresent()) {
			Course existingCourse = optional.get();

			// 2. Cập nhật các thông tin mới từ DTO đè lên các trường cũ
			existingCourse.setTitle(courseDTO.getTitle());
			existingCourse.setSlug(courseDTO.getSlug());
			existingCourse.setSubtitle(courseDTO.getSubtitle());
			existingCourse.setDescription(courseDTO.getDescription());
			existingCourse.setThumbnailUrl(courseDTO.getThumbnailUrl());
			existingCourse.setPreviewVideoUrl(courseDTO.getPreviewVideoUrl());
			existingCourse.setLevel(courseDTO.getLevel());
			existingCourse.setIsPro(courseDTO.getIsPro());
			existingCourse.setPrice(courseDTO.getPrice());
			existingCourse.setOriginalPrice(courseDTO.getOriginalPrice());
			existingCourse.setPriceType(courseDTO.getPriceType());
			existingCourse.setRating(courseDTO.getRating());
			existingCourse.setRatingCount(courseDTO.getRatingCount());
			existingCourse.setStudentCount(courseDTO.getStudentCount());
			existingCourse.setLessonCount(courseDTO.getLessonCount());
			existingCourse.setDurationText(courseDTO.getDurationText());

			// Cập nhật lại danh mục liên kết nếu có thay đổi
			if (courseDTO.getCategoryId() != null) {
				Optional<Category> catOpt = categoryRepository.findById(courseDTO.getCategoryId());
				if (catOpt.isPresent()) {
					existingCourse.setCategory(catOpt.get());
				}
			}

			// Cập nhật lại giảng viên phụ trách nếu có thay đổi
			if (courseDTO.getInstructorId() != null) {
				Optional<User> userOpt = userRepository.findById(courseDTO.getInstructorId());
				if (userOpt.isPresent()) {
					existingCourse.setInstructor(userOpt.get());
				}
			}

			// Lưu lại thực thể đã chỉnh sửa
			return courseRepository.save(existingCourse);
		}

		return null; // Trả về null nếu không tìm thấy khóa học để sửa
	}

	@Override
	public void deleteCourse(Short id) {
		// Tìm xem khóa học có tồn tại trước khi xóa không bằng Optional
		Optional<Course> optional = courseRepository.findById(id);
		if (optional.isPresent()) {
			// Thực hiện xóa bản ghi khỏi database
			courseRepository.delete(optional.get());
		}
	}

	/**
	 * Lưu dữ liệu gửi lên từ form admin (Thymeleaf).
	 *
	 * Form chỉ chứa một phần các trường của Course. Spring dựng object Course mới
	 * tinh từ dữ liệu form, nên những trường KHÔNG có trên form sẽ mang giá trị
	 * khởi tạo mặc định (0, 0.0). Nếu lưu thẳng object đó xuống DB thì rating,
	 * ratingCount và studentCount của khóa học bị xóa trắng — chỉ sửa mỗi cái tiêu
	 * đề cũng mất sạch đánh giá và số học viên.
	 *
	 * Vì vậy khi SỬA, ta nạp bản ghi cũ từ DB rồi chỉ chép đè đúng những trường mà
	 * form thực sự gửi lên. Khi THÊM MỚI thì lưu thẳng, vì chưa có gì để giữ.
	 */
	@Override
	public Course save(Course course) {
		if (course.getId() == null) {
			return courseRepository.save(course);
		}

		Optional<Course> optional = courseRepository.findById(course.getId());
		if (optional.isEmpty()) {
			return courseRepository.save(course);
		}

		Course existing = optional.get();

		existing.setTitle(course.getTitle());
		existing.setSlug(course.getSlug());
		existing.setSubtitle(course.getSubtitle());
		existing.setDescription(course.getDescription());
		existing.setThumbnailUrl(course.getThumbnailUrl());
		existing.setPreviewVideoUrl(course.getPreviewVideoUrl());
		existing.setLevel(course.getLevel());
		existing.setPriceType(course.getPriceType());
		existing.setPrice(course.getPrice());
		existing.setOriginalPrice(course.getOriginalPrice());
		existing.setDurationText(course.getDurationText());
		existing.setCategory(course.getCategory());
		existing.setInstructor(course.getInstructor());
		// Để trống ô này nghĩa là "gỡ liên kết với LMS", nên cho phép ghi đè bằng
		// null — khác với isPro và lessonCount ở dưới.
		existing.setMoodleCourseId(course.getMoodleCourseId());
		existing.setStatus(course.getStatus());

		// Hai trường này để trống trên form thì Spring bind thành null — kiểm tra
		// trước khi chép để không ghi null đè lên giá trị đang có.
		if (course.getIsPro() != null) {
			existing.setIsPro(course.getIsPro());
		}
		if (course.getLessonCount() != null) {
			existing.setLessonCount(course.getLessonCount());
		}

		// rating, ratingCount, studentCount: KHÔNG đụng tới. Đây là số liệu thống kê
		// tích lũy, không phải thứ admin gõ tay trên form.
		return courseRepository.save(existing);
	}
}
