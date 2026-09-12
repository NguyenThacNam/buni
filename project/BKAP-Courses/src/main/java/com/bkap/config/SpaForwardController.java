package com.bkap.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Bản build React nằm trong src/main/resources/static và dùng BrowserRouter,
 * nghĩa là các đường dẫn như /courses/5 hay /dashboard/profile chỉ tồn tại phía
 * trình duyệt — Spring MVC không có handler nào khớp.
 *
 * Nếu người dùng F5 hoặc dán thẳng link vào thanh địa chỉ, request sẽ đi tới
 * server và nhận 404. Controller này forward các route đó về /index.html để
 * React Router tự dựng lại màn hình tương ứng.
 *
 * Chỉ liệt kê đúng các route public của React (xem App.jsx). Không dùng
 * wildcard toàn cục để /api/** và /admin/** vẫn do controller thật xử lý.
 */
@Controller
public class SpaForwardController {

	@GetMapping({ "/", "/courses/**", "/about", "/contact", "/not-found", "/checkout/**", "/payment/**",
			"/dashboard/**" })
	public String forwardToSpa() {
		return "forward:/index.html";
	}
}
