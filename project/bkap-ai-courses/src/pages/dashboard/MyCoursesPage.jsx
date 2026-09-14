import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { AlertCircle, Filter, Search } from "lucide-react";

import { api } from "../../api/Api";
import { CONTACT_INFO } from "../../data/constants";

// ─── DỮ LIỆU GIẢ CŨ — ĐÃ THAY BẰNG DỮ LIỆU THẬT (11/09/2026) ─────────────────
// Trang giờ lấy từ /api/v1/learn/my-courses: đúng những khóa admin đã ghi
// danh cho tài khoản này ở /admin/enrollment.
//
// const mockEnrolledCourses = [
//   { id: 1, title: "ReactJS từ Zero đến Hero", instructor: "Nguyễn Đức Tài",
//     thumbnail: "https://placehold.co/400x225/1e293b/white?text=ReactJS",
//     progress: 75, totalLessons: 48, completedLessons: 36,
//     lastAccessed: "Hôm nay", status: "Đang học", category: "Frontend" },
//   { id: 2, title: "Spring Boot Backend từ A-Z", instructor: "Trần Minh Quân",
//     thumbnail: "https://placehold.co/400x225/0f172a/white?text=Spring+Boot",
//     progress: 100, totalLessons: 60, completedLessons: 60,
//     lastAccessed: "3 ngày trước", status: "Hoàn thành", category: "Backend" },
//   { id: 3, title: "Thiết kế UI/UX với Figma", instructor: "Lê Thị Hoa",
//     thumbnail: "https://placehold.co/400x225/7c3aed/white?text=UI%2FUX+Figma",
//     progress: 20, totalLessons: 35, completedLessons: 7,
//     lastAccessed: "1 tuần trước", status: "Đang học", category: "Design" },
// ];

const TABS = ["Tất cả", "Đang học", "Hoàn thành"];

/** Ngày dạng 11/9/2026 từ chuỗi ISO của server. */
const doiNgay = (iso) => {
  if (!iso) return "";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? "" : d.toLocaleDateString("vi-VN");
};

/**
 * MyCoursesPage: Trang Khóa học của tôi /dashboard/my-courses
 *
 * Chỉ hiện khóa mà trung tâm đã cấp. Học viên không tự ghi danh được, nên
 * danh sách trống thì chỉ đường liên hệ trung tâm thay vì mời "khám phá khóa học".
 */
export default function MyCoursesPage() {
  const [activeTab, setActiveTab] = useState("Tất cả");
  const [searchQuery, setSearchQuery] = useState("");
  const [khoaHoc, setKhoaHoc] = useState([]);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);

  useEffect(() => {
    api("get", "/learn/my-courses")
      .then((res) => setKhoaHoc(res.data || []))
      .catch(() => setLoi("Không tải được danh sách khóa học. Vui lòng thử lại."))
      .finally(() => setDangTai(false));
  }, []);

  // Lọc theo tab và ô tìm kiếm
  const filteredCourses = khoaHoc.filter((course) => {
    const xong = course.status === "COMPLETED";
    const matchTab =
      activeTab === "Tất cả" ||
      (activeTab === "Hoàn thành" && xong) ||
      (activeTab === "Đang học" && !xong);
    const matchSearch = (course.title || "")
      .toLowerCase()
      .includes(searchQuery.toLowerCase());
    return matchTab && matchSearch;
  });

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
      className="space-y-6"
    >
      {/* ---- Tiêu đề ---- */}
      <div>
        <h1 className="text-2xl font-extrabold text-gray-800 mb-1">
          📚 Khóa học của tôi
        </h1>
        <p className="text-gray-400 text-sm">
          Các khóa trung tâm đã cấp cho tài khoản của bạn
        </p>
      </div>

      {/* ---- Thanh lọc & Tìm kiếm ---- */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="flex bg-gray-100 p-1 rounded-xl gap-1">
          {TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className="px-4 py-1.5 rounded-lg text-sm font-semibold transition-all duration-200"
              style={
                activeTab === tab
                  ? { background: "#DC2626", color: "#fff" }
                  : { background: "transparent", color: "#64748B" }
              }
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="relative flex-1">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            style={{ width: 16, height: 16 }}
          />
          <input
            type="text"
            placeholder="Tìm kiếm khóa học..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl border border-gray-200 text-sm text-gray-700 focus:outline-none focus:border-red-400 focus:ring-2 focus:ring-red-100 bg-white transition-all"
          />
        </div>
      </div>

      {/* ---- Danh sách khóa học ---- */}
      {dangTai ? (
        <div className="flex justify-center py-16">
          <div className="w-8 h-8 border-4 border-red-600 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : loi ? (
        <div className="text-center py-16 text-gray-500">
          <AlertCircle className="w-12 h-12 mx-auto mb-3 text-red-400" />
          <p className="font-semibold">{loi}</p>
        </div>
      ) : khoaHoc.length === 0 ? (
        // Chưa được cấp khóa nào: không có nút tự ghi danh, nên chỉ đường liên hệ.
        <div className="text-center py-16 text-gray-500">
          <Filter className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p className="font-semibold mb-1">Bạn chưa được ghi danh khóa học nào</p>
          <p className="text-sm">
            Liên hệ trung tâm để được cấp khóa học:{" "}
            <a
              href={`tel:${CONTACT_INFO.phone.replace(/\./g, "")}`}
              className="font-semibold text-red-600 hover:underline"
            >
              {CONTACT_INFO.phone}
            </a>
          </p>
        </div>
      ) : filteredCourses.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Filter className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p className="font-semibold">Không tìm thấy khóa học nào</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredCourses.map((course, index) => {
            const isCompleted = course.status === "COMPLETED";
            const tienDo = course.progressPercent || 0;

            return (
              <motion.div
                key={course.courseId}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.35, delay: index * 0.08, ease: "easeOut" }}
                className="bg-white rounded-2xl p-4 md:p-5 shadow-sm border border-gray-100 flex gap-4 hover:shadow-md transition-shadow duration-300"
              >
                <div className="flex-shrink-0">
                  <img
                    src={
                      course.thumbnailUrl ||
                      "https://placehold.co/400x225/1e293b/white?text=BKAP"
                    }
                    alt={course.title}
                    className="w-28 h-20 md:w-36 md:h-24 object-cover rounded-xl"
                  />
                </div>

                <div className="flex-1 min-w-0 flex flex-col gap-1.5">
                  <div className="flex items-center gap-2 flex-wrap">
                    {course.category && (
                      <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-gray-100 text-gray-700">
                        {course.category}
                      </span>
                    )}
                    <span
                      className="text-xs font-bold px-2 py-0.5 rounded-full"
                      style={
                        isCompleted
                          ? { background: "#DCFCE7", color: "#166534" }
                          : { background: "#DBEAFE", color: "#1D4ED8" }
                      }
                    >
                      {course.statusLabel || (isCompleted ? "Hoàn thành" : "Đang học")}
                    </span>
                  </div>

                  <h3 className="font-bold text-gray-800 text-sm md:text-base leading-snug line-clamp-1">
                    {course.title}
                  </h3>

                  {/* Tiến độ sẽ lấy từ completion bên LMS ở bước sau. Chưa có thì
                      ẩn hẳn thanh tiến độ, hiện 0% cho mọi khóa là sai sự thật. */}
                  <div className="mt-1" hidden={course.progressPercent == null}>
                    <div className="flex justify-between items-center mb-1">
                      <span className="text-xs text-gray-500">Tiến độ</span>
                      <span className="text-xs font-bold text-red-600">{tienDo}%</span>
                    </div>
                    <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                      <motion.div
                        className="h-full rounded-full"
                        style={{ background: isCompleted ? "#22C55E" : "#DC2626" }}
                        initial={{ width: 0 }}
                        animate={{ width: `${tienDo}%` }}
                        transition={{ duration: 0.9, ease: "easeOut", delay: 0.2 + index * 0.1 }}
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-between mt-1">
                    {/* Ngày ghi danh do LMS quản lý và chưa trả về trong danh sách
                        khóa — không có thì bỏ trống hẳn, đừng hiện nhãn cụt. */}
                    <span className="text-xs text-gray-400">
                      {course.enrolledAt ? `Ghi danh ngày ${doiNgay(course.enrolledAt)}` : ""}
                    </span>
                    {course.coNoiDung ? (
                      <Link
                        to={`/hoc/${course.courseId}`}
                        className="text-xs font-bold px-3 py-1.5 rounded-lg transition-all duration-200"
                        style={
                          isCompleted
                            ? { background: "#F0FDF4", color: "#166534", border: "1px solid #BBF7D0" }
                            : { background: "#DC2626", color: "#fff" }
                        }
                      >
                        {isCompleted ? "Xem lại" : "Vào học →"}
                      </Link>
                    ) : (
                      <span className="text-xs font-bold px-3 py-1.5 rounded-lg bg-gray-100 text-gray-400">
                        Sắp khai giảng
                      </span>
                    )}
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
    </motion.div>
  );
}
