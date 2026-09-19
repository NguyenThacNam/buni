import React, { useState, useEffect, useRef, useMemo } from "react";
import { NavLink, Link, useLocation } from "react-router-dom";
import { Menu, X, ChevronDown, LogOut, User, BookOpen } from "lucide-react";
import { NAV_LINKS, SITE_NAME } from "../../data/constants";
import { useAuth } from "../../context/AuthContext";
import { getAllCategoriesApi } from "../../api/CourseApi";
import { dungCayDanhMuc } from "../../utils/categoryTree";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "react-toastify";
import ChuongThongBao from "./ChuongThongBao";

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const [categories, setCategories] = useState([]);

  // Menu "Khóa học": danh mục gốc, bên dưới thụt vào là danh mục con của nó.
  const dsMenuDanhMuc = useMemo(() => {
    const cay = dungCayDanhMuc(categories);
    return cay.goc.flatMap((g) => [{ ...g, doSau: 0 }, ...cay.conChau(g.id)]);
  }, [categories]);
  const userMenuRef = useRef(null);
  const location = useLocation();
  const { user, logout, openLogin } = useAuth();
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState(false);

  // Lấy role từ user context hoặc mặc định là STUDENT
  const userRole = user?.role || "STUDENT";

  // Close mobile drawer when location/page changes
  useEffect(() => {
    setIsOpen(false);
  }, [location]);

  // Fetch categories for dropdown
  useEffect(() => {
    getAllCategoriesApi()
      .then((res) => {
        const data = Array.isArray(res.data) ? res.data : [];
        setCategories(data);
      })
      .catch((err) => console.error("Navbar failed to load categories:", err));
  }, []);

  // Close user dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(e) {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target)) {
        setIsUserDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Avatar initials — fullname → name → username
  const getInitials = () => {
    const raw = user?.fullname || user?.name || user?.username || "";
    if (!raw) return "U";

    const parts = raw.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return "U";
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();

    // Lấy chữ cái đầu của từ đầu và từ cuối (Trần Thị Anh → TA)
    const firstInitial = parts[0][0];
    const lastInitial = parts[parts.length - 1][0];
    return (firstInitial + lastInitial).toUpperCase();
  };

  const handleLogout = () => {
    logout();
    toast.success("Đăng xuất thành công!", { icon: "👋" });
  };

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-gray-100 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20">
          {/* Logo & Branding */}
          <Link
            to="/"
            className="flex items-center space-x-2 focus-ring rounded-lg"
          >
            <span className="font-heading text-2xl font-extrabold text-primary tracking-tight">
              {SITE_NAME}
            </span>
            <span className="h-6 w-[2px] bg-gray-300 hidden sm:inline" />
            <span className="font-sans text-xs font-semibold text-gray-500 hidden sm:inline tracking-wider uppercase">
              APTECH
            </span>
          </Link>

          {/* Desktop Navigation menu */}
          <nav
            className="hidden lg:flex items-center space-x-8"
            aria-label="Menu chính"
          >
            {NAV_LINKS.map((link) => (
              <div key={link.path} className="relative group/nav">
                <NavLink
                  to={link.path}
                  className={({ isActive }) =>
                    `group flex items-center gap-1 font-sans text-sm font-semibold transition-colors focus-ring rounded px-2 py-1 ${
                      isActive
                        ? "text-primary"
                        : "text-gray-600 hover:text-primary"
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      {link.label}
                      {link.dropdown && (
                        <ChevronDown className="w-4 h-4 text-gray-400 transition-transform group-hover/nav:-rotate-180" />
                      )}
                      {/* Animated underline */}
                      <span
                        className={`absolute -bottom-1 left-0 h-0.5 bg-primary transition-all duration-300 ${
                          isActive ? "w-full" : "w-0 group-hover/nav:w-full"
                        }`}
                      />
                    </>
                  )}
                </NavLink>

                {/* Dropdown Menu */}
                {link.dropdown && (
                  <div className="absolute top-full left-0 mt-2 min-w-[14rem] w-max max-w-[22rem] opacity-0 invisible group-hover/nav:opacity-100 group-hover/nav:visible transition-all duration-300 transform translate-y-2 group-hover/nav:translate-y-0 z-50 pt-1">
                    <div className="bg-white rounded-xl shadow-[0_10px_40px_-10px_rgba(0,0,0,0.1)] border border-gray-100 py-2 flex flex-col relative overflow-hidden">
                      <div className="absolute top-0 left-0 w-full h-1 bg-primary"></div>
                      {link.label === "Khóa học" && categories.length > 0 ? (
                        <>
                          <Link
                            to="/courses"
                            className="px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-red-50 hover:text-primary transition-colors whitespace-nowrap block"
                          >
                            Tất cả khóa học
                          </Link>
                          {dsMenuDanhMuc.map((cat) => (
                            <Link
                              key={cat.slug}
                              to={`/courses?category=${cat.slug}`}
                              style={{ paddingLeft: `${1.25 + cat.doSau}rem` }}
                              className={`pr-5 text-gray-700 hover:bg-red-50 hover:text-primary transition-colors block ${
                                cat.doSau === 0
                                  ? "py-2.5 text-sm font-semibold"
                                  : "py-1.5 text-[13px] font-medium text-gray-500"
                              }`}
                            >
                              {cat.doSau > 0 && <span className="mr-1.5 text-gray-300">›</span>}
                              {cat.name}
                            </Link>
                          ))}
                        </>
                      ) : (
                        link.dropdown.map((dropItem) => (
                          <Link
                            key={dropItem.path}
                            to={dropItem.path}
                            className="px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-red-50 hover:text-primary transition-colors whitespace-nowrap block"
                          >
                            {dropItem.label}
                          </Link>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            ))}
          </nav>

          {/* Auth / User Area – Desktop */}
          <div className="hidden lg:flex items-center gap-3">
            {/* Chuông thông báo lấy từ LMS, chỉ có khi đã đăng nhập. */}
            {user && <ChuongThongBao />}
            {user ? (
              /* Logged-in: Unified Avatar dropdown */
              <div className="relative" ref={userMenuRef}>
                <button
                  onClick={() => setIsUserDropdownOpen((s) => !s)}
                  className="flex items-center gap-1.5 focus:outline-none group py-1"
                  aria-label="Tài khoản"
                >
                  {user.avatar ? (
                    <img
                      src={user.avatar}
                      alt={user.fullname || user.name || "Avatar"}
                      className="w-8 h-8 rounded-full object-cover ring-2 ring-primary/20"
                    />
                  ) : (
                    <div
                      className={`w-9 h-9 rounded-full flex items-center justify-center text-white font-bold text-xs shadow-sm border border-slate-200/50 group-hover:scale-105 transition-transform duration-200 ${
                        userRole === "ADMIN"
                          ? "bg-gradient-to-br from-red-500 to-orange-500"
                          : "bg-gradient-to-br from-red-500 to-rose-400"
                      }`}
                    >
                      {getInitials()}
                    </div>
                  )}

                  <ChevronDown
                    className={`w-4 h-4 text-gray-400 transition-transform duration-200 ${
                      isUserDropdownOpen ? "-rotate-180" : ""
                    }`}
                  />
                </button>

                {/* Dropdown Card */}
                <AnimatePresence>
                  {isUserDropdownOpen && (
                    <motion.div
                      initial={{ opacity: 0, y: 8 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: 8 }}
                      className="absolute right-0 mt-2.5 w-60 bg-white border border-slate-100 rounded-2xl shadow-xl py-3.5 z-50 overflow-hidden"
                    >
                      {/* User info header */}
                      <div className="px-4 py-2 border-b border-slate-50 pb-3">
                        <p className="text-[10px] font-extrabold text-slate-400 uppercase tracking-widest">
                          Tài khoản
                        </p>
                        <p className="text-sm font-bold text-slate-800 mt-1 truncate">
                          {user.fullname || user.name}
                        </p>
                        <p className="text-xs text-slate-400 truncate mt-0.5">
                          {user.email}
                        </p>

                        {/* Thay thế chữ Học viên / Quản trị viên cứng bằng badge ROLE từ database */}
                        <span
                          className={`inline-block mt-2.5 text-[9px] font-extrabold px-2.5 py-0.5 rounded-full uppercase tracking-wider ${
                            userRole === "ADMIN"
                              ? "bg-red-50 text-red-600 border border-red-100"
                              : "bg-slate-100 text-slate-600 border border-slate-200/60"
                          }`}
                        >
                          {userRole}
                        </span>
                      </div>

                      {/* Menu items */}
                      <div className="py-1 mt-1">
                        <Link
                          to="/dashboard/my-courses"
                          onClick={() => setIsUserDropdownOpen(false)}
                          className="flex items-center gap-3 px-4 py-2.5 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors font-semibold"
                        >
                          <BookOpen className="w-4 h-4 text-slate-400" />
                          {userRole === "ADMIN"
                            ? "Quản lý khóa học"
                            : "Khóa học của tôi"}
                        </Link>
                        <Link
                          to="/dashboard"
                          onClick={() => setIsUserDropdownOpen(false)}
                          className="flex items-center gap-3 px-4 py-2.5 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors font-semibold"
                        >
                          <User className="w-4 h-4 text-slate-400" />
                          Hồ sơ cá nhân
                        </Link>
                      </div>

                      {/* Logout */}
                      <div className="border-t border-slate-50 mt-1 pt-1">
                        <button
                          onClick={() => {
                            setIsUserDropdownOpen(false);
                            handleLogout();
                          }}
                          className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-500 hover:bg-red-50 hover:text-red-600 transition-colors font-bold"
                        >
                          <LogOut className="w-4 h-4 text-red-400" />
                          Đăng xuất
                        </button>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            ) : (
              /* Khách: chỉ còn nút Đăng nhập. Tự đăng ký tài khoản đã tắt —
                 trung tâm cấp tài khoản cho học viên. */
              <>
                <button
                  onClick={openLogin}
                  className="bg-primary hover:bg-primary-dark text-white font-heading text-sm font-bold px-5 py-2.5 rounded-btn shadow-btn hover:shadow-btn-hover transition-all duration-300"
                >
                  Đăng nhập
                </button>
                {/* <button
                  onClick={openRegister}
                  className="bg-primary hover:bg-primary-dark text-white font-heading text-sm font-bold px-5 py-2.5 rounded-btn shadow-btn hover:shadow-btn-hover transition-all duration-300"
                >
                  Đăng ký
                </button> */}
              </>
            )}
          </div>

          {/* Hamburger button (Mobile/Tablet) */}
          <div className="lg:hidden">
            <button
              onClick={() => setIsOpen(!isOpen)}
              type="button"
              className="p-2 text-gray-600 hover:text-primary focus-ring rounded-lg"
              aria-label={isOpen ? "Đóng menu" : "Mở menu"}
              aria-expanded={isOpen}
            >
              {isOpen ? (
                <X className="w-6 h-6" />
              ) : (
                <Menu className="w-6 h-6" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile/Tablet drawer overlay */}
      <div
        className={`lg:hidden fixed inset-x-0 top-20 bottom-0 z-40 bg-white border-t border-gray-100 transition-transform duration-300 ease-in-out overflow-y-auto ${
          isOpen ? "translate-x-0" : "translate-x-full"
        }`}
      >
        <div className="flex flex-col p-6 space-y-6">
          <nav className="flex flex-col space-y-4" aria-label="Menu di động">
            {NAV_LINKS.map((link) => (
              <div key={link.path} className="flex flex-col">
                <NavLink
                  to={link.path}
                  className={({ isActive }) =>
                    `font-sans text-lg font-bold transition-colors py-2 border-b border-gray-50 flex items-center justify-between ${
                      isActive
                        ? "text-primary"
                        : "text-gray-700 hover:text-primary"
                    }`
                  }
                >
                  {link.label}
                  {link.dropdown && (
                    <ChevronDown className="w-5 h-5 text-gray-400" />
                  )}
                </NavLink>

                {link.dropdown && (
                  <div className="flex flex-col pl-4 mt-2 space-y-3">
                    {link.label === "Khóa học" && categories.length > 0 ? (
                      <>
                        <Link
                          to="/courses"
                          className="text-gray-500 font-semibold text-sm hover:text-primary transition-colors py-1"
                          onClick={() => setIsOpen(false)}
                        >
                          Tất cả khóa học
                        </Link>
                        {dsMenuDanhMuc.map((cat) => (
                          <Link
                            key={cat.slug}
                            to={`/courses?category=${cat.slug}`}
                            style={{ paddingLeft: `${cat.doSau}rem` }}
                            className={`hover:text-primary transition-colors py-1 ${
                              cat.doSau === 0
                                ? "text-gray-500 font-semibold text-sm"
                                : "text-gray-400 font-medium text-[13px]"
                            }`}
                            onClick={() => setIsOpen(false)}
                          >
                            {cat.doSau > 0 && <span className="mr-1.5 text-gray-300">›</span>}
                            {cat.name}
                          </Link>
                        ))}
                      </>
                    ) : (
                      link.dropdown.map((dropItem) => (
                        <Link
                          key={dropItem.path}
                          to={dropItem.path}
                          className="text-gray-500 font-semibold text-sm hover:text-primary transition-colors py-1"
                          onClick={() => setIsOpen(false)}
                        >
                          {dropItem.label}
                        </Link>
                      ))
                    )}
                  </div>
                )}
              </div>
            ))}
          </nav>

          {/* Mobile Auth Buttons */}
          {user ? (
            <div className="flex flex-col gap-3 pt-4 border-t border-gray-100">
              <div className="flex items-center gap-3">
                {user.avatar ? (
                  <img
                    src={user.avatar}
                    alt={user.name}
                    className="w-10 h-10 rounded-full object-cover ring-2 ring-primary/20"
                  />
                ) : (
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-sm ${
                      userRole === "ADMIN"
                        ? "bg-gradient-to-br from-red-500 to-orange-500"
                        : "bg-gradient-to-br from-red-500 to-rose-400"
                    }`}
                  >
                    {getInitials()}
                  </div>
                )}
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-bold text-gray-900 text-sm">
                      {user.fullname || user.name || user.username}
                    </p>
                    {/* Badge hiển thị Role trên Mobile Drawer */}
                    <span
                      className={`text-[8px] font-extrabold px-2 py-0.5 rounded-full uppercase ${
                        userRole === "ADMIN"
                          ? "bg-red-50 text-red-600"
                          : "bg-slate-100 text-slate-500"
                      }`}
                    >
                      {userRole}
                    </span>
                  </div>
                  <p className="text-xs text-gray-400">{user.email}</p>
                </div>
              </div>
              <button
                onClick={() => {
                  logout();
                  setIsOpen(false);
                }}
                className="flex items-center gap-2 text-red-500 font-bold text-sm py-2 mt-2"
              >
                <LogOut className="w-4 h-4" /> Đăng xuất
              </button>
            </div>
          ) : (
            <div className="flex flex-col gap-3 pt-2 border-t border-gray-100">
              <button
                onClick={() => {
                  openLogin();
                  setIsOpen(false);
                }}
                className="w-full py-3 bg-primary text-white font-heading font-bold rounded-btn shadow-btn transition-all"
              >
                Đăng nhập
              </button>
              {/* Tự đăng ký tài khoản đã tắt — trung tâm cấp tài khoản.
              <button
                onClick={() => {
                  openRegister();
                  setIsOpen(false);
                }}
                className="w-full py-3 bg-primary text-white font-heading font-bold rounded-btn shadow-btn transition-all"
              >
                Đăng ký
              </button> */}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
