import React, { useEffect } from "react";
import { motion, useScroll, useTransform } from "framer-motion";
import HeroSlider from "../components/sections/HeroSlider";
import StatsBar from "../components/sections/StatsBar";
import FeaturedCourses from "../components/sections/FeaturedCourses";
import WhyBKAP from "../components/sections/WhyBKAP";
import Testimonials from "../components/sections/Testimonials";
import CTABanner from "../components/sections/CTABanner";

export default function Home() {
  useEffect(() => {
    document.title = "BKAP AI – Đào tạo CNTT Quốc tế | Bachkhoa-Aptech";
  }, []);

  const { scrollY } = useScroll();
  const y1 = useTransform(scrollY, [0, 2000], [0, 400]);
  const y2 = useTransform(scrollY, [0, 2000], [0, -300]);

  return (
    <main className="relative overflow-hidden">
      {/* Parallax Blobs Background */}
      <motion.div
        className="absolute top-[10%] left-[-10%] w-[500px] h-[500px] rounded-full bg-primary/5 blur-[100px] -z-10 pointer-events-none"
        style={{ y: y1 }}
      />
      <motion.div
        className="absolute top-[40%] right-[-5%] w-[400px] h-[400px] rounded-full bg-accent/5 blur-[100px] -z-10 pointer-events-none"
        style={{ y: y2 }}
      />
      <motion.div
        className="absolute top-[70%] left-[20%] w-[600px] h-[600px] rounded-full bg-blue-500/5 blur-[120px] -z-10 pointer-events-none"
        style={{ y: y1 }}
      />

      <HeroSlider />
      <StatsBar />
      <FeaturedCourses />
      <WhyBKAP />
      <Testimonials />
      <CTABanner />
    </main>
  );
}
