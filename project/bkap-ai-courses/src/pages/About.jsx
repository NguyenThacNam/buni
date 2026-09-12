import React, { useEffect, useRef } from "react";
import { useInView, motion, useScroll, useTransform } from "framer-motion";
import { Eye, Target, Sparkles, Award } from "lucide-react";
import { team } from "../data/team";
import { fadeInUp, slideInLeft, slideInRight } from "../utils/animations";
import SectionTitle from "../components/ui/SectionTitle";
import StatCard from "../components/ui/StatCard";
import LazyImage from "../components/ui/LazyImage";

export default function About() {
  const statsRef = useRef(null);
  const isStatsInView = useInView(statsRef, { once: true, amount: 0.2 });

  useEffect(() => {
    document.title = "Giới thiệu | BKAP AI – 23 năm kinh nghiệm";
  }, []);

  const timelineEvents = [
    {
      year: "2001",
      title: "Thành lập Bachkhoa-Aptech",
      description:
        "Hợp tác chính thức với tập đoàn Aptech Ấn Độ, bắt đầu sứ mệnh đào tạo công nghệ thông tin chuẩn quốc tế tại Việt Nam.",
    },
    {
      year: "2010",
      title: "Cột mốc 5.000 Học viên",
      description:
        "Mở rộng hệ thống giảng đường, ký kết hợp tác cung ứng nhân sự chất lượng cao với hơn 100 doanh nghiệp phần mềm.",
    },
    {
      year: "2018",
      title: "Mô hình Làm trước học sau",
      description:
        "Tiên phong đưa mô hình đào tạo thực chiến 'Làm trước học sau' tích hợp dự án doanh nghiệp thực tế vào giáo trình.",
    },
    {
      year: "2024",
      title: "Ra mắt BKAP AI Academy",
      description:
        "Chuyển mình đón đầu xu hướng trí tuệ nhân tạo, thiết lập chương trình học AI & Data Science chất lượng cao chuyên sâu.",
    },
  ];

  const partners = [
    { name: "FPT Software", text: "FPT" },
    { name: "Viettel Group", text: "Viettel" },
    { name: "CMC Technology", text: "CMC" },
    { name: "VNG Corporation", text: "VNG" },
    { name: "Rikkeisoft", text: "Rikkei" },
    { name: "MISA", text: "MISA" },
  ];

  const { scrollY } = useScroll();
  const y1 = useTransform(scrollY, [0, 2000], [0, 300]);
  const y2 = useTransform(scrollY, [0, 2000], [0, -200]);

  return (
    <main className="min-h-screen bg-bg relative overflow-hidden">
      {/* Parallax Background Blobs */}
      <motion.div
        className="absolute top-[20%] left-[-5%] w-[400px] h-[400px] rounded-full bg-primary/5 blur-[100px] -z-10 pointer-events-none"
        style={{ y: y1 }}
      />
      <motion.div
        className="absolute top-[60%] right-[-10%] w-[500px] h-[500px] rounded-full bg-accent/5 blur-[120px] -z-10 pointer-events-none"
        style={{ y: y2 }}
      />
      {/* Page Title Header */}
      <section className="bg-navy text-white py-16 relative" aria-label="Giới thiệu chung">
        <div className="absolute inset-0 bg-dot-grid opacity-10 pointer-events-none" />
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 relative z-10 text-center">
          <h1 className="font-heading text-4xl font-extrabold mb-4">
            Về Chúng Tôi
          </h1>
          <p className="font-sans text-base text-gray-300 max-w-xl mx-auto leading-relaxed">
            Hành trình 23 năm kiến tạo nguồn nhân lực CNTT chất lượng cao, đồng hành cùng thế hệ trẻ
            chinh phục đỉnh cao công nghệ.
          </p>
        </div>
      </section>

      {/* Mission & Vision Section */}
      <section className="py-20 bg-white" aria-label="Sứ mệnh và tầm nhìn">
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
            {/* Mission Card */}
            <motion.div
              variants={fadeInUp}
              initial="initial"
              whileInView="whileInView"
              viewport={fadeInUp.viewport}
              transition={fadeInUp.transition}
              className="bg-bg p-8 rounded-card border border-gray-100 shadow-card flex flex-col h-full"
            >
              <div className="w-12 h-12 bg-red-50 text-primary rounded-xl flex items-center justify-center mb-6">
                <Target className="w-6 h-6" />
              </div>
              <h2 className="font-heading text-xl font-bold text-gray-900 mb-4">
                Sứ Mệnh Của Chúng Tôi
              </h2>
              <p className="font-sans text-sm md:text-base text-gray-600 leading-relaxed">
                Đem đến mô hình đào tạo CNTT thực chiến hiệu quả nhất, giúp học viên trang bị kỹ năng
                nghề vững chắc, tư duy sáng tạo và bằng cấp quốc tế để tự tin lập nghiệp toàn cầu.
              </p>
            </motion.div>

            {/* Vision Card */}
            <motion.div
              variants={fadeInUp}
              initial="initial"
              whileInView="whileInView"
              viewport={fadeInUp.viewport}
              transition={fadeInUp.transition}
              className="bg-bg p-8 rounded-card border border-gray-100 shadow-card flex flex-col h-full"
            >
              <div className="w-12 h-12 bg-red-50 text-primary rounded-xl flex items-center justify-center mb-6">
                <Eye className="w-6 h-6" />
              </div>
              <h2 className="font-heading text-xl font-bold text-gray-900 mb-4">
                Tầm Nhìn Đến Năm 2030
              </h2>
              <p className="font-sans text-sm md:text-base text-gray-600 leading-relaxed">
                Trở thành hệ thống đào tạo công nghệ thông tin và trí tuệ nhân tạo (AI) hàng đầu khu
                vực, liên tục cung ứng nguồn nhân sự chất lượng cao đáp ứng tiêu chuẩn khắt khe của
                cuộc cách mạng công nghiệp 4.0.
              </p>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Chronological Timeline Section (23 Years) */}
      <section className="py-20 bg-bg overflow-hidden" aria-label="Hành trình phát triển">
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
          <SectionTitle
            title="Hành Trình 23 Năm Kiến Tạo"
            subtitle="Cột mốc lịch sử"
            align="center"
          />

          <div className="relative border-l-2 border-primary/20 md:border-l-0 md:before:absolute md:before:left-1/2 md:before:top-0 md:before:bottom-0 md:before:w-[2px] md:before:bg-primary/20 space-y-12">
            {timelineEvents.map((event, index) => {
              const isEven = index % 2 === 0;

              return (
                <div key={event.year} className="relative flex flex-col md:flex-row md:items-center">
                  {/* Left Column Spacer (Desktop) or Card */}
                  <div className={`w-full md:w-1/2 pl-8 md:pl-0 md:pr-8 ${isEven ? "md:text-right" : "md:order-2 md:text-left md:pl-8"}`}>
                    <motion.div
                      variants={isEven ? slideInLeft : slideInRight}
                      initial="initial"
                      whileInView="whileInView"
                      viewport={{ once: true }}
                      className="bg-white p-6 rounded-card border border-gray-100 shadow-card"
                    >
                      <span className="inline-block bg-primary text-white font-heading text-sm font-extrabold px-3 py-1 rounded-full mb-3">
                        {event.year}
                      </span>
                      <h3 className="font-heading text-lg font-bold text-gray-900 mb-2">
                        {event.title}
                      </h3>
                      <p className="font-sans text-sm text-gray-600 leading-relaxed">
                        {event.description}
                      </p>
                    </motion.div>
                  </div>

                  {/* Dot Marker */}
                  <div className="absolute left-[-9px] top-4 md:left-1/2 md:-translate-x-1/2 md:top-1/2 md:-translate-y-1/2 z-10">
                    <div className="w-4 h-4 rounded-full bg-primary border-4 border-white shadow-md" />
                  </div>

                  {/* Right Column Spacer (Desktop) */}
                  <div className="hidden md:block md:w-1/2" />
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Leadership & Lecturers Team Grid */}
      <section className="py-20 bg-white" aria-label="Đội ngũ giảng viên">
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
          <SectionTitle
            title="Ban Giám Đốc & Giảng Viên"
            subtitle="Đội ngũ chuyên gia"
            align="center"
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
            {team.map((member) => (
              <div
                key={member.id}
                className="bg-white p-6 rounded-card border border-gray-100 shadow-card hover:shadow-card-hover transition-all duration-300 text-center flex flex-col items-center"
              >
                <LazyImage
                  src={member.avatar}
                  alt={member.name}
                  className="w-24 h-24 rounded-full border-2 border-primary mb-4"
                />
                <h3 className="font-heading font-bold text-gray-900 text-lg">
                  {member.name}
                </h3>
                <p className="font-sans text-xs text-primary font-bold mb-3 uppercase tracking-wider">
                  {member.role}
                </p>
                <p className="font-sans text-sm text-gray-500 leading-relaxed">
                  {member.bio}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Animated Counter Stats */}
      <section
        ref={statsRef}
        className="bg-primary py-16 text-white text-center"
        aria-label="Số liệu thống kê"
      >
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-8">
            <StatCard value="23" suffix=" năm" label="Thành lập & phát triển" inView={isStatsInView} />
            <StatCard value="98" suffix="%" label="Tỷ lệ sinh viên có việc làm" inView={isStatsInView} />
            <StatCard value="10000" suffix="+" label="Học viên đã tốt nghiệp" inView={isStatsInView} />
          </div>
        </div>
      </section>

      {/* Partner Logos Section */}
      <section className="py-20 bg-white" aria-label="Doanh nghiệp đối tác">
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
          <SectionTitle
            title="500+ Doanh Nghiệp Đối Tác Cung Ứng"
            subtitle="Mạng lưới kết nối"
            align="center"
          />

          <div className="flex flex-wrap items-center justify-center gap-8 md:gap-12">
            {partners.map((partner, index) => (
              <div
                key={index}
                className="w-32 h-16 bg-bg border border-gray-100 rounded-xl flex items-center justify-center shadow-sm select-none filter grayscale hover:grayscale-0 hover:scale-105 hover:border-primary/20 transition-all duration-300 cursor-default"
                aria-label={`Logo đối tác ${partner.name}`}
              >
                <span className="font-heading font-extrabold text-lg text-gray-400 hover:text-primary transition-colors">
                  {partner.text}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}
