import React from "react";
import PropTypes from "prop-types";
import { Quote } from "lucide-react";
import LazyImage from "./LazyImage";

function TestimonialCard({ name, role, course, content, rating, avatar }) {
  const stars = Array.from({ length: 5 }, (_, i) => i);

  return (
    <div className="bg-white p-8 rounded-card border border-gray-100 shadow-card relative flex flex-col h-full select-none text-left">
      {/* Huge quote mark background */}
      <div 
        className="absolute right-6 top-6 text-primary opacity-10 pointer-events-none select-none" 
        style={{ fontSize: "5rem" }}
      >
        <Quote className="w-16 h-16" strokeWidth={1.5} aria-hidden="true" />
      </div>

      {/* Stars Rating */}
      <div className="flex items-center space-x-1 mb-4" aria-label={`Đánh giá ${rating} sao`}>
        {stars.map((starIndex) => (
          <span
            key={starIndex}
            className={`text-lg ${starIndex < rating ? "text-accent" : "text-gray-200"}`}
            aria-hidden="true"
          >
            ★
          </span>
        ))}
      </div>

      {/* Testimonial Quote */}
      <blockquote className="font-sans text-gray-600 mb-6 leading-relaxed flex-grow italic">
        &ldquo;{content}&rdquo;
      </blockquote>

      {/* Alumni Details */}
      <div className="flex items-center space-x-4 mt-auto">
        <LazyImage
          src={avatar}
          alt={`Chân dung ${name}`}
          className="w-12 h-12 rounded-full border-2 border-primary"
        />
        <div>
          <cite className="font-heading font-bold text-gray-900 text-sm md:text-base not-italic block">
            {name}
          </cite>
          <span className="font-sans text-xs text-gray-500 font-medium block">
            {role}
          </span>
        </div>
      </div>
    </div>
  );
}

TestimonialCard.propTypes = {
  name: PropTypes.string.isRequired,
  role: PropTypes.string.isRequired,
  course: PropTypes.string,
  content: PropTypes.string.isRequired,
  rating: PropTypes.number.isRequired,
  avatar: PropTypes.string.isRequired,
};

const MemoizedTestimonialCard = React.memo(TestimonialCard);
export default MemoizedTestimonialCard;
