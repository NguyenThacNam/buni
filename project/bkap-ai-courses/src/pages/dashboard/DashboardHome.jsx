import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  AlertCircle,
  BarChart2,
  BookOpen,
  CheckCircle,
  Sparkles,
  TrendingUp,
} from "lucide-react";

import StatsCard from "../../components/dashboard/StatsCard";
import { api } from "../../api/Api";
import { CONTACT_INFO } from "../../data/constants";

// ─── DỮ LIỆU GIẢ CŨ — ĐÃ THAY BẰNG DỮ LIỆU THẬT (18/09/2026) ─────────────────
// Trang này từng hiện 3 khóa bịa (ReactJS, Spring Boot, UI/UX Figma) với tiến độ
// 75/100/20%, và mục "Gợi ý dành cho bạn" kèm giá tiền — trung tâm không bán khóa
// trên web nên giá là sai. Giờ lấy từ /api/v1/learn/my-courses (khóa trung tâm đã
// cấp, tiến độ do LMS tính) và /api/v1/courses cho phần gợi ý.
//
// const mockEnrolledCourses = [
//   { id: 1, title: "ReactJS từ Zero đến Hero", instructor: "Nguyễn Đức Tài",
//     progress: 75, totalLessons: 48, completedLessons: 36, status: "Đang học" },
//   ... (Spring Boot 100%, UI/UX Figma 20%)
// ];
// const suggestedCourses = [
//   { id: 4, title: "Docker & Kubernetes DevOps", price: "2.990.000đ", rating: 4.8 },
//   ... (Python AI, Flutter)
// ];
//
// Hai component CourseProgressCard và SuggestedCourseCard cũng không dùng nữa: cả
// hai đòi những trường LMS không có (số bài đã học, ngày truy cập gần nhất, giá,
// đánh giá). Thẻ khóa học dựng ngay trong trang này, chỉ hiện những gì có thật.

const ANH_MAC_DINH = "https://placehold.co/400x225/1e293b/white?text=BKAP";

export default function DashboardHome() {
  const [khoaHoc, setKhoaHoc] = useState([]);
  const [goiY, setGoiY] = useState([]);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);

  const savedUser = localStorage.getItem("user");
  const userData = savedUser ? JSON.parse(savedUser) : {};
  const userName = userData.name || userData.fullname || "Học viên";

  const today = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });

  useEffect(() => {
    let conDung = true;
    api("get", "/learn/my-courses")
      .then((res) => {
        if (!conDung) return;
        const ds = res.data || [];
        setKhoaHoc(ds);

        // Gợi ý: các khóa trong danh mục mà tài khoản này chưa được cấp. Hỏng thì
        // thôi, phần gợi ý không đáng để làm hỏng cả trang.
        const daCo = new Set(ds.map((k) => k.courseId));
        api("get", "/courses")
          .then((r) => {
            if (conDung) {
              setGoiY((r.data || []).filter((k) => !daCo.has(k.id)).slice(0, 3));
            }
          })
          .catch(() => {});
      })
      .catch(() => conDung && setLoi("Không tải được khóa học của bạn. Vui lòng thử lại."))
      .finally(() => conDung && setDangTai(false));
    return () => {
      conDung = false;
    };
  }, []);

  const soXong = khoaHoc.filter((k) => k.status === "COMPLETED").length;
  const soDangHoc = khoaHoc.length - soXong;

  // Tiến độ trung bình chỉ tính trên khóa LMS đo được. Khóa chưa bật theo dõi
  // hoàn thành thì không có số, tính vào sẽ kéo trung bình xuống oan.
  const coTienDo = khoaHoc.filter((k) => k.progressPercent != null);
  const tienDoTb = coTienDo.length
    ? Math.round(coTienDo.reduce((t, k) => t + k.progressPercent, 0) / coTienDo.length)
    : null;

  const statsData = [
    {
      label: "Được cấp",
      value: `${khoaHoc.length} khóa`,
      icon: <BookOpen style={{ width: 22, height: 22, color: "#DC2626" }} />,
      iconBg: "#FEE2E2",
    },
    {
      label: "Hoàn thành",
      value: `${soXong} khóa`,
      icon: <CheckCircle style={{ width: 22, height: 22, color: "#166534" }} />,
      iconBg: "#DCFCE7",
    },
    {
      label: "Đang học",
      value: `${soDangHoc} khóa`,
      icon: <TrendingUp style={{ width: 22, height: 22, color: "#1D4ED8" }} />,
      iconBg: "#DBEAFE",
    },
    {
      label: "Tiến độ TB",
      value: tienDoTb === null ? "—" : `${tienDoTb}%`,
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
        className="relative overflow-hidden rounded-2xl p-6 text-white md:p-8"
        style={{
          background: "linear-gradient(135deg, #0F172A 0%, #1E293B 60%, #7c3aed22 100%)",
        }}
      >
        <div
          className="absolute -right-16 -top-16 h-64 w-64 rounded-full opacity-10"
          style={{ background: "#DC2626" }}
        />
        <div
          className="absolute bottom-0 right-8 h-32 w-32 rounded-full opacity-5"
          style={{ background: "#7C3AED" }}
        />

        <p className="mb-1 text-sm font-medium text-white/50">{today}</p>
        <h1 className="mb-1 text-2xl font-extrabold md:text-3xl">
          Chào mừng trở lại, {userName}! 👋
        </h1>
        <p className="text-sm text-white/60">
          {dangTai
            ? "Đang tải khóa học của bạn..."
            : khoaHoc.length === 0
              ? "Tài khoản của bạn chưa được cấp khóa học nào."
              : soDangHoc > 0
                ? `Bạn đang học ${soDangHoc} khóa học. Tiếp tục phát huy nhé!`
                : "Bạn đã hoàn thành tất cả khóa học được cấp. Tuyệt vời!"}
        </p>
      </div>

      {/* ---- B. STATS CARDS ---- */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {statsData.map((stat, i) => (
          <StatsCard key={stat.label} {...stat} index={i} />
        ))}
      </div>

      {/* ---- C. KHÓA HỌC CỦA TÔI ---- */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-extrabold text-gray-800">📚 Khóa học của tôi</h2>
          <Link
            to="/dashboard/my-courses"
            className="text-sm font-semibold text-red-600 hover:underline"
          >
            Xem tất cả →
          </Link>
        </div>

        {dangTai ? (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-64 animate-pulse rounded-2xl bg-gray-100" />
            ))}
          </div>
        ) : loi ? (
          <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
            <AlertCircle className="mx-auto mb-3 h-10 w-10 text-red-600" strokeWidth={1.4} />
            <p className="text-sm text-gray-600">{loi}</p>
          </div>
        ) : khoaHoc.length === 0 ? (
          <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
            <BookOpen className="mx-auto mb-3 h-10 w-10 text-gray-300" strokeWidth={1.4} />
            <p className="mb-1 font-bold text-gray-800">Chưa có khóa học nào</p>
            <p className="text-sm text-gray-500">
              Khóa học do trung tâm cấp. Gọi{" "}
              <a
                href={`tel:${CONTACT_INFO.phone.replace(/\./g, "")}`}
                className="font-semibold text-red-600 hover:underline"
              >
                {CONTACT_INFO.phone}
              </a>{" "}
              để được mở khóa học.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {khoaHoc.slice(0, 3).map((k, i) => {
              const xong = k.status === "COMPLETED";
              const tienDo = k.progressPercent;
              return (
                <motion.div
                  key={k.courseId}
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.4, delay: i * 0.08, ease: "easeOut" }}
                  className="flex flex-col overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm transition-shadow duration-300 hover:shadow-lg"
                >
                  <div className="relative">
                    <img
                      src={k.thumbnailUrl || ANH_MAC_DINH}
                      alt={k.title}
                      className="h-44 w-full object-cover"
                    />
                    <span
                      className="absolute right-3 top-3 rounded-full px-2.5 py-1 text-xs font-bold"
                      style={
                        xong
                          ? { background: "#DCFCE7", color: "#166534" }
                          : { background: "#DBEAFE", color: "#1D4ED8" }
                      }
                    >
                      {k.statusLabel || (xong ? "Hoàn thành" : "Đang học")}
                    </span>
                  </div>

                  <div className="flex flex-1 flex-col gap-2 p-4">
                    {k.category && (
                      <span className="self-start rounded-md bg-gray-100 px-2.5 py-1 text-xs font-bold text-gray-700">
                        {k.category}
                      </span>
                    )}
                    <h3 className="line-clamp-2 text-sm font-bold leading-snug text-gray-800">
                      {k.title}
                    </h3>

                    {/* Khóa chưa bật theo dõi hoàn thành bên LMS thì không có số —
                        ẩn hẳn thanh tiến độ thay vì hiện 0%. */}
                    <div className="mt-1" hidden={tienDo == null}>
                      <div className="mb-1 flex items-center justify-between">
                        <span className="text-xs text-gray-500">Tiến độ</span>
                        <span className="text-xs font-bold text-red-600">{tienDo}%</span>
                      </div>
                      <div className="h-2 w-full overflow-hidden rounded-full bg-gray-100">
                        <motion.div
                          className="h-full rounded-full"
                          style={{ background: xong ? "#22C55E" : "#DC2626" }}
                          initial={{ width: 0 }}
                          animate={{ width: `${tienDo || 0}%` }}
                          transition={{ duration: 0.8, ease: "easeOut", delay: 0.2 }}
                        />
                      </div>
                    </div>

                    <div className="mt-auto flex items-center justify-end pt-2">
                      <Link
                        to={`/hoc/${k.courseId}`}
                        className="rounded-lg px-3 py-1.5 text-xs font-bold"
                        style={
                          xong
                            ? { background: "#F0FDF4", color: "#166534", border: "1px solid #BBF7D0" }
                            : { background: "#DC2626", color: "#fff" }
                        }
                      >
                        {xong ? "Xem lại" : "Vào học →"}
                      </Link>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </section>

      {/* ---- D. KHÓA HỌC KHÁC CỦA TRUNG TÂM ---- */}
      {goiY.length > 0 && (
        <section>
          <div className="mb-4 flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-yellow-500" />
            <h2 className="text-lg font-extrabold text-gray-800">
              Khóa học khác của trung tâm
            </h2>
          </div>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {goiY.map((k, i) => (
              <motion.div
                key={k.id}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: i * 0.08, ease: "easeOut" }}
                className="flex flex-col overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm transition-shadow duration-300 hover:shadow-lg"
              >
                <img
                  src={k.thumbnailUrl || ANH_MAC_DINH}
                  alt={k.title}
                  className="h-40 w-full object-cover"
                />
                <div className="flex flex-1 flex-col gap-2 p-4">
                  {k.category?.name && (
                    <span className="self-start rounded-md bg-gray-100 px-2.5 py-1 text-xs font-bold text-gray-700">
                      {k.category.name}
                    </span>
                  )}
                  <h3 className="line-clamp-2 text-sm font-bold leading-snug text-gray-800">
                    {k.title}
                  </h3>
                  <Link
                    to={`/courses/${k.id}`}
                    className="mt-auto self-start text-xs font-bold text-red-600 hover:underline"
                  >
                    Xem chi tiết →
                  </Link>
                </div>
              </motion.div>
            ))}
          </div>
        </section>
      )}
    </motion.div>
  );
}
