import React from "react";
import PropTypes from "prop-types";

export default function SectionTitle({
  title,
  subtitle = "",
  align = "center",
  className = "",
}) {
  const isCenter = align === "center";

  return (
    <div className={`mb-12 ${isCenter ? "text-center" : "text-left"} ${className}`}>
      {subtitle && (
        <span className="text-primary font-heading text-sm font-semibold uppercase tracking-wider block mb-2">
          {subtitle}
        </span>
      )}
      <h2 className="font-heading text-2xl md:text-3xl font-bold text-gray-900 relative inline-block pb-3">
        {title}
        <span
          className={`absolute bottom-0 h-[3px] bg-primary rounded-full w-12 ${
            isCenter ? "left-1/2 -translate-x-1/2" : "left-0"
          }`}
        />
      </h2>
    </div>
  );
}

SectionTitle.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  align: PropTypes.oneOf(["center", "left"]),
  className: PropTypes.string,
};
