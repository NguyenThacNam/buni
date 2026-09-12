import React from "react";
import { motion } from "framer-motion";
import { Star } from "lucide-react";

/**
 * SuggestedCourseCard: Thẻ khóa học gợi ý nhỏ
 * Hiển thị: thumbnail, tag, tên, giá, rating, số học viên
 */
export default function SuggestedCourseCard({ course, index = 0 }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.1, ease: "easeOut" }}
      className="bg-white rounded-2xl overflow-hidden shadow-sm border border-gray-100 hover:shadow-lg transition-all duration-300 hover:-translate-y-1 flex flex-col"
    >
      {/* Thumbnail */}
      <div className="relative">
        <img
          src={course.thumbnail}
          alt={course.title}
          className="w-full h-36 object-cover"
        />
        {/* Tag badge */}
        <span className="absolute top-2 left-2 text-xs font-bold px-2.5 py-1 rounded-full bg-yellow-400 text-yellow-900">
          {course.tag}
        </span>
      </div>

      {/* Content */}
      <div className="p-3 flex flex-col gap-1.5 flex-1">
        <h4 className="font-bold text-gray-800 text-sm leading-snug line-clamp-2">
          {course.title}
        </h4>

        {/* Giá */}
        <p className="text-base font-extrabold text-red-600">{course.price}</p>

        {/* Rating + Students */}
        <div className="flex items-center gap-1.5 mt-auto">
          <Star className="w-3.5 h-3.5 fill-yellow-400 text-yellow-400" />
          <span className="text-xs font-bold text-gray-700">
            {course.rating}
          </span>
          <span className="text-xs text-gray-400">
            · {course.students.toLocaleString()} học viên
          </span>
        </div>

        {/* Nút xem */}
        <button className="mt-2 w-full py-2 rounded-lg text-xs font-bold border-2 border-red-600 text-red-600 hover:bg-red-600 hover:text-white transition-all duration-200">
          Xem khóa học
        </button>
      </div>
    </motion.div>
  );
}
