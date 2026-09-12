import React, { useEffect, useState } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import { motion } from "framer-motion";
import { CheckCircle2, XCircle, BookOpen, RotateCcw } from "lucide-react";
import { LMS_URL } from "../../data/constants";

export default function PaymentResultPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const status = params.get("status"); // Đọc trực tiếp từ ?status=success hoặc ?status=fail
  const courseId = params.get("course");
  const isSuccess = status === "success";
  const [count, setCount] = useState(5);

  // Hàm xử lý nhảy sang hệ thống LMS bên ngoài
  const handleGoToLMS = () => {
    window.location.href = `${LMS_URL}/login/index.php`;
  };

  // ── Tự động chuyển hướng sang LMS sau 5 giây nếu thành công ─────────────────
  useEffect(() => {
    if (!isSuccess) return;
    const timer = setInterval(() => {
      setCount((c) => {
        if (c <= 1) {
          clearInterval(timer);
          handleGoToLMS(); // Thay vì về dashboard nội bộ, tự động đẩy ra LMS
        }
        return c - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [isSuccess]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.4 }}
        className="bg-white rounded-2xl shadow-lg p-10 max-w-md w-full text-center"
      >
        {isSuccess ? (
          <>
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: "spring", stiffness: 200, delay: 0.2 }}
              className="flex justify-center mb-6"
            >
              <CheckCircle2 size={72} className="text-green-500" />
            </motion.div>

            <h1 className="text-2xl font-bold font-[Montserrat] text-gray-900 mb-2">
              Thanh toán thành công! 🎉
            </h1>

            <p className="text-gray-500 mb-6">
              Khóa học đã được thêm vào tài khoản của bạn.
              <br />
              Chuyển hướng sang LMS sau{" "}
              <span className="font-bold text-red-600">{count}s</span>...
            </p>

            {/* Thay nút Link nội bộ bằng nút bấm chuyển thẳng ra LMS */}
            <button
              onClick={handleGoToLMS}
              className="inline-flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white font-semibold px-6 py-3 rounded-xl transition w-full justify-center"
            >
              <BookOpen size={18} /> Vào học ngay trên LMS
            </button>
          </>
        ) : (
          <>
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: "spring", stiffness: 200, delay: 0.2 }}
              className="flex justify-center mb-6"
            >
              <XCircle size={72} className="text-red-500" />
            </motion.div>

            <h1 className="text-2xl font-bold font-[Montserrat] text-gray-900 mb-2">
              Thanh toán thất bại
            </h1>

            <p className="text-gray-500 mb-6">
              Giao dịch không thành công. Vui lòng kiểm tra lại thông tin và thử
              lại.
            </p>

            <div className="flex gap-3 justify-center">
              {/* Quay lại trang trước (Trang Checkout cũ) */}
              <button
                onClick={() => navigate(-1)}
                className="inline-flex items-center gap-2 border border-red-600 text-red-600 hover:bg-red-50 font-semibold px-5 py-3 rounded-xl transition"
              >
                <RotateCcw size={16} /> Thử lại
              </button>
              <Link
                to="/courses"
                className="inline-flex items-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold px-5 py-3 rounded-xl transition"
              >
                Xem khóa học khác
              </Link>
            </div>
          </>
        )}
      </motion.div>
    </div>
  );
}
