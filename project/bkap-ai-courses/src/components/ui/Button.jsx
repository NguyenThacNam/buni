import React from "react";
import PropTypes from "prop-types";

export default function Button({
  children,
  type = "button",
  variant = "primary",
  className = "",
  disabled = false,
  isLoading = false,
  onClick = undefined,
  ...props
}) {
  let variantClasses = "";

  switch (variant) {
    case "primary":
      variantClasses =
        "bg-primary text-white hover:bg-primary-dark shadow-btn hover:shadow-btn-hover focus:ring-primary";
      break;
    case "secondary":
      variantClasses =
        "border-2 border-white text-white hover:bg-white hover:text-navy focus:ring-white";
      break;
    case "accent":
      variantClasses =
        "bg-accent text-navy hover:bg-yellow-500 shadow-md focus:ring-accent";
      break;
    case "text":
      variantClasses =
        "text-primary hover:text-primary-dark bg-transparent focus:ring-primary";
      break;
    default:
      variantClasses =
        "bg-primary text-white hover:bg-primary-dark focus:ring-primary";
  }

  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center font-heading font-bold rounded-btn px-8 py-4 transition-all duration-300 ease-in-out focus:ring-2 focus:ring-offset-2 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed ${variantClasses} ${className}`}
      disabled={disabled || isLoading}
      onClick={onClick}
      {...props}
    >
      {isLoading && (
        <svg
          className="animate-spin -ml-1 mr-3 h-5 w-5 text-current"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      )}
      {children}
    </button>
  );
}

Button.propTypes = {
  children: PropTypes.node.isRequired,
  type: PropTypes.string,
  variant: PropTypes.oneOf(["primary", "secondary", "accent", "text"]),
  className: PropTypes.string,
  disabled: PropTypes.bool,
  isLoading: PropTypes.bool,
  onClick: PropTypes.func,
};
