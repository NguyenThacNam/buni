import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Button from "../ui/Button";

// Dữ liệu cho các slide của quy trình
const processSlides = [
  {
    type: "process",
    id: 1,
    steps: [
      {
        number: "01",
        title: "NHẬP HỌC",
        desc: "Làm thủ tục nhập học tại Bachkhoa-Aptech",
        color: "#00BCD4",
      },
      {
        number: "02",
        title: "THĂM QUAN DOANH NGHIỆP",
        desc: "Thăm quan doanh nghiệp tìm hiểu công việc thực tế",
        color: "#E91E8C",
      },
      {
        number: "03",
        title: "HỌC TẬP TẠI BACHKHOA-APTECH",
        desc: "Bắt đầu chương trình học tại Bachkhoa-Aptech",
        color: "#FF9800",
      },
      {
        number: "04",
        title: "THỰC HÀNH TẠI DOANH NGHIỆP",
        desc: "Đi thực tập tại các doanh nghiệp liên kết",
        color: "#8BC34A",
      },
      {
        number: "05",
        title: "TỐT NGHIỆP & GIỚI THIỆU VIỆC LÀM",
        desc: "Hoàn thành chương trình học. Tốt nghiệp & Được giới thiệu việc làm",
        color: "#F44336",
      },
    ],
  },
  {
    type: "process",
    id: 2,
    steps: [
      {
        number: "01",
        title: "ĐĂNG KÝ TƯ VẤN",
        desc: "Điền form đăng ký nhận tư vấn miễn phí",
        color: "#00BCD4",
      },
      {
        number: "02",
        title: "CHỌN KHÓA HỌC",
        desc: "Tư vấn viên giúp chọn lộ trình phù hợp",
        color: "#E91E8C",
      },
      {
        number: "03",
        title: "BẮT ĐẦU HỌC",
        desc: "Tham gia lớp học với giảng viên doanh nghiệp",
        color: "#FF9800",
      },
      {
        number: "04",
        title: "LÀM DỰ ÁN THỰC TẾ",
        desc: "Thực hành với dự án thật ngay trong quá trình học",
        color: "#8BC34A",
      },
      {
        number: "05",
        title: "NHẬN CHỨNG CHỈ & VIỆC LÀM",
        desc: "Tốt nghiệp với chứng chỉ quốc tế và cơ hội việc làm",
        color: "#F44336",
      },
    ],
  },
];

const slidesData = [
  { type: "hero", id: 0 },
  ...processSlides
];

function StepCircle({ number, color, index, total }) {
  const isFirst = index === 0;
  const isLast = index === total - 1;

  return (
    <div className="flex flex-col items-center relative">
      {/* Connector line left */}
      {!isFirst && (
        <div
          className="absolute top-[52px] right-1/2 w-full h-[3px] z-0"
          style={{
            background: `linear-gradient(to right, transparent, ${color}55)`,
          }}
        />
      )}
      {/* Connector line right */}
      {!isLast && (
        <div
          className="absolute top-[52px] left-1/2 w-full h-[3px] z-0"
          style={{
            background: `linear-gradient(to right, ${color}55, transparent)`,
          }}
        />
      )}

      {/* Outer ring */}
      <div
        className="relative z-10 w-28 h-28 rounded-full flex items-center justify-center"
        style={{
          background: `radial-gradient(circle at 35% 35%, #ffffff55, transparent)`,
          boxShadow: `0 8px 32px ${color}66, 0 2px 8px #0006`,
          border: `3px solid ${color}88`,
        }}
      >
        {/* Inner circle */}
        <div
          className="w-20 h-20 rounded-full flex items-center justify-center"
          style={{
            background: `radial-gradient(circle at 35% 35%, ${color}ff, ${color}aa)`,
            boxShadow: `inset 0 -4px 12px #0003, inset 0 4px 8px #ffffff44`,
          }}
        >
          <span
            className="font-heading font-extrabold text-white"
            style={{ fontSize: "2rem", textShadow: "0 2px 6px #0005" }}
          >
            {number}
          </span>
        </div>
      </div>
    </div>
  );
}

export default function HeroSlider() {
  const [current, setCurrent] = useState(0);
  const [direction, setDirection] = useState(1);

  const total = slidesData.length;

  const goTo = (idx) => {
    setDirection(idx > current ? 1 : -1);
    setCurrent(idx);
  };

  const prev = () => goTo((current - 1 + total) % total);
  const next = () => goTo((current + 1) % total);

  // Auto-play (7s)
  useEffect(() => {
    const timer = setInterval(next, 7000);
    return () => clearInterval(timer);
  }, [current]);

  const slide = slidesData[current];

  const variants = {
    enter: (dir) => ({ opacity: 0, x: dir > 0 ? 60 : -60 }),
    center: { opacity: 1, x: 0 },
    exit: (dir) => ({ opacity: 0, x: dir > 0 ? -60 : 60 }),
  };

  return (
    <section
      className="relative w-full overflow-hidden transition-colors duration-700 ease-in-out min-h-[80vh] flex items-center"
      style={{
        background: slide.type === "hero" 
          ? "linear-gradient(to bottom right, #111827, #7f1d1d)" // bg-gradient-to-br from-navy to-primary-dark
          : "linear-gradient(135deg, #2d0a6e 0%, #6b21a8 50%, #9333ea 100%)",
      }}
      aria-label="Khám phá Bachkhoa-Aptech"
    >
      {/* Background Dot Grid SVG for Hero */}
      {slide.type === "hero" && (
        <>
          <div className="absolute inset-0 bg-dot-grid opacity-20 pointer-events-none" />
          <div className="absolute -right-20 -bottom-20 w-80 h-80 rounded-full bg-accent opacity-10 blur-3xl pointer-events-none" />
        </>
      )}

      {/* City silhouette overlay for Process */}
      {slide.type === "process" && (
        <div
          className="absolute inset-0 opacity-10 pointer-events-none"
          style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 320'%3E%3Cpath fill='%23ffffff' d='M0,160L48,149.3C96,139,192,117,288,128C384,139,480,181,576,181.3C672,181,768,139,864,122.7C960,107,1056,117,1152,138.7C1248,160,1344,192,1392,208L1440,224L1440,320L0,320Z'/%3E%3C/svg%3E")`,
            backgroundSize: "cover",
            backgroundPosition: "bottom",
          }}
        />
      )}

      <div className="relative z-10 w-full max-w-7xl mx-auto px-4 md:px-6 lg:px-8 py-20">
        <AnimatePresence custom={direction} mode="wait">
          <motion.div
            key={current}
            custom={direction}
            variants={variants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.45, ease: "easeInOut" }}
            className="w-full"
          >
            {slide.type === "hero" ? (
              /* --- Hero Slide Content --- */
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center w-full">
                <div className="text-center lg:text-left text-white">
                  <h1 className="font-heading text-4xl md:text-5xl lg:text-6xl font-extrabold leading-tight tracking-tight mb-6 bg-clip-text bg-gradient-to-r from-white via-gray-100 to-red-200">
                    Hệ thống Đào tạo <br />
                    <span className="text-accent">CNTT & AI Quốc tế</span>
                  </h1>
                  <p className="font-sans text-lg text-gray-300 mb-8 max-w-xl mx-auto lg:mx-0 leading-relaxed">
                    Hơn 23 năm kiến tạo và dẫn đầu chất lượng đào tạo công nghệ thông tin. Cam kết việc làm
                    bằng văn bản, sẵn sàng làm chủ làn sóng AI toàn cầu.
                  </p>
                  <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4">
                    <Link to="/courses" className="w-full sm:w-auto">
                      <Button variant="accent" className="w-full sm:w-auto">
                        Xem khóa học
                      </Button>
                    </Link>
                    <Link to="/contact" className="w-full sm:w-auto">
                      <Button variant="secondary" className="w-full sm:w-auto">
                        Đăng ký tư vấn
                      </Button>
                    </Link>
                  </div>
                </div>

                <div className="hidden lg:flex justify-center" aria-hidden="true">
                  <svg
                    className="w-full max-w-lg h-auto text-white/10"
                    viewBox="0 0 500 500"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    {/* Outer Glow Ring */}
                    <circle cx="250" cy="250" r="180" stroke="currentColor" strokeWidth="2" strokeDasharray="8 8" className="animate-spin" style={{ animationDuration: "60s" }} />
                    <circle cx="250" cy="250" r="140" stroke="#C8102E" strokeWidth="3" opacity="0.3" />
                    <circle cx="250" cy="250" r="100" stroke="#F59E0B" strokeWidth="2" strokeDasharray="4 4" />
                    <circle cx="250" cy="250" r="40" fill="#C8102E" />
                    <circle cx="250" cy="250" r="20" fill="#F59E0B" />
                    <circle cx="150" cy="150" r="12" fill="#FFFFFF" className="animate-pulse" />
                    <line x1="250" y1="250" x2="150" y2="150" stroke="currentColor" strokeWidth="2" opacity="0.5" />
                    <circle cx="370" cy="200" r="8" fill="#F59E0B" />
                    <line x1="250" y1="250" x2="370" y2="200" stroke="currentColor" strokeWidth="2" opacity="0.5" />
                    <circle cx="200" cy="360" r="14" fill="#C8102E" />
                    <line x1="250" y1="250" x2="200" y2="360" stroke="currentColor" strokeWidth="2" opacity="0.5" />
                    <circle cx="340" cy="320" r="10" fill="#FFFFFF" />
                    <line x1="250" y1="250" x2="340" y2="320" stroke="currentColor" strokeWidth="2" opacity="0.5" />
                  </svg>
                </div>
              </div>
            ) : (
              /* --- Process Slide Content --- */
              <div className="w-full flex flex-col items-center">
                <div className="relative z-10 text-center mb-16 px-4">
                  <p className="text-white text-sm font-semibold tracking-[0.3em] uppercase mb-2 opacity-90">
                    MÔ HÌNH ĐỘC QUYỀN
                  </p>
                  <h2
                    className="font-heading font-extrabold uppercase mb-3"
                    style={{
                      fontSize: "clamp(2.2rem, 6vw, 4rem)",
                      background: "linear-gradient(90deg, #facc15, #fb923c)",
                      WebkitBackgroundClip: "text",
                      WebkitTextFillColor: "transparent",
                      textShadow: "none",
                      filter: "drop-shadow(0 2px 8px #0008)",
                    }}
                  >
                    LÀM TRƯỚC HỌC SAU
                  </h2>
                  <p className="text-white font-heading font-bold tracking-widest uppercase text-sm md:text-base opacity-90">
                    ĐÀO TẠO CNTT "NGƯỢC" ĐẦU TIÊN TẠI VIỆT NAM
                  </p>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6 w-full">
                  {slide.steps.map((step, i) => (
                    <div key={step.number} className="flex flex-col items-center gap-4">
                      <StepCircle
                        number={step.number}
                        color={step.color}
                        index={i}
                        total={slide.steps.length}
                      />
                      <div className="text-center px-1">
                        <p
                          className="font-heading font-extrabold text-white text-center uppercase leading-tight mb-2"
                          style={{ fontSize: "clamp(0.7rem, 1.2vw, 0.85rem)" }}
                        >
                          {step.title}
                        </p>
                        <p className="text-white/70 text-center text-xs leading-relaxed">
                          {step.desc}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Prev / Next buttons */}
      <button
        onClick={prev}
        aria-label="Slide trước"
        className="absolute left-4 top-1/2 -translate-y-1/2 z-20 w-12 h-12 rounded-full bg-white/10 hover:bg-white/30 backdrop-blur-sm flex items-center justify-center text-white transition-all duration-300"
      >
        <ChevronLeft className="w-6 h-6" />
      </button>
      <button
        onClick={next}
        aria-label="Slide tiếp theo"
        className="absolute right-4 top-1/2 -translate-y-1/2 z-20 w-12 h-12 rounded-full bg-white/10 hover:bg-white/30 backdrop-blur-sm flex items-center justify-center text-white transition-all duration-300"
      >
        <ChevronRight className="w-6 h-6" />
      </button>

      {/* Dots */}
      <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 flex justify-center gap-2">
        {slidesData.map((_, i) => (
          <button
            key={i}
            onClick={() => goTo(i)}
            aria-label={`Slide ${i + 1}`}
            className="transition-all duration-300 rounded-full"
            style={{
              width: i === current ? "32px" : "12px",
              height: "12px",
              background: i === current ? "#facc15" : "#ffffff55",
            }}
          />
        ))}
      </div>
    </section>
  );
}
