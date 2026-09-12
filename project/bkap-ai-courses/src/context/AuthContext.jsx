import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
} from "react";
import { loginApi } from "../api/LoginApi";
// Tự đăng ký tài khoản đã tắt — trung tâm cấp tài khoản cho học viên.
// import { registerApi } from "../api/RegisterApi";
import { logoutApi } from "../api/AuthApi";
import { SU_KIEN_HET_PHIEN } from "../api/Api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [authModal, setAuthModal] = useState(null); // "login" | "register" | null

  /**
   * HÀM HELPER: Chuẩn hóa dữ liệu user và lưu vào localStorage
   * Tách ra để dùng chung cho cả Login và Register
   */
  const handleAuthSuccess = useCallback((serverUser) => {
    // Không console.log serverUser: trong đó có access token và refresh token.
    // Ai mở DevTools đọc được là chiếm được phiên đăng nhập.

    // 1. Lưu token
    if (serverUser.token) {
      localStorage.setItem("token", serverUser.token);
    }

    // Access token chỉ sống 15 phút. Refresh token là thứ giữ cho người dùng
    // không bị văng ra giữa chừng — Api.js dùng nó để tự xin access token mới.
    if (serverUser.refreshToken) {
      localStorage.setItem("refreshToken", serverUser.refreshToken);
    }

    // KHÔNG giữ mật khẩu ở trình duyệt, dưới bất kỳ hình thức nào.
    //
    // Trước đây mật khẩu gốc được cất vào sessionStorage để nút "Đăng ký học"
    // gửi lại lên server tạo tài khoản Moodle. Nay server tự xác định người dùng
    // từ token và tự sinh mật khẩu cho Moodle, nên trình duyệt không cần giữ nữa.

    const authenticatedUser = {
      ...serverUser,
      name: serverUser.user?.name || serverUser.user?.username,
      email: serverUser.user?.email,
      username: serverUser.user?.username,
      fullname: serverUser.user?.name || serverUser.user?.fullname,
      avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(
        serverUser.user?.name || serverUser.user?.username || "User",
      )}&background=C8102E&color=fff`,
    };

    localStorage.setItem("user", JSON.stringify(authenticatedUser));
    setUser(authenticatedUser);
    setAuthModal(null);
  }, []);

  /**
   * HÀM LOGIN: Đăng nhập bằng username + mật khẩu
   */
  const login = useCallback(
    async (username, password) => {
      try {
        const response = await loginApi(username, password);
        if (response && response.data) {
          // Gọi helper xử lý lưu trữ dữ liệu đăng nhập
          handleAuthSuccess(response.data);
          return { success: true };
        }
      } catch (error) {
        return {
          success: false,
          message: "Tên đăng nhập hoặc mật khẩu không đúng!",
        };
      }
    },
    [handleAuthSuccess],
  );

  // ─── ĐĂNG KÝ TÀI KHOẢN — ĐÃ TẮT (11/09/2026) ─────────────────────────────
  // Trung tâm cấp tài khoản ở /admin/user; endpoint /api/v1/usersRegister
  // phía backend cũng đã tắt.
  // /**
  //  * HÀM REGISTER: Đăng ký thành viên mới
  //  * ✅ ĐÃ CẬP NHẬT: Đăng ký thành công sẽ tự động đăng nhập luôn
  //  */
  // const register = useCallback(
  //   async (fullName, username, email, phone, birthday, password) => {
  //     try {
  //       const response = await registerApi(
  //         fullName,
  //         username,
  //         email,
  //         phone,
  //         birthday,
  //         password,
  //       );

  //       // Theo hình Postman 1 và 3, API Register trả về HTTP Status 201 cùng dữ liệu token/user giống API Login
  //       if (response && (response.status === 201 || response.data)) {
  //         const serverUser = response.data;

  //         // Tiến hành lưu token, user vào localStorage và đăng nhập ngay lập tức
  //         handleAuthSuccess(serverUser);

  //         return { success: true };
  //       }
  //       return {
  //         success: false,
  //         message: "Đăng ký không thành công. Vui lòng thử lại.",
  //       };
  //     } catch (error) {
  //       console.error("Register Error:", error);
  //       return {
  //         success: false,
  //         message:
  //           error.response?.data?.message ||
  //           "Tên đăng nhập hoặc Email đã tồn tại!",
  //       };
  //     }
  //   },
  //   [handleAuthSuccess],
  // );

  // Khôi phục user + token từ localStorage khi reload trang
  useEffect(() => {
    // Dọn mật khẩu mà bản cũ đã cất vào sessionStorage. Người dùng đang mở tab
    // từ trước khi cập nhật vẫn còn giữ nó — xóa ngay lần tải đầu tiên.
    sessionStorage.removeItem("_mp");

    const savedUser = localStorage.getItem("user");
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      delete parsedUser.password;
      setUser(parsedUser);
    }
  }, []);

  const logout = useCallback(async () => {
    // Báo server thu hồi refresh token trước. Bỏ qua lỗi: mạng hỏng hay token
    // đã hết hạn thì vẫn phải dọn sạch phía trình duyệt, không được để người
    // dùng mắc kẹt ở trạng thái nửa vời.
    try {
      const username = user?.username;
      if (username) {
        await logoutApi(username);
      }
    } catch (error) {
      console.warn("Không gọi được API đăng xuất, vẫn dọn phiên cục bộ.", error);
    }

    localStorage.removeItem("user");
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    sessionStorage.removeItem("_mp");
    setUser(null);
  }, [user]);

  // Api.js phát sự kiện này khi refresh token cũng hết hạn hoặc đã bị thu hồi,
  // nghĩa là không cách nào lấy access token mới nữa. Nó đã tự dọn localStorage,
  // ở đây chỉ cần đưa giao diện về trạng thái chưa đăng nhập.
  useEffect(() => {
    const xuLyHetPhien = () => setUser(null);
    window.addEventListener(SU_KIEN_HET_PHIEN, xuLyHetPhien);
    return () => window.removeEventListener(SU_KIEN_HET_PHIEN, xuLyHetPhien);
  }, []);

  const openLogin = useCallback(() => setAuthModal("login"), []);
  // const openRegister = useCallback(() => setAuthModal("register"), []);
  const closeModal = useCallback(() => setAuthModal(null), []);

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        authModal,
        openLogin,
        closeModal,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
