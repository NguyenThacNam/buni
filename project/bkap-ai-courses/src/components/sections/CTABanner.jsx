import React from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { fadeInUp } from "../../utils/animations";
import Button from "../ui/Button";

export default function CTABanner() {
  return (
    <section 
      className="bg-primary py-16 text-white text-center relative overflow-hidden" 
      aria-label="Liên hệ đăng ký ngay"
    >
      {/* Background Graphic Blobs */}
      <div className="absolute top-0 left-0 w-32 h-32 bg-white/5 rounded-full -translate-x-12 -translate-y-12 pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-64 h-64 bg-white/5 rounded-full translate-x-24 translate-y-24 pointer-events-none" />

      <div className="max-w-4xl mx-auto px-4 relative z-10">
        <motion.div
          variants={fadeInUp}
          initial="initial"
          whileInView="whileInView"
          viewport={fadeInUp.viewport}
          transition={fadeInUp.transition}
        >
          <h2 className="font-heading text-3xl md:text-4xl font-extrabold mb-4 tracking-tight leading-tight">
            Khởi Đầu Sự Nghiệp CNTT Quốc Tế Ngay Hôm Nay!
          </h2>
          <p className="font-sans text-base md:text-lg text-red-100 mb-8 max-w-xl mx-auto leading-relaxed">
            Nhận lộ trình học tập chi tiết và thông tin ưu đãi học phí lên tới 50% từ Bachkhoa-Aptech.
          </p>
          <Link to="/contact" className="inline-block focus-ring rounded-full">
            <Button variant="accent">
              Đăng ký nhận tư vấn miễn phí
            </Button>
          </Link>
        </motion.div>
      </div>
    </section>
  );
}
