import React from "react";
import PropTypes from "prop-types";

export default function Skeleton({ className = "" }) {
  return (
    <div
      className={`bg-white rounded-card shadow-card border border-gray-100 overflow-hidden flex flex-col h-full border-t-4 border-gray-200 animate-pulse ${className}`}
    >
      <div className="p-6 flex-grow">
        {/* Category Badge Placeholder */}
        <div className="h-5 bg-gray-200 rounded-full w-24 mb-4" />

        {/* Course Title Placeholder */}
        <div className="h-6 bg-gray-200 rounded-md w-3/4 mb-3" />
        <div className="h-6 bg-gray-200 rounded-md w-1/2 mb-4" />

        {/* Description Placeholders */}
        <div className="space-y-2 mb-6">
          <div className="h-4 bg-gray-200 rounded-md w-full" />
          <div className="h-4 bg-gray-200 rounded-md w-5/6" />
        </div>

        {/* Icon Stats Placeholders */}
        <div className="flex items-center space-x-4 border-t border-gray-100 pt-4 mb-4">
          <div className="flex items-center space-x-2 w-1/3">
            <div className="h-4 w-4 bg-gray-200 rounded-full" />
            <div className="h-4 bg-gray-200 rounded-md w-12" />
          </div>
          <div className="flex items-center space-x-2 w-1/3">
            <div className="h-4 w-4 bg-gray-200 rounded-full" />
            <div className="h-4 bg-gray-200 rounded-md w-16" />
          </div>
        </div>
      </div>

      {/* Button Placeholder */}
      <div className="p-6 pt-0 mt-auto">
        <div className="h-10 bg-gray-200 rounded-btn w-full" />
      </div>
    </div>
  );
}

Skeleton.propTypes = {
  className: PropTypes.string,
};
