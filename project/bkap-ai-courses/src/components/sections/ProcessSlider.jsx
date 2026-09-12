import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";

const slides = [
  {
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

export default function ProcessSlider() {
  const [current, setCurrent] = useState(0);
  const [direction, setDirection] = useState(1);

  const total = slides.length;

  const goTo = (idx) => {
    setDirection(idx > current ? 1 : -1);
    setCurrent(idx);
  };

  const prev = () => goTo((current - 1 + total) % total);
  const next = () => goTo((current + 1) % total);

  // Auto-play
  useEffect(() => {
    const timer = setInterval(next, 5000);
    return () => clearInterval(timer);
  }, [current]);

  const slide = slides[current];

  const variants = {
    enter: (dir) => ({ opacity: 0, x: dir > 0 ? 60 : -60 }),
    center: { opacity: 1, x: 0 },
    exit: (dir) => ({ opacity: 0, x: dir > 0 ? -60 : 60 }),
  };

  return (
    <section
      className="relative w-full overflow-hidden py-16"
      style={{
        background:
          "linear-gradient(135deg, #2d0a6e 0%, #6b21a8 50%, #9333ea 100%)",
        minHeight: "520px",
      }}
      aria-label="Quy trình đào tạo"
    >
      {/* City silhouette overlay */}
      <div
        className="absolute inset-0 opacity-10 pointer-events-none"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 320'%3E%3Cpath fill='%23ffffff' d='M0,160L48,149.3C96,139,192,117,288,128C384,139,480,181,576,181.3C672,181,768,139,864,122.7C960,107,1056,117,1152,138.7C1248,160,1344,192,1392,208L1440,224L1440,320L0,320Z'/%3E%3C/svg%3E")`,
          backgroundSize: "cover",
          backgroundPosition: "bottom",
        }}
      />

      {/* Header text */}
      <div className="relative z-10 text-center mb-10 px-4">
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

      {/* Slide content */}
      <AnimatePresence custom={direction} mode="wait">
        <motion.div
          key={current}
          custom={direction}
          variants={variants}
          initial="enter"
          animate="center"
          exit="exit"
          transition={{ duration: 0.45, ease: "easeInOut" }}
          className="relative z-10 max-w-7xl mx-auto px-4 md:px-8"
        >
          {/* Steps row */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6 mb-0">
            {slide.steps.map((step, i) => (
              <div
                key={step.number}
                className="flex flex-col items-center gap-4"
              >
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
        </motion.div>
      </AnimatePresence>

      {/* Prev / Next buttons */}
      <button
        onClick={prev}
        aria-label="Slide trước"
        className="absolute left-4 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full bg-white/20 hover:bg-white/40 flex items-center justify-center text-white transition-all duration-300"
      >
        <ChevronLeft className="w-5 h-5" />
      </button>
      <button
        onClick={next}
        aria-label="Slide tiếp theo"
        className="absolute right-4 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full bg-white/20 hover:bg-white/40 flex items-center justify-center text-white transition-all duration-300"
      >
        <ChevronRight className="w-5 h-5" />
      </button>

      {/* Dots */}
      <div className="relative z-10 flex justify-center gap-2 mt-8">
        {slides.map((_, i) => (
          <button
            key={i}
            onClick={() => goTo(i)}
            aria-label={`Slide ${i + 1}`}
            className="transition-all duration-300 rounded-full"
            style={{
              width: i === current ? "24px" : "10px",
              height: "10px",
              background: i === current ? "#facc15" : "#ffffff55",
            }}
          />
        ))}
      </div>
    </section>
  );
}
