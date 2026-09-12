import React from "react";
import { Swiper, SwiperSlide } from "swiper/react";
import { Autoplay, Pagination } from "swiper/modules";
import { testimonials } from "../../data/testimonials";
import TestimonialCard from "../ui/TestimonialCard";
import SectionTitle from "../ui/SectionTitle";

// Swiper styles
import "swiper/css";
import "swiper/css/pagination";

export default function Testimonials() {
  return (
    <section className="py-20 bg-bg" aria-label="Cảm nhận từ học viên">
      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
        <SectionTitle
          title="Học Viên Thành Đạt Nói Gì?"
          subtitle="Cảm nhận cựu học viên"
          align="center"
        />

        <div className="relative">
          <Swiper
            modules={[Autoplay, Pagination]}
            spaceBetween={30}
            slidesPerView={1}
            autoplay={{
              delay: 4000,
              disableOnInteraction: false,
              pauseOnMouseEnter: true,
            }}
            pagination={{
              clickable: true,
              dynamicBullets: true,
            }}
            breakpoints={{
              768: {
                slidesPerView: 2,
              },
              1024: {
                slidesPerView: 3,
              },
            }}
            className="pb-16"
            aria-label="Lời chứng thực từ học viên"
          >
            {testimonials.map((item) => (
              <SwiperSlide key={item.id} className="h-full flex">
                <div className="w-full flex-grow">
                  <TestimonialCard
                    name={item.name}
                    role={item.role}
                    course={item.course}
                    content={item.content}
                    rating={item.rating}
                    avatar={item.avatar}
                  />
                </div>
              </SwiperSlide>
            ))}
          </Swiper>
        </div>
      </div>
    </section>
  );
}
