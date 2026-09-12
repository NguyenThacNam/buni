import axios from "axios";
import { API_BASE_URL } from "../data/constants";

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "content-type": "application/json",
  },
});

// Tự động gắn JWT token vào header Authorization nếu đã đăng nhập
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Sự kiện phát ra khi không thể làm mới token nữa (refresh token hết hạn hoặc
 * đã bị thu hồi). AuthContext lắng nghe để dọn state và hiện lại nút đăng nhập.
 */
export const SU_KIEN_HET_PHIEN = "bkap:phien-het-han";

// Cờ và hàng đợi cho việc làm mới token.
// Vì sao cần: một trang thường bắn nhiều request cùng lúc. Access token hết hạn
// thì TẤT CẢ cùng nhận 401 một lượt. Không có cờ này thì mỗi request lại gọi
// /auth/refresh một lần — mà server có xoay vòng token, nên lần refresh thứ hai
// dùng token đã bị thu hồi và thất bại, đá người dùng ra ngoài oan.
// Cách xử lý: request đầu tiên đi làm mới, các request sau xếp hàng đợi kết quả.
let dangLamMoi = false;
let hangCho = [];

function giaiPhongHangCho(loi, tokenMoi) {
  hangCho.forEach(({ resolve, reject }) => {
    if (loi) {
      reject(loi);
    } else {
      resolve(tokenMoi);
    }
  });
  hangCho = [];
}

function xoaPhienCucBo() {
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
  sessionStorage.removeItem("_mp");
  window.dispatchEvent(new Event(SU_KIEN_HET_PHIEN));
}

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const requestGoc = error.config;
    const status = error.response?.status;

    // Chỉ xử lý đúng 401 (chưa xác thực). 403 là "đã đăng nhập nhưng không đủ
    // quyền" — làm mới token cũng vô ích nên để nguyên cho phía gọi xử lý.
    if (status !== 401 || !requestGoc || requestGoc._daThuLai) {
      return Promise.reject(error);
    }

    // Chính lời gọi làm mới mà cũng 401 thì hết đường, đừng đệ quy.
    if (requestGoc.url?.includes("auth/refresh")) {
      xoaPhienCucBo();
      return Promise.reject(error);
    }

    const refreshToken = localStorage.getItem("refreshToken");
    if (!refreshToken) {
      xoaPhienCucBo();
      return Promise.reject(error);
    }

    // Đã có người đi làm mới rồi thì xếp hàng đợi, đừng gọi thêm.
    if (dangLamMoi) {
      return new Promise((resolve, reject) => {
        hangCho.push({ resolve, reject });
      }).then((tokenMoi) => {
        requestGoc._daThuLai = true;
        requestGoc.headers.Authorization = `Bearer ${tokenMoi}`;
        return axiosClient(requestGoc);
      });
    }

    requestGoc._daThuLai = true;
    dangLamMoi = true;

    try {
      // Dùng axios gốc chứ KHÔNG dùng axiosClient: đi qua axiosClient là lại
      // chui vào chính interceptor này, gặp lỗi sẽ lặp vô hạn.
      const res = await axios.post(
        `${API_BASE_URL}/auth/refresh`,
        { refreshToken },
        { headers: { "content-type": "application/json" } },
      );

      const tokenMoi = res.data.token;
      localStorage.setItem("token", tokenMoi);

      // Server xoay vòng refresh token: cái cũ vừa bị thu hồi, BẮT BUỘC phải
      // thay bằng cái mới, không thì lần làm mới sau sẽ hỏng.
      if (res.data.refreshToken) {
        localStorage.setItem("refreshToken", res.data.refreshToken);
      }

      giaiPhongHangCho(null, tokenMoi);

      requestGoc.headers.Authorization = `Bearer ${tokenMoi}`;
      return axiosClient(requestGoc);
    } catch (loiLamMoi) {
      giaiPhongHangCho(loiLamMoi, null);
      xoaPhienCucBo();
      return Promise.reject(loiLamMoi);
    } finally {
      dangLamMoi = false;
    }
  },
);

export const api = (method, endpoint, payload) => {
  return axiosClient({
    method: method,
    url: endpoint,
    data: payload,
  })
    .then((response) => response) // Trả về toàn bộ response để lấy được status nếu cần
    .catch((error) => {
      console.error("API Error:", error);
      throw error; // Quăng lỗi để phía UI xử lý
    });
};
