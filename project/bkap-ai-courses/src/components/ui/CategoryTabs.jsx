import React, { useState, useEffect, useMemo } from "react";
import { getAllCategoriesApi } from "../../api/CourseApi";
import { RefreshCw } from "lucide-react";
import { dungCayDanhMuc } from "../../utils/categoryTree";

const KIEU_TAB_CHON = "bg-[#C40D2E] text-white shadow-md transform scale-105";
const KIEU_TAB_THUONG =
  "border border-gray-200 text-gray-700 bg-white hover:bg-gray-50 hover:border-gray-300";

export default function CategoryTabs({ activeCategory, onSelectCategory }) {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchCategories = () => {
    setLoading(true);
    setError(false);
    getAllCategoriesApi()
      .then((res) => {
        const data = Array.isArray(res.data) ? res.data : [];
        setCategories(data);
      })
      .catch((err) => {
        console.error("Failed to load categories:", err);
        setError(true);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  // Hàng trên chỉ hiện danh mục gốc. Chọn một nhánh có danh mục con thì hiện
  // thêm hàng dưới để lọc tiếp — trải hết ra một hàng thì 15 nút dồn cục, người
  // xem không nhận ra "Cho doanh nghiệp" là con của "Chuyển đổi số".
  const cay = useMemo(() => dungCayDanhMuc(categories), [categories]);
  const gocDangChon = activeCategory === "all" ? null : cay.gocCua(activeCategory);
  const conCuaGoc = gocDangChon ? cay.conChau(gocDangChon.id) : [];

  if (loading) {
    return (
      <div className="w-full flex items-center gap-3 overflow-x-auto pb-4 hide-scrollbar">
        {/* Skeleton for "Tất cả" */}
        <div className="h-10 w-24 bg-gray-200 rounded-full animate-pulse flex-shrink-0" />
        {/* Skeletons for other categories */}
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="h-10 w-32 bg-gray-200 rounded-full animate-pulse flex-shrink-0"
            style={{ width: `${Math.random() * 40 + 100}px` }} // Random width for varied look
          />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full flex flex-col items-center justify-center py-6 gap-3">
        <p className="text-gray-500 font-medium text-sm">
          Không thể tải danh mục. Vui lòng thử lại sau.
        </p>
        <button
          onClick={fetchCategories}
          className="flex items-center gap-2 px-5 py-2 rounded-full border border-gray-200 text-gray-700 bg-white hover:bg-gray-50 transition-colors text-sm font-semibold"
        >
          <RefreshCw className="w-4 h-4" />
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="w-full relative">
      <div className="flex items-center gap-2.5 overflow-x-auto pb-4 hide-scrollbar scroll-smooth">
        {/* Tab "Tất cả" */}
        <button
          onClick={() => onSelectCategory("all")}
          className={`flex-shrink-0 px-6 py-2 rounded-full text-sm font-bold transition-all duration-300 ${
            activeCategory === "all" ? KIEU_TAB_CHON : KIEU_TAB_THUONG
          }`}
        >
          Tất cả
        </button>

        {/* Danh mục gốc. Đang xem một danh mục con thì tab gốc của nó vẫn sáng,
            để người xem biết mình đang ở nhánh nào. */}
        {cay.goc.map((cat) => {
          const isActive = gocDangChon?.id === cat.id;
          return (
            <button
              key={cat.id || cat.slug}
              onClick={() => onSelectCategory(cat.slug)}
              className={`flex-shrink-0 px-6 py-2 rounded-full text-sm font-bold transition-all duration-300 ${
                isActive ? KIEU_TAB_CHON : KIEU_TAB_THUONG
              }`}
            >
              {cat.name}
            </button>
          );
        })}
      </div>

      {/* Hàng danh mục con của nhánh đang chọn */}
      {conCuaGoc.length > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-4 -mt-1 hide-scrollbar">
          <span className="flex-shrink-0 text-xs font-semibold uppercase tracking-wider text-gray-400 mr-1">
            Lọc tiếp
          </span>
          <button
            onClick={() => onSelectCategory(gocDangChon.slug)}
            className={`flex-shrink-0 px-4 py-1.5 rounded-full text-xs font-bold transition-colors ${
              activeCategory === gocDangChon.slug
                ? "bg-gray-900 text-white"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            Tất cả trong nhánh
          </button>
          {conCuaGoc.map((con) => (
            <button
              key={con.id}
              onClick={() => onSelectCategory(con.slug)}
              className={`flex-shrink-0 px-4 py-1.5 rounded-full text-xs font-bold transition-colors ${
                activeCategory === con.slug
                  ? "bg-gray-900 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {/* Cháu (tầng 3 trở xuống) thì thêm dấu › cho khỏi lẫn với con */}
              {con.doSau > 1 ? "› ".repeat(con.doSau - 1) : ""}
              {con.name}
            </button>
          ))}
        </div>
      )}

      {/* Global Style for hiding scrollbar */}
      <style dangerouslySetInnerHTML={{__html: `
        .hide-scrollbar::-webkit-scrollbar {
          display: none;
        }
        .hide-scrollbar {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
      `}} />
    </div>
  );
}
