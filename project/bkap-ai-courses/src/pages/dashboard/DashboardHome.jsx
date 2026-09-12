import React from "react";
import { motion } from "framer-motion";
import {
  BookOpen,
  CheckCircle,
  TrendingUp,
  BarChart2,
  Sparkles,
} from "lucide-react";
import StatsCard from "../../components/dashboard/StatsCard";
import CourseProgressCard from "../../components/dashboard/CourseProgressCard";
import SuggestedCourseCard from "../../components/dashboard/SuggestedCourseCard";

// ---- Mock data khóa học đã đăng ký ----
const mockEnrolledCourses = [
  {
    id: 1,
    title: "ReactJS từ Zero đến Hero",
    instructor: "Nguyễn Đức Tài",
    thumbnail: "https://placehold.co/400x225/1e293b/white?text=ReactJS",
    progress: 75,
    totalLessons: 48,
    completedLessons: 36,
    lastAccessed: "Hôm nay",
    status: "Đang học",
    category: "Frontend",
  },
  {
    id: 2,
    title: "Spring Boot Backend từ A-Z",
    instructor: "Trần Minh Quân",
    thumbnail: "https://placehold.co/400x225/0f172a/white?text=Spring+Boot",
    progress: 100,
    totalLessons: 60,
    completedLessons: 60,
    lastAccessed: "3 ngày trước",
    status: "Hoàn thành",
    category: "Backend",
  },
  {
    id: 3,
    title: "Thiết kế UI/UX với Figma",
    instructor: "Lê Thị Hoa",
    thumbnail: "https://placehold.co/400x225/7c3aed/white?text=UI%2FUX+Figma",
    progress: 20,
    totalLessons: 35,
    completedLessons: 7,
    lastAccessed: "1 tuần trước",
    status: "Đang học",
    category: "Design",
  },
];

// ---- Mock data khóa học gợi ý ----
const suggestedCourses = [
  {
    id: 4,
    title: "Docker & Kubernetes DevOps",
    price: "2.990.000đ",
    rating: 4.8,
    students: 1240,
    thumbnail: "https://placehold.co/400x225/0ea5e9/white?text=DevOps",
    tag: "Phổ biến",
  },
  {
    id: 5,
    title: "Python AI & Machine Learning",
    price: "3.490.000đ",
    rating: 4.9,
    students: 890,
    thumbnail: "https://placehold.co/400x225/7c3aed/white?text=AI+Python",
    tag: "Mới nhất",
  },
  {
    id: 6,
    title: "Flutter Mobile App",
    price: "2.490.000đ",
    rating: 4.7,
    students: 650,
    thumbnail: "https://placehold.co/400x225/0891b2/white?text=Flutter",
    tag: "Hot",
  },
];

// ---- Tính tiến độ trung bình ----
const avgProgress = Math.round(
  mockEnrolledCourses.reduce((sum, c) => sum + c.progress, 0) /
    mockEnrolledCourses.length
);

const inProgressCount = mockEnrolledCourses.filter(
  (c) => c.status === "Đang học"
).length;

/**
 * DashboardHome: Trang Tổng Quan /dashboard
 * - Header chào mừng, 4 stats card, khóa học đã đăng ký, gợi ý
 */
export default function DashboardHome() {
  // Lấy tên user từ localStorage
  const savedUser = localStorage.getItem("user");
  const userData = savedUser ? JSON.parse(savedUser) : {};
  const userName = userData.name || userData.fullname || "Học viên";

  // Ngày hôm nay
  const today = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });

  // Config cho 4 stats cards
  const statsData = [
    {
      label: "Đã đăng ký",
      value: mockEnrolledCourses.length,
      icon: <BookOpen style={{ width: 22, height: 22, color: "#DC2626" }} />,
      iconBg: "#FEE2E2",
    },
    {
      label: "Hoàn thành",
      value: `${mockEnrolledCourses.filter((c) => c.status === "Hoàn thành").length} khóa`,
      icon: <CheckCircle style={{ width: 22, height: 22, color: "#166534" }} />,
      iconBg: "#DCFCE7",
    },
    {
      label: "Đang học",
      value: `${inProgressCount} khóa`,
      icon: <TrendingUp style={{ width: 22, height: 22, color: "#1D4ED8" }} />,
      iconBg: "#DBEAFE",
    },
    {
      label: "Tiến độ TB",
      value: `${avgProgress}%`,
      icon: <BarChart2 style={{ width: 22, height: 22, color: "#7C3AED" }} />,
      iconBg: "#EDE9FE",
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
      className="space-y-8"
    >
      {/* ---- A. HEADER CHÀO MỪNG ---- */}
      <div
        className="rounded-2xl p-6 md:p-8 text-white relative overflow-hidden"
        style={{
          background: "linear-gradient(135deg, #0F172A 0%, #1E293B 60%, #7c3aed22 100%)",
        }}
      >
        {/* Decoration blob */}
        <div
          className="absolute -right-16 -top-16 w-64 h-64 rounded-full opacity-10"
          style={{ background: "#DC2626" }}
        />
        <div
          className="absolute right-8 bottom-0 w-32 h-32 rounded-full opacity-5"
          style={{ background: "#7C3AED" }}
        />

        <p className="text-white/50 text-sm font-medium mb-1">{today}</p>
        <h1 className="text-2xl md:text-3xl font-extrabold mb-1">
          Chào mừng trở lại, {userName}! 👋
        </h1>
        <p className="text-white/60 text-sm">
          Bạn đang học{" "}
          <span className="text-white font-bold">{inProgressCount} khóa học</span>.
          Tiếp tục phát huy nhé!
        </p>
      </div>

      {/* ---- B. STATS CARDS ---- */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {statsData.map((stat, i) => (
          <StatsCard key={stat.label} {...stat} index={i} />
        ))}
      </div>

      {/* ---- C. KHÓA HỌC CỦA TÔI ---- */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-extrabold text-gray-800">
            📚 Khóa học của tôi
          </h2>
          <a
            href="/dashboard/my-courses"
            className="text-sm font-semibold text-red-600 hover:underline"
          >
            Xem tất cả →
          </a>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {mockEnrolledCourses.map((course) => (
            <CourseProgressCard key={course.id} course={course} />
          ))}
        </div>
      </section>

      {/* ---- D. KHÓA HỌC GỢI Ý ---- */}
      <section>
        <div className="flex items-center gap-2 mb-4">
          <Sparkles className="w-5 h-5 text-yellow-500" />
          <h2 className="text-lg font-extrabold text-gray-800">
            Gợi ý dành cho bạn
          </h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
          {suggestedCourses.map((course, i) => (
            <SuggestedCourseCard key={course.id} course={course} index={i} />
          ))}
        </div>
      </section>
    </motion.div>
  );
}
