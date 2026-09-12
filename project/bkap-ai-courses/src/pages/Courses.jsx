import React, { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { motion } from "framer-motion";
import { Search, SearchX, RefreshCw } from "lucide-react";
import CourseCard from "../components/ui/CourseCard";
import SkeletonCard from "../components/SkeletonCard";
import CategoryTabs from "../components/ui/CategoryTabs";
import { getAllCoursesApi, getCoursesByCategoryApi } from "../api/CourseApi";
import { useAuth } from "../context/AuthContext";

const stagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08 } },
};
const item = {
  hidden: { opacity: 0, y: 24 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: "easeOut" } },
};

export default function Courses() {
  const [searchParams, setSearchParams] = useSearchParams();
  const defaultCategory = searchParams.get("category") || "all";

  const [courses, setCourses] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [activeCategory, setActiveCategory] = useState(defaultCategory);
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  // Sync activeCategory from searchParams when URL changes (e.g. clicking Navbar dropdown)
  useEffect(() => {
    const cat = searchParams.get("category") || "all";
    setActiveCategory(cat);
  }, [searchParams]);

  // Update page title
  useEffect(() => {
    document.title = "Các khóa học CNTT & AI | BKAP AI";
  }, []);

  // Fetch courses when activeCategory changes
  useEffect(() => {
    setLoading(true);
    setError(false);

    const fetchPromise =
      activeCategory === "all"
        ? getAllCoursesApi()
        : getCoursesByCategoryApi(activeCategory);

    fetchPromise
      .then((res) => {
        const data = Array.isArray(res.data) ? res.data : [];
        setCourses(data);
        setFiltered(data);
      })
      .catch((err) => {
        console.error(err);
        setError(true);
      })
      .finally(() => setLoading(false));
  }, [activeCategory]);

  // Filter theo category
  const handleCategoryFilter = (slug) => {
    setActiveCategory(slug);
    setSearchQuery(""); // reset search
    setSearchParams(slug === "all" ? {} : { category: slug }, {
      replace: true,
    });
  };

  // Search local — tìm trong data đã load.
  //
  // `courses` đã là đúng danh sách của danh mục đang chọn (server lọc sẵn, gồm
  // cả khóa của danh mục con), nên luôn tìm trên nó.
  //
  // Bản trước có hai lỗi: đang ở một danh mục mà gõ tìm rồi xóa hết chữ thì danh
  // sách không hiện lại (chỉ khôi phục khi đang ở "Tất cả"); và lọc thêm lần nữa
  // theo đúng slug đang chọn, làm rơi mất khóa của danh mục con.
  useEffect(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) {
      setFiltered(courses);
      return;
    }
    setFiltered(
      courses.filter(
        (c) =>
          c.title?.toLowerCase().includes(q) ||
          c.subtitle?.toLowerCase().includes(q) ||
          c.instructor?.fullname?.toLowerCase().includes(q) ||
          c.category?.name?.toLowerCase().includes(q),
      ),
    );
  }, [searchQuery, courses]);

  const basicCourses = filtered.filter((c) => c.isPro === 0 || !c.isPro);
  const proCourses = filtered.filter((c) => c.isPro === 1);

  return (
    <main>
      {/* Hero nhỏ */}
      <section
        className="py-14 md:py-20"
        style={{
          background: "linear-gradient(135deg, #1A1A2E 0%, #C8102E 100%)",
        }}
        aria-label="Trang khóa học"
      >
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 text-center">
          <p className="text-white/60 text-xs font-semibold uppercase tracking-widest mb-2">
            Bachkhoa-Aptech
          </p>
          <h1 className="font-heading font-extrabold text-white text-3xl md:text-4xl mb-4">
            Tất cả khóa học
          </h1>
          <p className="text-white/70 text-sm md:text-base max-w-xl mx-auto mb-6">
            Chọn lộ trình phù hợp với mục tiêu của bạn — từ cơ bản đến chuyên
            sâu.
          </p>
          {/* Search bar */}
          <div className="relative max-w-md mx-auto">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Tìm khóa học..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Tìm kiếm khóa học"
              className="w-full pl-10 pr-4 py-3 rounded-full bg-white text-gray-800 text-sm font-medium shadow-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
        </div>
      </section>

      {/* Filter + Grid */}
      <section
        className="py-12 bg-bg min-h-screen"
        aria-label="Danh sách khóa học"
      >
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
          {/* Tabs Category Component */}
          <div className="mb-8">
            <CategoryTabs
              activeCategory={activeCategory}
              onSelectCategory={handleCategoryFilter}
            />
          </div>

          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-gray-800">
              {searchQuery ? "Kết quả tìm kiếm" : "Danh sách khóa học"}
            </h2>
            {!loading && !error && (
              <span className="text-sm font-medium text-gray-500 bg-gray-100 px-3 py-1 rounded-full">
                {filtered.length} khóa học
              </span>
            )}
          </div>

          {/* Grid */}
          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
              {[...Array(8)].map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center py-24 text-center gap-4">
              <p className="text-red-500 font-bold text-lg">
                Không thể tải danh sách khóa học.
              </p>
              <button
                onClick={() => window.location.reload()}
                className="mt-4 px-6 py-2.5 rounded-full bg-primary text-white text-sm font-semibold hover:bg-primary-dark transition-colors"
              >
                Thử lại
              </button>
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-24 text-center gap-4">
              <SearchX className="w-14 h-14 text-gray-300" strokeWidth={1} />
              <p className="font-heading font-bold text-gray-500 text-xl">
                Không tìm thấy khóa học
              </p>
              <p className="text-gray-400 text-sm">
                Thử chọn danh mục khác hoặc xóa từ khóa tìm kiếm
              </p>
              <button
                onClick={() => {
                  handleCategoryFilter("all");
                }}
                className="mt-2 px-6 py-2.5 rounded-full bg-primary text-white text-sm font-semibold hover:bg-primary-dark transition-colors"
              >
                Xem tất cả
              </button>
            </div>
          ) : (
            <div className="space-y-16">
              {/* Lộ trình Cơ bản */}
              {basicCourses.length > 0 && (
                <div>
                  <h3 className="font-heading font-extrabold text-gray-900 text-2xl md:text-3xl mb-6 flex items-center gap-3">
                    <span className="w-2 h-8 bg-primary rounded-full"></span> Lộ
                    trình Cơ bản
                  </h3>
                  <motion.div
                    key={"basic-" + activeCategory + searchQuery}
                    variants={stagger}
                    initial="hidden"
                    animate="show"
                    className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5"
                  >
                    {basicCourses.map((course) => (
                      <motion.div key={course.id} variants={item}>
                        <CourseCard course={course} />
                      </motion.div>
                    ))}
                  </motion.div>
                </div>
              )}

              {/* Lộ trình Nâng cao (PRO) */}
              {proCourses.length > 0 && (
                <div>
                  <h3 className="font-heading font-extrabold text-gray-900 text-2xl md:text-3xl mb-6 flex items-center gap-3">
                    <span className="w-2 h-8 bg-amber-500 rounded-full"></span>{" "}
                    Lộ trình Nâng cao (PRO)
                  </h3>
                  <motion.div
                    key={"pro-" + activeCategory + searchQuery}
                    variants={stagger}
                    initial="hidden"
                    animate="show"
                    className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5"
                  >
                    {proCourses.map((course) => (
                      <motion.div key={course.id} variants={item}>
                        <CourseCard course={course} />
                      </motion.div>
                    ))}
                  </motion.div>
                </div>
              )}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
