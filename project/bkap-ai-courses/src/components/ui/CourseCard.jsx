import React from "react";
import { Link } from "react-router-dom";
import { Clock, PlayCircle, Users, BarChart2, Star } from "lucide-react";
import {
  formatPrice,
  levelLabel,
  getCategoryColor,
  getLevelColor,
  getInitials,
} from "../../utils/courseHelpers";

// ── helper: render sao ──────────────────────────────────────────────────────
function StarRating({ rating, count }) {
  if (!rating || rating === 0) {
    return (
      <span className="flex items-center gap-1 text-xs text-gray-400">
        {[1, 2, 3, 4, 5].map((s) => (
          <Star key={s} className="w-3.5 h-3.5" strokeWidth={1.5} />
        ))}
        <span className="ml-1">Chưa có đánh giá</span>
      </span>
    );
  }
  const full = Math.floor(rating);
  const half = rating - full >= 0.5;
  return (
    <span className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((s) => (
        <Star
          key={s}
          className="w-3.5 h-3.5"
          fill={
            s <= full
              ? "#f59e0b"
              : s === full + 1 && half
                ? "url(#half)"
                : "none"
          }
          stroke={s <= full || (s === full + 1 && half) ? "#f59e0b" : "#d1d5db"}
          strokeWidth={1.5}
        />
      ))}
      <span className="ml-1 text-xs font-semibold text-amber-500">
        {rating}
      </span>
      <span className="text-xs text-gray-400 ml-0.5">({count})</span>
    </span>
  );
}

// ── Thumbnail ──────────────────────────────────────────────────────────────
function CourseThumbnail({ thumbnailUrl, isPro, title }) {
  return (
    <div className="relative w-full aspect-video rounded-xl overflow-hidden flex-shrink-0 bg-gray-100">
      <img
        src={thumbnailUrl || "https://placehold.co/400x225/1e293b/white?text=BKAP+AI"}
        alt={title}
        className="w-full h-full object-cover"
        onError={(e) => {
          e.target.src = "https://placehold.co/400x225/1e293b/white?text=BKAP+AI";
        }}
      />
      {/* PRO badge */}
      {isPro === 1 && (
        <div className="absolute top-3 left-3 flex items-center gap-1 bg-amber-400 text-amber-900 text-[10px] font-bold px-2 py-0.5 rounded-full shadow">
          <span>👑</span>
          <span>PRO</span>
        </div>
      )}
    </div>
  );
}

// ── Price block ──────────────────────────────────────────────────────────────
function PriceBlock({ price, originalPrice }) {
  if (!price || price === 0) {
    return <p className="text-primary font-bold text-sm">Miễn phí</p>;
  }
  if (!originalPrice || originalPrice <= price) {
    return <p className="text-primary font-bold text-sm">{formatPrice(price)}</p>;
  }
  return (
    <div className="flex items-center gap-2">
      <span className="text-gray-400 text-xs line-through">
        {formatPrice(originalPrice)}
      </span>
      <span className="text-primary font-bold text-sm">
        {formatPrice(price)}
      </span>
    </div>
  );
}

// ── Main CourseCard ───────────────────────────────────────────────────────────
export default function CourseCard({ course }) {
  return (
    <Link
      to={`/courses/${course.id}`}
      className="group block bg-white rounded-2xl overflow-hidden transition-all duration-300 hover:-translate-y-1.5 hover:shadow-2xl"
      style={{
        boxShadow: "0 2px 12px rgba(0,0,0,0.08)",
        border: `1.5px solid transparent`,
        backgroundClip: "padding-box",
      }}
      onMouseEnter={(e) => {
        // e.currentTarget.style.border = `1.5px solid #E5E7EB`;
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.border = "1.5px solid transparent";
      }}
    >
      {/* Thumbnail */}
      <CourseThumbnail thumbnailUrl={course.thumbnailUrl} isPro={course.isPro} title={course.title} />

      {/* Content */}
      <div className="p-4 flex flex-col gap-2">
        <div className="flex justify-between items-start gap-2">
           <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${getCategoryColor(course.category?.slug)}`}>
            {course.category?.name || "Khóa học"}
           </span>
           <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${getLevelColor(course.level)}`}>
            {levelLabel[course.level] || course.level || "Tất cả"}
           </span>
        </div>

        {/* Title */}
        <h3 className="font-heading font-bold text-gray-900 text-base leading-snug group-hover:text-primary transition-colors duration-200 line-clamp-2 mt-1">
          {course.title}
        </h3>

        {/* Price */}
        <PriceBlock price={course.price} originalPrice={course.originalPrice} />

        {/* Rating */}
        <StarRating rating={course.rating} count={course.ratingCount} />

        {/* Divider */}
        <div className="border-t border-gray-100 mt-1 pt-2">
          {/* Instructor */}
          <div className="flex items-center gap-2 text-gray-500 text-xs">
            <div className="w-6 h-6 rounded-full flex-shrink-0 bg-primary flex items-center justify-center text-white font-bold text-[10px]">
              {getInitials(course.instructor?.fullname)}
            </div>
            <span className="font-medium text-gray-600 truncate flex-1">
              {course.instructor?.fullname || "Giảng viên BKAP"}
            </span>
          </div>

          <div className="flex items-center gap-3 mt-2 text-gray-400 text-xs">
            {/* Duration */}
            <span className="flex items-center gap-1">
              <Clock className="w-3.5 h-3.5" strokeWidth={1.5} />
              {course.durationText || "—"}
            </span>
            {/* Students */}
            <span className="flex items-center gap-1 ml-auto">
              <Users className="w-3.5 h-3.5" strokeWidth={1.5} />
              {(course.studentCount || 0).toLocaleString("vi-VN")}
            </span>
          </div>
        </div>
      </div>
    </Link>
  );
}
