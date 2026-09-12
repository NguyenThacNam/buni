package com.bkap.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nhận file ảnh admin tải lên, cất vào ổ đĩa và trả về đường dẫn công khai.
 *
 * Vì sao lưu ra thư mục ngoài chứ không phải trong project:
 *
 * 1. resources/static/ bị script deploy frontend xoá sạch mỗi lần build React.
 * 2. Khi chạy bằng file .jar thì nội dung jar là chỉ đọc, không ghi vào được.
 *
 * Nên đường dẫn lưu là một thư mục thật trên máy chủ, khai bằng app.upload.dir.
 * Thư mục này nằm ngoài mã nguồn nên nhớ giữ lại khi deploy bản mới, và nên
 * backup cùng với database.
 */
@Service
public class FileStorageService {

	/** Chỉ nhận đúng mấy đuôi ảnh này. Không có đuôi lạ nào lọt qua. */
	private static final Set<String> DUOI_CHO_PHEP = Set.of("jpg", "jpeg", "png", "webp", "gif");

	private static final Set<String> KIEU_MIME_CHO_PHEP = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

	private final Path thuMucLuu;

	/** Tiền tố URL để trình duyệt gọi lại file, khớp với WebMvcConfig. */
	public static final String DUONG_DAN_CONG_KHAI = "/uploads/";

	public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
		this.thuMucLuu = Paths.get(uploadDir).toAbsolutePath().normalize();
	}

	/**
	 * Lưu file và trả về đường dẫn dùng được trong thẻ img, ví dụ
	 * /uploads/9f2c1a....jpg. Trả null nếu không có file nào được chọn.
	 *
	 * @throws IOException khi file sai định dạng hoặc ghi đĩa thất bại
	 */
	public String luuAnh(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			return null;
		}

		String duoi = layDuoiFile(file.getOriginalFilename());
		if (!DUOI_CHO_PHEP.contains(duoi)) {
			throw new IOException("Chỉ nhận ảnh có đuôi: " + String.join(", ", DUOI_CHO_PHEP));
		}

		// Kiểm tra thêm kiểu MIME, vì đuôi file thì ai cũng đổi được
		String mime = file.getContentType();
		if (mime == null || !KIEU_MIME_CHO_PHEP.contains(mime.toLowerCase(Locale.ROOT))) {
			throw new IOException("File tải lên không phải ảnh hợp lệ.");
		}

		// Tên file do người dùng đặt KHÔNG được dùng lại: có thể chứa ../ để ghi đè
		// file ngoài thư mục, hoặc trùng tên làm mất ảnh cũ. Sinh tên ngẫu nhiên.
		String tenMoi = UUID.randomUUID().toString().replace("-", "") + "." + duoi;

		Files.createDirectories(thuMucLuu);
		Path dich = thuMucLuu.resolve(tenMoi).normalize();

		// Chốt chặn cuối: đường dẫn đích bắt buộc phải nằm trong thư mục lưu
		if (!dich.startsWith(thuMucLuu)) {
			throw new IOException("Đường dẫn lưu file không hợp lệ.");
		}

		try (var input = file.getInputStream()) {
			Files.copy(input, dich, StandardCopyOption.REPLACE_EXISTING);
		}

		return DUONG_DAN_CONG_KHAI + tenMoi;
	}

	/** Thư mục lưu file, để WebMvcConfig biết chỗ mà phục vụ ra ngoài. */
	public Path getThuMucLuu() {
		return thuMucLuu;
	}

	private String layDuoiFile(String tenGoc) {
		if (tenGoc == null) {
			return "";
		}
		int viTri = tenGoc.lastIndexOf('.');
		if (viTri < 0 || viTri == tenGoc.length() - 1) {
			return "";
		}
		return tenGoc.substring(viTri + 1).toLowerCase(Locale.ROOT);
	}
}
