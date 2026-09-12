import React from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { fadeInUp } from "../../utils/animations";
import Button from "../ui/Button";

export default function HeroSection() {
  return (
    <section
      className="relative bg-gradient-to-br from-navy to-primary-dark overflow-hidden py-20 lg:py-32 flex items-center min-h-[80vh]"
      aria-label="Giới thiệu Bachkhoa-Aptech"
    >
      {/* Background Dot Grid SVG */}
      <div className="absolute inset-0 bg-dot-grid opacity-20 pointer-events-none" />

      {/* Decorative Blob */}
      <div className="absolute -right-20 -bottom-20 w-80 h-80 rounded-full bg-accent opacity-10 blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 relative z-10 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Left Column: Text & CTAs */}
          <div className="text-center lg:text-left text-white">
            <motion.h1
              variants={fadeInUp}
              initial="initial"
              whileInView="whileInView"
              viewport={fadeInUp.viewport}
              transition={fadeInUp.transition}
              className="font-heading text-4xl md:text-5xl lg:text-6xl font-extrabold leading-tight tracking-tight mb-6 bg-clip-text bg-gradient-to-r from-white via-gray-100 to-red-200"
            >
              Hệ thống Đào tạo <br />
              <span className="text-accent">CNTT & AI Quốc tế</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: 0.2, ease: "easeOut" }}
              className="font-sans text-lg text-gray-300 mb-8 max-w-xl mx-auto lg:mx-0 leading-relaxed"
            >
              Hơn 23 năm kiến tạo và dẫn đầu chất lượng đào tạo công nghệ thông tin. Cam kết việc làm
              bằng văn bản, sẵn sàng làm chủ làn sóng AI toàn cầu.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: 0.4, ease: "easeOut" }}
              className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4"
            >
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
            </motion.div>
          </div>

          {/* Right Column: Abstract SVG Illustration (Hidden on mobile) */}
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, ease: "easeOut" }}
            className="hidden lg:flex justify-center"
            aria-hidden="true"
          >
            <svg
              className="w-full max-w-lg h-auto text-white/10"
              viewBox="0 0 500 500"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              {/* Outer Glow Ring */}
              <circle
                cx="250"
                cy="250"
                r="180"
                stroke="currentColor"
                strokeWidth="2"
                strokeDasharray="8 8"
                className="animate-spin"
                style={{ animationDuration: "60s" }}
              />
              {/* Inner Glowing Nodes */}
              <circle cx="250" cy="250" r="140" stroke="#C8102E" strokeWidth="3" opacity="0.3" />
              <circle cx="250" cy="250" r="100" stroke="#F59E0B" strokeWidth="2" strokeDasharray="4 4" />

              {/* Center Core node */}
              <circle cx="250" cy="250" r="40" fill="#C8102E" />
              <circle cx="250" cy="250" r="20" fill="#F59E0B" />

              {/* Orbiting nodes */}
              <circle cx="150" cy="150" r="12" fill="#FFFFFF" className="animate-pulse" />
              <line x1="250" y1="250" x2="150" y2="150" stroke="currentColor" strokeWidth="2" opacity="0.5" />

              <circle cx="370" cy="200" r="8" fill="#F59E0B" />
              <line x1="250" y1="250" x2="370" y2="200" stroke="currentColor" strokeWidth="2" opacity="0.5" />

              <circle cx="200" cy="360" r="14" fill="#C8102E" />
              <line x1="250" y1="250" x2="200" y2="360" stroke="currentColor" strokeWidth="2" opacity="0.5" />

              <circle cx="340" cy="320" r="10" fill="#FFFFFF" />
              <line x1="250" y1="250" x2="340" y2="320" stroke="currentColor" strokeWidth="2" opacity="0.5" />
            </svg>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
