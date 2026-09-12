import React, { useEffect } from "react";
import { Link } from "react-router-dom";
import { Frown } from "lucide-react";
import Button from "../components/ui/Button";

export default function NotFound() {
  useEffect(() => {
    document.title = "404 – Trang không tồn tại | BKAP AI";
  }, []);

  return (
    <main 
      className="min-h-[70vh] bg-bg flex flex-col items-center justify-center text-center px-4 py-16"
      aria-label="Lỗi 404 không tìm thấy trang"
    >
      {/* Abstract warning illustration */}
      <div 
        className="w-24 h-24 rounded-full bg-red-50 flex items-center justify-center text-primary mb-6 animate-bounce"
        aria-hidden="true"
      >
        <Frown className="w-12 h-12" />
      </div>

      {/* Large status code heading */}
      <h1 className="font-heading text-6xl md:text-8xl font-extrabold text-navy mb-4 tracking-tight">
        404
      </h1>

      <h2 className="font-heading text-xl md:text-2xl font-bold text-gray-900 mb-2">
        Trang Không Tồn Tại
      </h2>
      
      <p className="font-sans text-sm md:text-base text-gray-600 mb-8 max-w-sm leading-relaxed">
        Đường dẫn bạn truy cập có thể đã bị thay đổi, bị xóa hoặc không chính xác. Hãy quay về trang
        chủ để tìm kiếm lộ trình phù hợp.
      </p>

      {/* Back to Home CTA */}
      <Link to="/" className="focus-ring rounded-full">
        <Button variant="primary">
          Về trang chủ
        </Button>
      </Link>
    </main>
  );
}
