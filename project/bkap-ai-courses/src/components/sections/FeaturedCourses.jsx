import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import CourseCard from "../ui/CourseCard";
import SkeletonCard from "../SkeletonCard";
import { getAllCoursesApi } from "../../api/CourseApi";

const staggerContainer = {
  hidden: {},
  show: {
    transition: { staggerChildren: 0.1 },
  },
};

const cardVariant = {
  hidden: { opacity: 0, y: 28 },
  show: { opacity: 1, y: 0, transition: { duration: 0.45, ease: "easeOut" } },
};

export default function FeaturedCourses() {
  const [basicCourses, setBasicCourses] = useState([]);
  const [proCourses, setProCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    getAllCoursesApi()
      .then((res) => {
        const data = Array.isArray(res.data) ? res.data : [];
        const basic = data.filter((c) => c.isPro === 0 || !c.isPro);
        const pro = data.filter((c) => c.isPro === 1);
        setBasicCourses(basic.slice(0, 4)); // Lấy 4 khóa cơ bản
        setProCourses(pro.slice(0, 4));     // Lấy 4 khóa pro
      })
      .catch((err) => {
        console.error(err);
        setError(true);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <section className="py-16 md:py-20 bg-gray-50" aria-label="Khóa học nổi bật">
      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">

        {/* ─── PHẦN 1: KHÓA HỌC DÀNH CHO BẠN (CƠ BẢN) ─── */}
        <div className="mb-16">
          <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-8">
            <div>
              <p className="text-xs font-semibold text-primary uppercase tracking-widest mb-1">
                Lộ trình bắt đầu
              </p>
              <h2 className="font-heading font-extrabold text-gray-900 text-2xl md:text-3xl leading-tight">
                Khóa học dành cho bạn
              </h2>
            </div>
            <Link
              to="/courses"
              className="hidden md:flex items-center gap-1.5 text-sm font-semibold text-primary hover:gap-3 transition-all duration-300"
            >
              Xem tất cả
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {[...Array(4)].map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          ) : error ? (
            <div className="text-center py-10 text-gray-400">
              <p className="text-lg font-medium text-red-500">
                Không thể tải khóa học. Vui lòng thử lại sau.
              </p>
            </div>
          ) : basicCourses.length === 0 ? (
            <div className="text-center py-10 text-gray-400">
              <p className="text-lg font-medium">Chưa có khóa học cơ bản nào.</p>
            </div>
          ) : (
            <motion.div
              variants={staggerContainer}
              initial="hidden"
              whileInView="show"
              viewport={{ once: true, amount: 0.1 }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5"
            >
              {basicCourses.map((course) => (
                <motion.div key={course.id} variants={cardVariant}>
                  <CourseCard course={course} />
                </motion.div>
              ))}
            </motion.div>
          )}
        </div>

        {/* ─── PHẦN 2: KHÓA HỌC NÂNG CAO (PRO) ─── */}
        <div>
          <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-8">
            <div>
              <p className="text-xs font-semibold text-amber-500 uppercase tracking-widest mb-1">
                Chuyên sâu & Thực chiến
              </p>
              <h2 className="font-heading font-extrabold text-gray-900 text-2xl md:text-3xl leading-tight">
                Khóa học Nâng cao
              </h2>
            </div>
            <Link
              to="/courses"
              className="hidden md:flex items-center gap-1.5 text-sm font-semibold text-amber-600 hover:gap-3 transition-all duration-300"
            >
              Khám phá thêm
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {[...Array(4)].map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          ) : error ? (
            <div className="text-center py-10 text-gray-400">
              <p className="text-lg font-medium text-red-500">
                Không thể tải khóa học. Vui lòng thử lại sau.
              </p>
            </div>
          ) : proCourses.length === 0 ? (
            <div className="text-center py-10 text-gray-400">
              <p className="text-lg font-medium">Chưa có khóa học nâng cao nào.</p>
            </div>
          ) : (
            <motion.div
              variants={staggerContainer}
              initial="hidden"
              whileInView="show"
              viewport={{ once: true, amount: 0.1 }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5"
            >
              {proCourses.map((course) => (
                <motion.div key={course.id} variants={cardVariant}>
                  <CourseCard course={course} />
                </motion.div>
              ))}
            </motion.div>
          )}
        </div>

        {/* Mobile View All Button */}
        <div className="mt-10 flex md:hidden justify-center">
          <Link
            to="/courses"
            className="flex items-center gap-1.5 text-sm font-semibold text-primary border border-primary px-6 py-2.5 rounded-full"
          >
            Xem tất cả khóa học
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

      </div>
    </section>
  );
}
