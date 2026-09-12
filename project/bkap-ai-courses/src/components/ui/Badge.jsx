import React from "react";
import PropTypes from "prop-types";

export default function Badge({ text, color = "#C8102E", className = "" }) {
  return (
    <span
      className={`inline-block rounded-full px-3 py-1 text-xs font-semibold text-white uppercase tracking-wide transition-all duration-300 ${className}`}
      style={{ backgroundColor: color }}
    >
      {text}
    </span>
  );
}

Badge.propTypes = {
  text: PropTypes.string.isRequired,
  color: PropTypes.string,
  className: PropTypes.string,
};
