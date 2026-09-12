import React from "react";
import { motion } from "framer-motion";

/**
 * CourseProgressCard: Thẻ khóa học đã đăng ký trong Dashboard Home
 * Hiển thị: thumbnail, category badge, tên, giảng viên, progress bar, trạng thái
 */

// Màu badge theo category
const CATEGORY_COLORS = {
  Frontend: { bg: "#EFF6FF", text: "#1D4ED8" },
  Backend: { bg: "#F0FDF4", text: "#166534" },
  Design: { bg: "#FAF5FF", text: "#7C3AED" },
  DevOps: { bg: "#FFF7ED", text: "#C2410C" },
  Mobile: { bg: "#F0F9FF", text: "#0369A1" },
};

export default function CourseProgressCard({ course }) {
  const catColor = CATEGORY_COLORS[course.category] || {
    bg: "#F3F4F6",
    text: "#374151",
  };
  const isCompleted = course.status === "Hoàn thành";

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
      className="bg-white rounded-2xl overflow-hidden shadow-sm border border-gray-100 hover:shadow-lg transition-shadow duration-300 flex flex-col"
    >
      {/* Thumbnail */}
      <div className="relative overflow-hidden">
        <img
          src={course.thumbnail}
          alt={course.title}
          className="w-full h-44 object-cover"
        />
        {/* Badge trạng thái */}
        <span
          className="absolute top-3 right-3 text-xs font-bold px-2.5 py-1 rounded-full"
          style={
            isCompleted
              ? { background: "#DCFCE7", color: "#166534" }
              : { background: "#DBEAFE", color: "#1D4ED8" }
          }
        >
          {course.status}
        </span>
      </div>

      {/* Content */}
      <div className="p-4 flex flex-col gap-2 flex-1">
        {/* Category badge */}
        <span
          className="text-xs font-bold px-2.5 py-1 rounded-md self-start"
          style={{ background: catColor.bg, color: catColor.text }}
        >
          {course.category}
        </span>

        {/* Tên khóa học */}
        <h3 className="font-bold text-gray-800 text-sm leading-snug line-clamp-2">
          {course.title}
        </h3>

        {/* Giảng viên */}
        <p className="text-xs text-gray-400">👨‍🏫 {course.instructor}</p>

        {/* Progress bar */}
        <div className="mt-1">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs font-semibold text-gray-600">
              {course.completedLessons}/{course.totalLessons} bài
            </span>
            <span className="text-xs font-bold text-red-600">
              {course.progress}%
            </span>
          </div>
          <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
            <motion.div
              className="h-full rounded-full"
              style={{ background: "#DC2626" }}
              initial={{ width: 0 }}
              animate={{ width: `${course.progress}%` }}
              transition={{ duration: 0.8, ease: "easeOut", delay: 0.2 }}
            />
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between mt-2 pt-2 border-t border-gray-50">
          <span className="text-xs text-gray-400">
            🕐 {course.lastAccessed}
          </span>
          <button
            className="text-xs font-bold px-3 py-1.5 rounded-lg transition-all duration-200"
            style={
              isCompleted
                ? {
                    background: "#F0FDF4",
                    color: "#166534",
                    border: "1px solid #BBF7D0",
                  }
                : {
                    background: "#DC2626",
                    color: "#fff",
                  }
            }
          >
            {isCompleted ? "Xem lại" : "Tiếp tục học"}
          </button>
        </div>
      </div>
    </motion.div>
  );
}
