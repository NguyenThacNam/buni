import React, { useState, useEffect } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  Clock,
  PlayCircle,
  Users,
  BarChart2,
  Star,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Wifi,
  Award,
  ChevronRight,
  Play,
  X,
} from "lucide-react";
import { getCourseByIdApi, getCoursesByCategoryApi } from "../api/CourseApi";
import { api } from "../api/Api";
import { CONTACT_INFO } from "../data/constants";
import {
  formatPrice,
  levelLabel,
  getYoutubeEmbedUrl,
  getInitials,
} from "../utils/courseHelpers";
import CourseCard from "../components/ui/CourseCard";
import { useAuth } from "../context/AuthContext";

/**
 * Rút một đoạn chữ thuần từ phần mô tả để hiện ở đầu trang.
 *
 * Mô tả đồng bộ từ Moodle về là HTML đầy đủ — có thẻ h2, bảng, thẻ span kèm
 * style của Word. Đổ thẳng vào đoạn tóm tắt thì người xem đọc được nguyên mấy
 * dòng thẻ; mà nhúng dạng HTML cũng không xong, vì chỗ đó chỉ dành cho một
 * đoạn ngắn, cả cái bảng nhảy vào là vỡ bố cục.
 *
 * DOMParser bóc thẻ mà không chạy script hay tải ảnh, khác với gán innerHTML.
 */
const rutGonMoTa = (html, gioiHan = 220) => {
  if (!html) return "";
  const chu = (
    new DOMParser().parseFromString(html, "text/html").body.textContent || ""
  )
    .replace(/\s+/g, " ")
    .trim();
  return chu.length > gioiHan ? chu.slice(0, gioiHan).trimEnd() + "…" : chu;
};

// ── helpers ──────────────────────────────────────────────────────────────────
function StarRow({ rating, count }) {
  const full = Math.floor(rating);
  const half = rating - full >= 0.5;
  return (
    <span className="flex items-center gap-1 flex-wrap">
      {[1, 2, 3, 4, 5].map((s) => (
        <Star
          key={s}
          className="w-4 h-4"
          fill={
            s <= full ? "#f59e0b" : s === full + 1 && half ? "#f59e0b" : "none"
          }
          stroke={s <= full || (s === full + 1 && half) ? "#f59e0b" : "#d1d5db"}
          strokeWidth={1.5}
        />
      ))}
      <span className="ml-1 font-bold text-amber-500 text-sm">{rating}</span>
      <span className="text-gray-500 text-sm">({count} đánh giá)</span>
    </span>
  );
}

// ── Accordion ─────────────────────────────────────────────────────────────────
function AccordionItem({ title, lessonsCount, index }) {
  const [open, setOpen] = useState(index === 0);

  return (
    <div className="border border-gray-200 rounded-xl overflow-hidden">
      <button
        onClick={() => setOpen((o) => !o)}
        className="w-full flex items-center justify-between px-5 py-4 bg-white hover:bg-gray-50 transition-colors text-left gap-3"
        aria-expanded={open}
      >
        <div className="flex items-center gap-3 min-w-0">
          <span
            className="flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white"
            style={{ background: "#C8102E" }}
          >
            {index + 1}
          </span>
          <span className="font-semibold text-gray-800 text-sm truncate">
            {title}
          </span>
        </div>
        <div className="flex items-center gap-3 flex-shrink-0">
          <span className="text-xs text-gray-400 hidden sm:block">
            {lessonsCount} bài học
          </span>
          {open ? (
            <ChevronUp className="w-4 h-4 text-gray-400" />
          ) : (
            <ChevronDown className="w-4 h-4 text-gray-400" />
          )}
        </div>
      </button>

      {open && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          exit={{ opacity: 0, height: 0 }}
          transition={{ duration: 0.25 }}
          className="border-t border-gray-100 divide-y divide-gray-50"
        >
          {Array.from({ length: lessonsCount }).map((_, i) => (
            <div
              key={i}
              className="flex items-center gap-3 px-5 py-3 bg-gray-50 hover:bg-gray-100 transition-colors"
            >
              <PlayCircle
                className="w-4 h-4 text-gray-300 flex-shrink-0"
                strokeWidth={1.5}
              />
              <span className="text-sm text-gray-600 flex-1">
                Bài {i + 1}: Nội dung bài học {i + 1}
              </span>
              <span className="text-xs text-gray-400 flex-shrink-0">05:00</span>
            </div>
          ))}
        </motion.div>
      )}
    </div>
  );
}

// ── Video Modal ───────────────────────────────────────────────────────────────
function VideoModal({ videoUrl, onClose }) {
  useEffect(() => {
    const h = (e) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-[999] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-4xl bg-black rounded-2xl overflow-hidden shadow-2xl flex items-center justify-center"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          aria-label="Đóng"
          className="absolute -top-10 right-0 md:top-3 md:-right-12 w-8 h-8 rounded-full bg-white/20 hover:bg-white/40 flex items-center justify-center text-white transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
        {videoUrl ? (
          <iframe
            className="w-full aspect-video"
            src={videoUrl}
            title="Video Player"
            frameBorder="0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          ></iframe>
        ) : (
          <div className="w-full aspect-video flex items-center justify-center text-white">
            Không có video giới thiệu.
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Người đang đăng nhập có phải admin không, đọc từ access token.
 *
 * Chỉ dùng để chọn nút hiển thị. Không phải chốt bảo mật: sửa được giá trị này
 * trong trình duyệt thì cũng chỉ thấy nút "Vào học", bấm vào backend vẫn kiểm
 * tra quyền thật và trả 403.
 */
function laQuanTri() {
  try {
    const token = localStorage.getItem("token");
    if (!token) return false;
    const phan = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(phan)).role === "ADMIN";
  } catch {
    return false;
  }
}

// ── Sidebar (Đã cập nhật gọi API Spring Boot chuẩn xác) ───────────────────────────
function CourseSidebar({ course }) {
  const [showVideo, setShowVideo] = useState(false);
  const { user, openLogin } = useAuth();
  const isLoggedIn = !!user;

  // Quyền học khóa này: null = đang hỏi server, true/false = đã biết.
  //
  // Nút hiện theo quyền chứ không hiện một nút cho tất cả: tài khoản do trung
  // tâm cấp và khóa nào mở cho ai cũng do trung tâm quyết, nên người chưa được
  // ghi danh phải thấy lối liên hệ, không phải nút "Vào học" bấm vào lại bị chặn.
  //
  // Đây chỉ là gợi ý giao diện — cửa thật nằm ở backend, /learn/** tự kiểm tra
  // ghi danh và trả 403 nếu chưa có.
  const [duocHoc, setDuocHoc] = useState(null);

  useEffect(() => {
    if (!isLoggedIn || !course.moodleCourseId) {
      setDuocHoc(false);
      return;
    }
    if (laQuanTri()) {
      // Admin vào được mọi khóa để kiểm tra nội dung trước khi cấp cho học viên.
      setDuocHoc(true);
      return;
    }
    let conDung = true;
    setDuocHoc(null);
    api("get", "/learn/my-courses")
      .then((res) => {
        if (conDung) {
          setDuocHoc(res.data.some((k) => k.courseId === course.id));
        }
      })
      .catch(() => conDung && setDuocHoc(false));
    return () => {
      conDung = false;
    };
  }, [isLoggedIn, course.id, course.moodleCourseId]);
  const navigate = useNavigate();

  const videoUrl = getYoutubeEmbedUrl(course.previewVideoUrl);
  const isFree = course.price === 0 || !course.price;
  const isContact = course.priceType === "CONTACT";

  // ─── NÚT "ĐĂNG KÝ ĐỂ LÀM BÀI KIỂM TRA" — ĐÃ BỎ (11/09/2026) ────────────────
  // Giờ admin ghi danh ở /admin/enrollment thì hệ thống đẩy ghi danh sang LMS
  // luôn, còn bài kiểm tra mở thẳng từ trang học (/hoc/:id). Học viên không cần
  // tự bấm ghi danh nữa.
  // /**
  //  * Nút "Đăng ký để làm bài kiểm tra": ghi danh vào khóa bên LMS rồi mở trang
  //  * khóa đó, đã đăng nhập sẵn.
  //  *
  //  * Chỉ gửi mã khóa học. Server tự biết ai đang bấm nhờ token đăng nhập, tự tạo
  //  * tài khoản LMS nếu chưa có — nên trình duyệt không phải gửi, và cũng không
  //  * phải giữ, mật khẩu của người dùng.
  //  */
  // const handleEnrollClick = async () => {
  //   if (!isLoggedIn) {
  //     if (typeof openLogin === "function") openLogin();
  //     return;
  //   }

  //   // Mở tab TRƯỚC khi gọi mạng. Gọi xong mới mở thì đã rời khỏi nhịp bấm chuột
  //   // của người dùng, trình duyệt coi là cửa sổ tự bật và chặn.
  //   const tab = window.open("", "_blank");
  //   try {
  //     const res = await api("post", "/learn/lms-url", {
  //       courseId: String(course.id),
  //     });
  //     if (tab) tab.location.href = res.data.loginUrl;
  //     else window.location.href = res.data.loginUrl;
  //   } catch (err) {
  //     if (tab) tab.close();
  //     alert(
  //       err?.response?.data?.error ||
  //         "Chưa kết nối được hệ thống LMS. Vui lòng thử lại sau.",
  //     );
  //   }
  // };

  return (
    <>
      {showVideo && (
        <VideoModal videoUrl={videoUrl} onClose={() => setShowVideo(false)} />
      )}

      {/* Card sidebar */}
      <div
        className="bg-white rounded-2xl overflow-hidden"
        style={{ boxShadow: "0 4px 40px rgba(0,0,0,0.13)" }}
      >
        {/* Thumbnail + play button */}
        <div
          className="relative w-full aspect-video cursor-pointer group bg-gray-900"
          onClick={() => setShowVideo(true)}
          role="button"
          tabIndex={0}
          aria-label="Xem giới thiệu khóa học"
          onKeyDown={(e) => e.key === "Enter" && setShowVideo(true)}
        >
          <img
            src={
              course.thumbnailUrl ||
              "https://placehold.co/400x225/1e293b/white?text=BKAP+AI"
            }
            alt={course.title}
            className="w-full h-full object-cover opacity-80 group-hover:opacity-100 transition-opacity"
          />
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-2">
            <div className="w-14 h-14 rounded-full bg-white/90 flex items-center justify-center shadow-2xl group-hover:scale-110 transition-transform duration-300">
              <Play
                className="w-6 h-6 text-gray-900 ml-0.5"
                fill="currentColor"
              />
            </div>
            <p className="text-white font-semibold text-sm drop-shadow-md">
              Xem giới thiệu khóa học
            </p>
          </div>
        </div>

        <div className="p-5 space-y-5">
          {/* Price */}
          <div>
            <p className="text-xs text-gray-400 font-medium mb-1">
              Chi phí khóa học
            </p>
            {isFree ? (
              <p className="font-heading font-extrabold text-primary text-3xl">
                Miễn phí
              </p>
            ) : isContact ? (
              <p className="font-heading font-extrabold text-primary text-3xl">
                Liên hệ
              </p>
            ) : (
              <div className="flex items-baseline gap-2 flex-wrap">
                <p className="font-heading font-extrabold text-primary text-3xl">
                  {formatPrice(course.price)}
                </p>
                {course.originalPrice > course.price && (
                  <p className="text-gray-400 text-sm line-through">
                    {formatPrice(course.originalPrice)}
                  </p>
                )}
              </div>
            )}
          </div>

          {/* NÚT CHÍNH — hiện theo quyền học của người đang xem.
              Khóa chưa nối LMS thì chưa có bài giảng để học: "Sắp khai giảng". */}
          {!course.moodleCourseId ? (
            <div>
              <button
                type="button"
                disabled
                className="w-full py-3.5 rounded-full bg-gray-200 text-gray-500 font-heading font-bold text-sm tracking-widest uppercase cursor-not-allowed"
              >
                Sắp khai giảng
              </button>
              <p className="mt-2 text-center text-xs text-gray-400">
                Khóa học đang được chuẩn bị trên hệ thống học trực tuyến.
              </p>
            </div>
          ) : duocHoc === null ? (
            // Đang hỏi server — giữ chỗ để nút không nhảy từ "Liên hệ" sang "Vào học".
            <div className="w-full py-3.5 rounded-full bg-gray-100 animate-pulse" />
          ) : duocHoc ? (
            <Link
              to={`/hoc/${course.id}`}
              className="block w-full py-3.5 rounded-full bg-primary text-white text-center font-heading font-bold text-sm tracking-widest uppercase hover:bg-primary-dark transition-colors shadow-lg"
            >
              Vào học
            </Link>
          ) : (
            <div>
              <Link
                to="/contact"
                className="block w-full py-3.5 rounded-full bg-primary text-white text-center font-heading font-bold text-sm tracking-widest uppercase hover:bg-primary-dark transition-colors shadow-lg"
              >
                Liên hệ để đăng ký
              </Link>
              <p className="mt-2 text-center text-xs text-gray-500">
                {isLoggedIn
                  ? "Tài khoản của bạn chưa được ghi danh khóa này. "
                  : "Khóa học do trung tâm cấp quyền. "}
                Gọi{" "}
                <a
                  href={`tel:${CONTACT_INFO.phone.replace(/\./g, "")}`}
                  className="font-semibold text-primary hover:underline"
                >
                  {CONTACT_INFO.phone}
                </a>
              </p>
              {!isLoggedIn && (
                <p className="mt-1.5 text-center text-xs text-gray-500">
                  Đã là học viên?{" "}
                  <button
                    type="button"
                    onClick={openLogin}
                    className="font-semibold text-primary hover:underline"
                  >
                    Đăng nhập
                  </button>
                </p>
              )}
            </div>
          )}

          {/* Course meta */}
          <div className="space-y-2.5 pt-1 border-t border-gray-100">
            {[
              {
                icon: (
                  <BarChart2
                    className="w-4 h-4 text-primary"
                    strokeWidth={1.5}
                  />
                ),
                label: "Trình độ",
                value: levelLabel[course.level] || course.level,
              },
              {
                icon: (
                  <Clock className="w-4 h-4 text-red-400" strokeWidth={1.5} />
                ),
                label: "Thời lượng",
                value: course.durationText || "—",
              },
              {
                icon: (
                  <Award className="w-4 h-4 text-amber-400" strokeWidth={1.5} />
                ),
                label: "Chứng chỉ hoàn thành",
                value: null,
              },
              {
                icon: (
                  <Wifi className="w-4 h-4 text-blue-400" strokeWidth={1.5} />
                ),
                label: "Học mọi lúc, mọi nơi",
                value: null,
              },
            ].map((row, i) => (
              <div
                key={i}
                className="flex items-center gap-2.5 text-sm text-gray-600"
              >
                {row.icon}
                <span>
                  {row.value ? (
                    <>
                      {row.label}{" "}
                      <strong className="text-gray-800">{row.value}</strong>
                    </>
                  ) : (
                    row.label
                  )}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
}

// ── Related Courses ───────────────────────────────────────────────────────────
function RelatedCourses({ currentId, categorySlug }) {
  const [related, setRelated] = useState([]);

  useEffect(() => {
    if (!categorySlug) return;
    getCoursesByCategoryApi(categorySlug).then((res) => {
      const data = Array.isArray(res.data) ? res.data : [];
      setRelated(
        data.filter((c) => String(c.id) !== String(currentId)).slice(0, 3),
      );
    });
  }, [categorySlug, currentId]);

  if (related.length === 0) return null;

  return (
    <section className="mt-14" aria-label="Khóa học liên quan">
      <div className="flex items-center justify-between mb-6">
        <h2 className="font-heading font-bold text-gray-900 text-xl md:text-2xl">
          Khóa học liên quan
        </h2>
        <Link
          to="/courses"
          className="flex items-center gap-1 text-sm font-semibold text-primary hover:gap-2 transition-all duration-200"
        >
          Xem tất cả <ChevronRight className="w-4 h-4" />
        </Link>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {related.map((c) => (
          <CourseCard key={c.id} course={c} />
        ))}
      </div>
    </section>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────
export default function CourseDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [course, setCourse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    window.scrollTo(0, 0);
    if (!id) return;
    setLoading(true);
    getCourseByIdApi(id)
      .then((res) => {
        setCourse(res.data);
        document.title = `${res.data.title} | BKAP AI`;
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-4">
        <p className="text-xl font-bold text-gray-800 mb-4">
          Không tìm thấy khóa học
        </p>
        <button
          onClick={() => navigate("/courses")}
          className="text-primary hover:underline font-semibold"
        >
          Quay lại danh sách
        </button>
      </div>
    );
  }

  // Fallback modules
  const modulesFallback = [
    { title: "Giới thiệu khóa học", lessons: 2 },
    { title: "Kiến thức cơ bản", lessons: 5 },
    { title: "Thực hành", lessons: 8 },
  ];

  return (
    <main className="bg-gray-50 min-h-screen">
      {/* ── Top header bar ── */}
      <div
        style={{
          background: "linear-gradient(135deg, #1A1A2E 0%, #0f2850 100%)",
        }}
      >
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 py-10 md:py-14">
          {/* Breadcrumb */}
          <nav
            className="flex items-center gap-1.5 text-xs text-white/40 mb-5 flex-wrap"
            aria-label="Điều hướng"
          >
            <Link to="/" className="hover:text-white/70 transition-colors">
              Trang chủ
            </Link>
            <ChevronRight className="w-3 h-3" />
            <Link
              to="/courses"
              className="hover:text-white/70 transition-colors"
            >
              Khóa học
            </Link>
            <ChevronRight className="w-3 h-3" />
            <span className="text-white/70">{course.title}</span>
          </nav>

          {/* Info left */}
          <div className="max-w-2xl">
            <span className="inline-block px-3 py-1 rounded-full text-xs font-bold text-white bg-white/15 border border-white/20 mb-4">
              {course.category?.name || "Khóa học"}
            </span>

            <h1 className="font-heading font-extrabold text-white text-2xl md:text-3xl xl:text-4xl leading-tight mb-3">
              {course.title}
            </h1>

            <p className="text-white/65 text-sm md:text-base leading-relaxed mb-5">
              {course.subtitle || rutGonMoTa(course.description)}
            </p>

            {/* Rating + stats */}
            <div className="flex flex-wrap items-center gap-x-5 gap-y-2 mb-5">
              {course.rating > 0 && (
                <StarRow rating={course.rating} count={course.ratingCount} />
              )}
              <span className="flex items-center gap-1.5 text-white/55 text-sm">
                <Users className="w-4 h-4" strokeWidth={1.5} />
                {(course.studentCount || 0).toLocaleString("vi-VN")} học viên
              </span>
              <span className="flex items-center gap-1.5 text-white/55 text-sm">
                <Clock className="w-4 h-4" strokeWidth={1.5} />
                {course.durationText || "—"}
              </span>
              <span className="flex items-center gap-1.5 text-white/55 text-sm">
                <BarChart2 className="w-4 h-4" strokeWidth={1.5} />
                {levelLabel[course.level] || course.level || "Tất cả trình độ"}
              </span>
              {course.isPro === 1 && (
                <span className="bg-amber-400 text-amber-900 text-xs font-bold px-2 py-0.5 rounded-sm">
                  PRO
                </span>
              )}
            </div>

            {/* Instructor */}
            <div className="flex items-center gap-2 mt-4">
              <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-white font-bold text-xs border-2 border-white/25">
                {getInitials(course.instructor?.fullname)}
              </div>
              <span className="text-white/55 text-sm">
                Giảng viên:{" "}
                <span className="text-white font-semibold">
                  {course.instructor?.fullname || "Giảng viên BKAP"}
                </span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* ── Body content ── */}
      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
        <div className="grid lg:grid-cols-3 gap-8 items-start overflow-visible">
          {/* LEFT CONTENT */}
          <div className="lg:col-span-2 py-10 space-y-6">
            {/* Bạn sẽ học được gì */}
            <div className="bg-white rounded-2xl p-6 border border-gray-100">
              <h2 className="font-heading font-bold text-gray-900 text-lg mb-4">
                Bạn sẽ học được gì?
              </h2>
              <ul className="grid sm:grid-cols-2 gap-3">
                {[
                  "Hiểu sâu về công nghệ",
                  "Làm dự án thực tế",
                  "Tư duy giải quyết vấn đề",
                  "Chứng chỉ hoàn thành",
                  "Hỗ trợ việc làm sau tốt nghiệp",
                ].map((h, i) => (
                  <li
                    key={i}
                    className="flex items-start gap-2 text-sm text-gray-700"
                  >
                    <CheckCircle2
                      className="w-4 h-4 text-primary flex-shrink-0 mt-0.5"
                      strokeWidth={2}
                    />
                    {h}
                  </li>
                ))}
              </ul>
            </div>

            {/* Nội dung khóa học */}
            <div className="bg-white rounded-2xl p-6 border border-gray-100">
              <h2 className="font-heading font-bold text-gray-900 text-lg mb-1">
                Nội dung khóa học
              </h2>
              <p className="text-sm text-gray-400 mb-4">
                {course.durationText || "—"}
              </p>
              <div className="space-y-3">
                {modulesFallback.map((mod, i) => (
                  <AccordionItem
                    key={i}
                    title={mod.title}
                    lessonsCount={mod.lessons}
                    index={i}
                  />
                ))}
              </div>
            </div>

            {/* Mô tả */}
            <div className="bg-white rounded-2xl p-6 border border-gray-100">
              <h2 className="font-heading font-bold text-gray-900 text-lg mb-3">
                Mô tả khóa học
              </h2>
              <div className="text-sm text-gray-600 leading-relaxed space-y-3">
                <div
                  dangerouslySetInnerHTML={{
                    __html: course.description || "Nội dung đang cập nhật...",
                  }}
                />
                <p>
                  With tỷ lệ <strong>75% thực hành</strong>, mỗi module đều có
                  bài tập lab chi tiết và đồ án thực tế.
                </p>
                <ul className="list-none space-y-1.5">
                  {[
                    "Thực hành lab-guide chi tiết từng bước.",
                    "Đồ án thực tế (e-Project) đánh giá năng lực cuối môn.",
                    "Giảng viên là chuyên gia đang làm tại doanh nghiệp.",
                    "Hỗ trợ giới thiệu việc làm sau khi tốt nghiệp.",
                  ].map((t, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <span className="text-primary mt-1">•</span>
                      {t}
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Khóa học liên quan */}
            <RelatedCourses
              currentId={course.id}
              categorySlug={course.category?.slug}
            />
          </div>

          {/* RIGHT SIDEBAR (STICKY ON DESKTOP) */}
          <div className="hidden lg:block self-start sticky top-20 z-20 -mt-[310px] pb-10">
            <CourseSidebar course={course} />
          </div>
        </div>

        {/* MOBILE SIDEBAR */}
        <div className="lg:hidden py-6">
          <CourseSidebar course={course} />
        </div>
      </div>
    </main>
  );
}
