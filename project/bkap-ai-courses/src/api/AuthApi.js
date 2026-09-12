import { api } from "./Api";

/**
 * Báo server đăng xuất.
 *
 * Trước đây đăng xuất chỉ xóa localStorage phía trình duyệt, nên token vẫn hợp
 * lệ với server tới khi hết hạn — ai kịp sao chép ra là dùng tiếp được. Gọi
 * endpoint này để server thu hồi refresh token trong DB, đăng xuất mới có hiệu
 * lực thật.
 */
export const logoutApi = (username) => api("POST", "logout", { username });
