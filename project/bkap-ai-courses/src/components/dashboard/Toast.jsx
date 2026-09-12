import React, { useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CheckCircle, XCircle, X } from "lucide-react";

/**
 * Toast: Thông báo nổi góc trên phải
 * @param {string} message - Nội dung thông báo
 * @param {string} type - "success" | "error"
 * @param {boolean} show - Hiển thị hay không
 * @param {function} onClose - Hàm đóng toast
 */
export default function Toast({ message, type = "success", show, onClose }) {
  // Tự động ẩn sau 3 giây
  useEffect(() => {
    if (show) {
      const timer = setTimeout(onClose, 3000);
      return () => clearTimeout(timer);
    }
  }, [show, onClose]);

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={{ opacity: 0, y: -20, x: 20 }}
          animate={{ opacity: 1, y: 0, x: 0 }}
          exit={{ opacity: 0, y: -20, x: 20 }}
          transition={{ duration: 0.3, ease: "easeOut" }}
          className="fixed top-5 right-5 z-[9999] flex items-center gap-3 px-5 py-3.5 rounded-xl shadow-2xl min-w-[280px] max-w-sm"
          style={{
            background: type === "success"
              ? "linear-gradient(135deg, #065f46, #047857)"
              : "linear-gradient(135deg, #991b1b, #DC2626)",
            color: "#fff",
          }}
        >
          {type === "success" ? (
            <CheckCircle className="w-5 h-5 flex-shrink-0" />
          ) : (
            <XCircle className="w-5 h-5 flex-shrink-0" />
          )}
          <p className="text-sm font-semibold flex-1">{message}</p>
          <button
            onClick={onClose}
            className="ml-2 opacity-70 hover:opacity-100 transition-opacity"
          >
            <X className="w-4 h-4" />
          </button>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
