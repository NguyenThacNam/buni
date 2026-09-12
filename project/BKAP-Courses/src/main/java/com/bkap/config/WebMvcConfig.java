package com.bkap.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Phục vụ ảnh admin tải lên ra ngoài web.
 *
 * Ảnh nằm ở một thư mục thật trên ổ đĩa (app.upload.dir), không nằm trong jar
 * cũng không nằm trong resources/static, nên Spring không tự biết mà phục vụ.
 * Cần khai tay: /uploads/** trỏ tới thư mục đó.
 *
 * Lưu ý: KHÔNG dùng @EnableWebMvc ở đây. Chỉ implements WebMvcConfigurer thì
 * cấu hình này được cộng thêm vào phần Spring Boot tự dựng sẵn; thêm
 * @EnableWebMvc là tắt hết auto-config, kéo theo mất luôn phần phục vụ bản build
 * React trong static/.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Autowired
	private FileStorageService fileStorageService;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// toUri() cho ra dạng file:///... đúng chuẩn trên cả Windows lẫn Linux,
		// an toàn hơn là tự nối chuỗi "file:" + đường dẫn.
		String viTri = fileStorageService.getThuMucLuu().toUri().toString();

		registry.addResourceHandler(FileStorageService.DUONG_DAN_CONG_KHAI + "**").addResourceLocations(viTri);
	}
}
