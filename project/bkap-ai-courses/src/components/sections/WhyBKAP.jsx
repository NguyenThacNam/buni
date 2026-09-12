import React from "react";
import { motion } from "framer-motion";
import { Terminal, Users, Award, HeartHandshake } from "lucide-react";
import { staggerContainer, staggerItem } from "../../utils/animations";
import SectionTitle from "../ui/SectionTitle";

export default function WhyBKAP() {
  const benefits = [
    {
      icon: <Terminal className="w-8 h-8 text-primary" />,
      title: "75% Thời lượng Thực hành",
      description:
        "Học thực tế thông qua các dự án doanh nghiệp (Project-based learning), rèn luyện kỹ năng code thực chiến hàng ngày.",
    },
    {
      icon: <Users className="w-8 h-8 text-primary" />,
      title: "Giảng viên Doanh nghiệp",
      description:
        "Đội ngũ giảng viên là các chuyên gia, Tech Lead và Quản lý dự án trực tiếp dẫn dắt và chia sẻ kinh nghiệm thực tế.",
    },
    {
      icon: <Award className="w-8 h-8 text-primary" />,
      title: "Chứng chỉ Quốc tế",
      description:
        "Bằng cấp Aptech chuẩn quốc tế được công nhận rộng rãi tại hơn 100 quốc gia, mở rộng cơ hội việc làm toàn cầu.",
    },
    {
      icon: <HeartHandshake className="w-8 h-8 text-primary" />,
      title: "Cam kết Hỗ trợ Việc làm",
      description:
        "Ký cam kết hỗ trợ giới thiệu việc làm bằng văn bản ngay khi nhập học. Kết nối trực tiếp đến mạng lưới 500+ doanh nghiệp đối tác.",
    },
  ];

  return (
    <section className="py-20 bg-white" aria-label="Tại sao chọn Bachkhoa-Aptech">
      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
        <SectionTitle
          title="Tại Sao Chọn Bachkhoa-Aptech?"
          subtitle="Ưu thế vượt trội"
          align="center"
        />

        <motion.div
          variants={staggerContainer}
          initial="initial"
          whileInView="whileInView"
          viewport={staggerContainer.viewport}
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8"
        >
          {benefits.map((benefit, index) => (
            <motion.div
              key={index}
              variants={staggerItem}
              whileHover={{ y: -6 }}
              className="bg-bg p-8 rounded-card border border-gray-100 shadow-card hover:shadow-card-hover transition-all duration-300 flex flex-col h-full text-center items-center"
            >
              <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center mb-6">
                {benefit.icon}
              </div>
              <h3 className="font-heading text-lg font-bold text-gray-900 mb-3">
                {benefit.title}
              </h3>
              <p className="font-sans text-sm text-gray-600 leading-relaxed">
                {benefit.description}
              </p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}
