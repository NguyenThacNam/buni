import React, { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import {
  ShieldCheck,
  Tag,
  ChevronRight,
  Loader2,
  AlertCircle,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";
import { getCourseByIdApi } from "../../api/CourseApi";
import {
  checkPaymentStatusApi,
  checkVoucherApi,
  createOrderApi,
} from "../../api/PaymentApi";
import { formatPrice, PAYMENT_METHODS } from "../../utils/paymentHelpers";

export default function CheckoutPage() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [course, setCourse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedMethod, setSelected] = useState("vnpay");
  const [voucher, setVoucher] = useState("");
  const [voucherStatus, setVStatus] = useState(null); // null | "checking" | "valid" | "invalid"
  const [discount, setDiscount] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  // ── Fetch thông tin khóa học ──────────────────────────
  useEffect(() => {
    if (!user) {
      navigate("/");
      return;
    }
    // THÊM ĐIỀU KIỆN NÀY: Nếu không có hoặc chưa lấy được courseId từ URL, không gọi API bừa bãi
    if (!courseId || courseId === "undefined") {
      console.error("Không tìm thấy Course ID trên URL");
      return;
    }

    getCourseByIdApi(courseId)
      .then((res) => setCourse(res.data))
      .catch(() => navigate("/"))
      .finally(() => setLoading(false));
  }, [courseId, user, navigate]);

  // ── Kiểm tra voucher ─────────────────────────────────
  const handleCheckVoucher = async () => {
    if (!voucher.trim()) return;
    setVStatus("checking");
    try {
      const res = await checkVoucherApi(voucher.trim());
      setDiscount(res.data.discountAmount);
      setVStatus("valid");
    } catch {
      setDiscount(0);
      setVStatus("invalid");
    }
  };

  // ── Tính toán giá tiền ───────────────────────────────
  const originalPrice = course?.price ?? 0;
  const finalPrice = Math.max(0, originalPrice - discount);

  // ── Xử lý Xác nhận thanh toán (Tích hợp mock thành công) ──
  // ── Xử lý Xác nhận thanh toán (Đã đồng bộ với useSearchParams) ──
  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const payload = {
        courseId,
        paymentMethod: selectedMethod,
        voucherCode: voucherStatus === "valid" ? voucher : null,
        finalPrice: finalPrice,
      };

      // 1. Gọi API tạo đơn hàng
      const res = await createOrderApi(payload);

      // Nếu Backend thật trả về link cổng thanh toán (VNPay/MoMo) → Đi tới cổng thật
      if (res?.data?.paymentUrl) {
        window.location.href = res.data.paymentUrl;
      } else {
        // 2. LOGIC GIẢ LẬP: Nếu chưa có BE thật, mock dữ liệu tại đây
        const mockOrderId = "ORDER-123";

        // Gọi API kiểm tra trạng thái giả lập của bạn
        await checkPaymentStatusApi(mockOrderId);

        // ĐỒNG BỘ: Chuyển hướng bằng URL Query Parameter để PaymentResultPage nhận diện đúng
        navigate(`/payment/result?status=success&course=${courseId}`);
      }
    } catch (error) {
      // Nếu có lỗi phát sinh trong quá trình gọi API, báo thất bại qua URL
      navigate(`/payment/result?status=fail&course=${courseId}`);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading)
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="animate-spin text-red-600" size={40} />
      </div>
    );

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-5xl mx-auto px-4">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-sm text-gray-500 mb-6">
          <Link to="/" className="hover:text-red-600">
            Trang chủ
          </Link>
          <ChevronRight size={14} />
          <Link to={`/courses/${courseId}`} className="hover:text-red-600">
            {course?.title}
          </Link>
          <ChevronRight size={14} />
          <span className="text-gray-800 font-medium">Thanh toán</span>
        </nav>

        <h1 className="text-2xl font-bold font-[Montserrat] text-gray-900 mb-8">
          Xác nhận đăng ký khóa học
        </h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Cột trái: Phương thức thanh toán + Voucher */}
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="font-semibold text-lg mb-4">
                Phương thức thanh toán
              </h2>
              <div className="space-y-3">
                {PAYMENT_METHODS.map((method) => (
                  <motion.label
                    key={method.id}
                    whileHover={{ scale: 1.01 }}
                    className={`flex items-center gap-4 p-4 rounded-lg border-2 cursor-pointer transition-all ${
                      selectedMethod === method.id
                        ? "border-red-600 bg-red-50"
                        : "border-gray-200 hover:border-gray-300"
                    }`}
                  >
                    <input
                      type="radio"
                      name="payment"
                      value={method.id}
                      checked={selectedMethod === method.id}
                      onChange={() => setSelected(method.id)}
                      className="accent-red-600"
                    />
                    <span className="text-2xl">{method.icon}</span>
                    <div>
                      <p
                        className="font-semibold"
                        style={{ color: method.color }}
                      >
                        {method.name}
                      </p>
                      <p className="text-sm text-gray-500">{method.desc}</p>
                    </div>
                    {selectedMethod === method.id && (
                      <ShieldCheck className="ml-auto text-red-600" size={20} />
                    )}
                  </motion.label>
                ))}
              </div>
            </div>

            {/* Voucher */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="font-semibold text-lg mb-4 flex items-center gap-2">
                <Tag size={18} className="text-red-600" /> Mã giảm giá
              </h2>
              <div className="flex gap-3">
                <input
                  type="text"
                  placeholder="Nhập mã voucher..."
                  value={voucher}
                  onChange={(e) => {
                    setVoucher(e.target.value.toUpperCase());
                    setVStatus(null);
                    setDiscount(0);
                  }}
                  className="flex-1 border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
                />
                <button
                  onClick={handleCheckVoucher}
                  disabled={!voucher || voucherStatus === "checking"}
                  className="bg-red-600 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition"
                >
                  {voucherStatus === "checking" ? (
                    <Loader2 className="animate-spin" size={16} />
                  ) : (
                    "Áp dụng"
                  )}
                </button>
              </div>

              <AnimatePresence>
                {voucherStatus === "valid" && (
                  <motion.p
                    initial={{ opacity: 0, y: -6 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="mt-2 text-green-600 text-sm font-medium"
                  >
                    ✅ Áp dụng thành công! Giảm {formatPrice(discount)}
                  </motion.p>
                )}
                {voucherStatus === "invalid" && (
                  <motion.p
                    initial={{ opacity: 0, y: -6 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="mt-2 text-red-500 text-sm flex items-center gap-1"
                  >
                    <AlertCircle size={14} /> Mã không hợp lệ hoặc đã hết hạn
                  </motion.p>
                )}
              </AnimatePresence>
            </div>
          </div>

          {/* Cột phải: Tóm tắt đơn hàng */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-sm p-6 sticky top-24">
              <h2 className="font-semibold text-lg mb-4">Tóm tắt đơn hàng</h2>

              <div className="flex gap-3 mb-5 pb-5 border-b">
                <img
                  src={
                    course?.thumbnailUrl ||
                    `https://placehold.co/80x54/1A1A2E/FFFFFF?text=BKAP`
                  }
                  alt={course?.title}
                  className="w-20 h-14 rounded-lg object-cover"
                />
                <div>
                  <p className="font-medium text-sm leading-snug line-clamp-2">
                    {course?.title}
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    {typeof course?.instructor === "object"
                      ? course?.instructor?.name || course?.instructor?.fullname
                      : course?.instructor}
                  </p>
                </div>
              </div>

              <div className="space-y-2 text-sm mb-5">
                <div className="flex justify-between text-gray-600">
                  <span>Giá gốc</span>
                  <span>{formatPrice(originalPrice)}</span>
                </div>
                {discount > 0 && (
                  <div className="flex justify-between text-green-600">
                    <span>Giảm giá (voucher)</span>
                    <span>- {formatPrice(discount)}</span>
                  </div>
                )}
                <div className="flex justify-between font-bold text-base pt-2 border-t">
                  <span>Tổng thanh toán</span>
                  <span className="text-red-600">
                    {formatPrice(finalPrice)}
                  </span>
                </div>
              </div>

              {/* Nút bấm duy nhất gọi hàm handleSubmit đã tích hợp mock */}
              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={handleSubmit}
                disabled={submitting}
                className="w-full bg-red-600 hover:bg-red-700 text-white font-semibold py-3 rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-60"
              >
                {submitting ? (
                  <>
                    <Loader2 className="animate-spin" size={18} /> Đang xử lý...
                  </>
                ) : (
                  `Thanh toán ${formatPrice(finalPrice)}`
                )}
              </motion.button>

              <p className="text-xs text-gray-400 text-center mt-3 flex items-center justify-center gap-1">
                <ShieldCheck size={12} /> Giao dịch được mã hóa & bảo mật
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
