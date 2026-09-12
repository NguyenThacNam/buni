package com.bkap.UserModule.backend.service;

import com.bkap.UserModule.dto.MoodleUserRequest;

/**
 * Interface định nghĩa các hành động tương tác với Moodle LMS. Tách interface
 * giúp dễ mock/test và tuân theo nguyên tắc DIP.
 *
 * 3 chức năng chính: 1. Tạo tài khoản Moodle khi user đăng ký BKAP AI 2. Ghi
 * danh user vào khóa học Moodle 3. Lấy URL auto-login để SSO từ BKAP AI sang
 * Moodle
 */
public interface IMoodleService {

	/**
	 * Tạo tài khoản mới trên Moodle thông qua API core_user_create_users. Được gọi
	 * ngay sau khi user đăng ký thành công trên BKAP AI.
	 *
	 * @param user DTO chứa thông tin user cần tạo
	 * @return JSON string từ Moodle (chứa id và username nếu thành công)
	 */
	String createMoodleUser(MoodleUserRequest user);

	/**
	 * Ghi danh một user vào một khóa học cụ thể trên Moodle. Gọi API
	 * enrol_manual_enrol_users.
	 *
	 * @param courseId     ID khóa học trên Moodle
	 * @param moodleUserId ID của user trên Moodle (lấy từ kết quả createMoodleUser)
	 * @return JSON string từ Moodle (null nếu thành công — đây là behavior của
	 *         Moodle)
	 */
	String enrolUserToCourse(int courseId, int moodleUserId);

	/**
	 * Gỡ ghi danh của một user khỏi khóa học trên Moodle. Gọi API
	 * enrol_manual_unenrol_users — hàm này phải được thêm vào external service
	 * của token, nếu không Moodle trả accessexception.
	 *
	 * Chỉ gỡ lượt ghi danh, KHÔNG xóa tài khoản và KHÔNG xóa bài làm cũ: Moodle
	 * vẫn giữ điểm, ghi danh lại là thấy lại.
	 *
	 * @return true nếu gỡ xong (kể cả khi người đó vốn không có trong khóa)
	 */
	boolean unenrolUserFromCourse(int courseId, int moodleUserId);

	/**
	 * Lấy URL auto-login để redirect user từ BKAP AI vào Moodle mà không cần nhập
	 * lại mật khẩu. Flow: username + password → /login/token.php → lấy token → tạo
	 * URL redirect.
	 *
	 * @param username username của user (giống nhau trên cả 2 hệ thống)
	 * @param password password plain text (Moodle yêu cầu plain text để xác thực)
	 * @return URL dạng https://lms.buni.vn/login/index.php?token=xxx hoặc null nếu
	 *         lỗi
	 */
	String getMoodleAutoLoginUrl(String username, String password);
}