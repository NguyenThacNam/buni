import React, { useRef } from "react";
import { useInView } from "framer-motion";
import StatCard from "../ui/StatCard";

export default function StatsBar() {
  const containerRef = useRef(null);
  // Trigger counting animation when the stats block reaches 20% in viewport height
  const isInView = useInView(containerRef, { once: true, amount: 0.2 });

  const stats = [
    { value: "23", suffix: " năm", label: "Kinh nghiệm đào tạo" },
    { value: "98", suffix: "%", label: "Học viên có việc làm" },
    { value: "75", suffix: "%", label: "Làm việc tại Top 500" },
    { value: "10000", suffix: "+", label: "Học viên thành đạt" },
  ];

  return (
    <section
      ref={containerRef}
      className="bg-primary py-12 border-y border-red-700/30"
      aria-label="Thành tựu Bachkhoa-Aptech"
    >
      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-y-8 md:gap-y-0 divide-x-0 md:divide-x divide-white/20">
          {stats.map((stat, index) => (
            <div
              key={index}
              className={`flex justify-center items-center ${
                index % 2 === 0 ? "border-r border-white/10 md:border-r-0" : ""
              }`}
            >
              <StatCard
                value={stat.value}
                suffix={stat.suffix}
                label={stat.label}
                inView={isInView}
              />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
