import React from "react";
import { Navigate } from "react-router-dom";

/**
 * ProtectedRoute: Bảo vệ các route yêu cầu đăng nhập
 * - Kiểm tra token và user trong localStorage
 * - Nếu không có → redirect về trang chủ (mở modal login)
 * - Nếu có → render children bình thường
 */
export default function ProtectedRoute({ children }) {
  const token = localStorage.getItem("token");
  const user = localStorage.getItem("user");

  // Chưa đăng nhập → về trang chủ
  if (!token && !user) {
    return <Navigate to="/" replace />;
  }

  return children;
}
