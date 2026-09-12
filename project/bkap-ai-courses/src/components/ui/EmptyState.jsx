import React from "react";
import PropTypes from "prop-types";
import { SearchX } from "lucide-react";
import Button from "./Button";

export default function EmptyState({ onReset, className = "" }) {
  return (
    <div
      className={`flex flex-col items-center justify-center text-center p-12 bg-white rounded-card shadow-card border border-gray-100 ${className}`}
    >
      <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center text-primary mb-6 animate-bounce">
        <SearchX className="w-8 h-8" />
      </div>
      <h3 className="font-heading text-xl font-semibold text-gray-900 mb-2">
        Không tìm thấy khóa học
      </h3>
      <p className="font-sans text-gray-600 mb-6 max-w-sm">
        Thử chọn danh mục khác hoặc xóa bộ lọc để xem lại tất cả các khóa học CNTT của chúng tôi.
      </p>
      <Button variant="primary" onClick={onReset}>
        Xem tất cả khóa học
      </Button>
    </div>
  );
}

EmptyState.propTypes = {
  onReset: PropTypes.func.isRequired,
  className: PropTypes.string,
};
