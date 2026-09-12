import React from "react";
import PropTypes from "prop-types";
import useCounter from "../../hooks/useCounter";

function StatCard({ value, suffix = "", label, inView = false }) {
  // Extract number from target for useCounter, then display with suffix
  const numVal = parseInt(value.replace(/[^0-9]/g, ""), 10) || 0;
  const animatedValue = useCounter(numVal, 2000, inView);

  // If there are non-digit chars in value (e.g., "+" or similar prefix), we can show them
  const hasPlus = value.includes("+");

  return (
    <div className="text-center p-4">
      <div 
        className="font-heading text-3xl md:text-4xl lg:text-5xl font-extrabold text-white mb-2 tracking-tight"
        aria-live="polite"
      >
        {animatedValue}
        {suffix}
        {hasPlus && !suffix.includes("+") && "+"}
      </div>
      <div className="font-sans text-xs md:text-sm lg:text-base text-red-100 font-medium uppercase tracking-wider">
        {label}
      </div>
    </div>
  );
}

StatCard.propTypes = {
  value: PropTypes.string.isRequired,
  suffix: PropTypes.string,
  label: PropTypes.string.isRequired,
  inView: PropTypes.bool,
};

const MemoizedStatCard = React.memo(StatCard);
export default MemoizedStatCard;
