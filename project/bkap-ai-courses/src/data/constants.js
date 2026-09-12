// ── Cấu hình lấy từ biến môi trường (.env / .env.development / .env.production) ──
// Fallback là giá trị dùng khi build cùng origin với Spring Boot.
export const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || "/api/v1";
export const LMS_URL = process.env.REACT_APP_LMS_URL || "https://lms.buni.vn";

export const SITE_NAME = "BACHKHOA EDUCATION";
export const SITE_SLOGAN = "Đào tạo CNTT Quốc tế – Hơn 23 năm kinh nghiệm";

export const CONTACT_INFO = {
  address: "236 Hoàng Quốc Việt, Cầu Giấy, Hà Nội",
  phone: "0968.276.996",
  email: "tuyensinh@bachkhoa-aptech.edu.vn",
  facebook: "https://www.facebook.com/BachkhoaAptech236HoangQuocViet",
};

export const NAV_LINKS = [
  { label: "Trang chủ", path: "/" },
  {
    label: "Khóa học",
    path: "/courses",
    dropdown: [
      { label: "Tất cả khóa học", path: "/courses" },
      { label: "Full-Stack Developer", path: "/courses/full-stack" },
      { label: "Frontend Developer", path: "/courses/frontend" },
      { label: "AI & Data Science", path: "/courses/ai-data" },
    ],
  },
  {
    label: "Giới thiệu",
    path: "/about",
    dropdown: [
      { label: "Giới thiệu chung", path: "/about" },
      { label: "Thư viện ảnh", path: "/about#gallery" },
      { label: "Giảng viên", path: "/about#teachers" },
    ],
  },
  { label: "Liên hệ", path: "/contact" },
];

export const ROUTES = {
  HOME: "/",
  COURSES: "/courses",
  COURSE_DETAIL: "/courses/:id",
  ABOUT: "/about",
  CONTACT: "/contact",
  NOT_FOUND: "/not-found",
};

export const STAGGER_DELAY = 0.15;
export const ANIMATION_DURATION = 0.6;
