import React from "react";
import { motion } from "framer-motion";

/**
 * StatsCard: Thẻ thống kê với hiệu ứng fade-in stagger
 * @param {React.Node} icon - Icon component
 * @param {string} label - Nhãn thống kê
 * @param {string|number} value - Giá trị thống kê
 * @param {string} iconBg - Màu nền icon
 * @param {number} index - Vị trí để tính delay stagger
 */
export default function StatsCard({ icon, label, value, iconBg = "#FEE2E2", index = 0 }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.1, ease: "easeOut" }}
      className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100 flex items-center gap-4 hover:shadow-md transition-shadow duration-300"
    >
      {/* Icon box */}
      <div
        className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{ background: iconBg }}
      >
        {icon}
      </div>

      {/* Nội dung */}
      <div>
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-0.5">
          {label}
        </p>
        <p className="text-2xl font-bold text-gray-800">{value}</p>
      </div>
    </motion.div>
  );
}
