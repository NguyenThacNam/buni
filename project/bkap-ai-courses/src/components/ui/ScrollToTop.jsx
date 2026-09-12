import React, { useState, useEffect } from "react";
import { ChevronUp } from "lucide-react";

export default function ScrollToTop() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setVisible(window.scrollY > 300);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  if (!visible) return null;

  return (
    <button
      onClick={scrollToTop}
      aria-label="Cuộn về đầu trang"
      className="
        fixed bottom-8 right-8 z-50
        w-12 h-12
        bg-primary hover:bg-primary-dark
        text-white
        rounded-full
        shadow-btn hover:shadow-btn-hover
        flex items-center justify-center
        transition-all duration-300
        hover:-translate-y-1
      "
    >
      <ChevronUp className="w-6 h-6" />
    </button>
  );
}
