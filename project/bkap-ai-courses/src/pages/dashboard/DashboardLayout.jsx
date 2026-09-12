import React, { useState } from "react";
import { NavLink, useNavigate, Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import {
  LayoutDashboard,
  BookOpen,
  UserCircle,
  Settings,
  LogOut,
  Menu,
  X,
  GraduationCap,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";

// ---- Danh sách menu sidebar ----
const MENU_ITEMS = [
  {
    label: "Tổng quan",
    icon: LayoutDashboard,
    path: "/dashboard",
    exact: true,
  },
  { label: "Khóa học của tôi", icon: BookOpen, path: "/dashboard/my-courses" },
  { label: "Hồ sơ cá nhân", icon: UserCircle, path: "/dashboard/profile" },
  { label: "Cài đặt", icon: Settings, path: "/dashboard/settings" },
];

// ---- Lấy chữ cái đầu tên user ----
function getInitials(name = "") {
  return name
    .split(" ")
    .map((w) => w[0])
    .slice(-2)
    .join("")
    .toUpperCase();
}

// ---- Sidebar Component (nội bộ) ----
function Sidebar({ onClose }) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  // Lấy thông tin user từ localStorage
  const savedUser = localStorage.getItem("user");
  const userData = savedUser ? JSON.parse(savedUser) : {};

  const userName = userData.name || userData.fullname || "Thành viên";
  const userEmail = userData.email || "";

  // Lấy role từ database (mặc định trả về STUDENT nếu chưa đăng nhập/không có data)
  const userRole = userData.role || "STUDENT";
  const initials = getInitials(userName);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    logout(); // Cập nhật AuthContext
    navigate("/");
  };

  return (
    <div
      className="flex flex-col h-full w-full"
      style={{ background: "#0F172A" }}
    >
      {/* Logo khu vực */}
      <div className="px-6 py-5 border-b border-white/10 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <div
            className="w-8 h-8 rounded-lg flex items-center justify-center"
            style={{ background: "#DC2626" }}
          >
            <GraduationCap className="w-4 h-4 text-white" />
          </div>
          <div className="leading-none">
            <span className="text-white font-extrabold text-base tracking-tight">
              BKAP AI
            </span>
            <span className="block text-[10px] text-white/40 font-medium tracking-widest uppercase">
              Portal
            </span>
          </div>
        </Link>
        {/* Nút đóng sidebar trên mobile */}
        {onClose && (
          <button
            onClick={onClose}
            className="text-white/50 hover:text-white lg:hidden"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Avatar + tên user */}
      <div className="px-6 py-6 border-b border-white/10">
        <div className="flex items-center gap-3">
          <div
            className="w-12 h-12 rounded-full flex items-center justify-center text-white font-extrabold text-lg flex-shrink-0"
            style={{ background: "#DC2626" }}
          >
            {initials}
          </div>
          <div className="overflow-hidden">
            <p className="text-white font-bold text-sm truncate">{userName}</p>
            <p className="text-white/40 text-xs truncate">
              {userEmail ||
                (userRole === "ADMIN" ? "Quản trị viên" : "Học viên")}
            </p>

            {/* Badge hiển thị Role động từ Database */}
            <span
              className="inline-block mt-1 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider"
              style={{
                background:
                  userRole === "ADMIN"
                    ? "rgba(220, 38, 38, 0.2)"
                    : "rgba(255, 255, 255, 0.1)",
                color: userRole === "ADMIN" ? "#FCA5A5" : "#94A3B8",
              }}
            >
              {userRole}
            </span>
          </div>
        </div>
      </div>

      {/* Menu items */}
      <nav className="flex-1 px-3 py-4 overflow-y-auto">
        <p className="px-3 mb-2 text-[10px] font-bold text-white/30 uppercase tracking-widest">
          Menu chính
        </p>
        <ul className="space-y-1">
          {MENU_ITEMS.map((item) => {
            const Icon = item.icon;
            return (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  end={item.exact}
                  onClick={onClose}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 ${
                      isActive
                        ? "bg-white/15 text-white shadow-sm"
                        : "text-white/50 hover:text-white hover:bg-white/8"
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      <Icon
                        className={`w-4.5 h-4.5 flex-shrink-0 ${
                          isActive ? "text-red-400" : ""
                        }`}
                        style={{ width: 18, height: 18 }}
                      />
                      <span>{item.label}</span>
                      {isActive && (
                        <span
                          className="ml-auto w-1.5 h-1.5 rounded-full"
                          style={{ background: "#DC2626" }}
                        />
                      )}
                    </>
                  )}
                </NavLink>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* Nút đăng xuất */}
      <div className="px-3 py-4 border-t border-white/10">
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-semibold text-red-400 hover:bg-red-500/15 transition-all duration-200"
        >
          <LogOut style={{ width: 18, height: 18 }} />
          <span>Đăng xuất</span>
        </button>
      </div>
    </div>
  );
}

/**
 * DashboardLayout: Layout 2 cột — Sidebar trái + Content area phải
 * - Desktop: sidebar cố định 260px
 * - Mobile: sidebar collapse/drawer
 */
export default function DashboardLayout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex min-h-screen" style={{ background: "#F8FAFC" }}>
      {/* ---- DESKTOP SIDEBAR (fixed) ---- */}
      <aside
        className="hidden lg:flex flex-col fixed left-0 top-0 bottom-0 z-40"
        style={{ width: 260, background: "#0F172A" }}
      >
        <Sidebar />
      </aside>

      {/* ---- MOBILE OVERLAY ---- */}
      <AnimatePresence>
        {sidebarOpen && (
          <>
            {/* Backdrop */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-40 bg-black/60 lg:hidden"
              onClick={() => setSidebarOpen(false)}
            />
            {/* Drawer */}
            <motion.aside
              initial={{ x: -260 }}
              animate={{ x: 0 }}
              exit={{ x: -260 }}
              transition={{ duration: 0.3, ease: "easeOut" }}
              className="fixed left-0 top-0 bottom-0 z-50 lg:hidden"
              style={{ width: 260 }}
            >
              <Sidebar onClose={() => setSidebarOpen(false)} />
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* ---- MAIN CONTENT AREA ---- */}
      <main
        className="flex-1 flex flex-col min-h-screen"
        style={{ marginLeft: 0 }}
      >
        {/* Mobile top bar */}
        <div className="lg:hidden flex items-center justify-between px-4 py-3 border-b border-gray-200 sticky top-0 z-30 bg-white">
          <button
            onClick={() => setSidebarOpen(true)}
            className="p-2 rounded-lg text-gray-600 hover:bg-gray-100 transition"
          >
            <Menu className="w-5 h-5" />
          </button>
          <span className="font-bold text-gray-800 text-sm">
            BKAP AI Portal
          </span>
          <div style={{ width: 36 }} />
        </div>

        {/* Page content với padding desktop (sidebar chiếm 260px) */}
        <div className="flex-1" style={{ paddingLeft: 0 }}>
          {/* Desktop left offset */}
          <div className="lg:ml-[260px]">
            <div className="p-6 md:p-8">{children}</div>
          </div>
        </div>
      </main>
    </div>
  );
}
