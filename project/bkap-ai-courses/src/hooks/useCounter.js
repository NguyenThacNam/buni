import { useState, useEffect } from "react";

/**
 * Animate numbers from 0 to a target value.
 * Animates only when inView is true.
 */
export default function useCounter(target, duration = 1500, inView = false) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (!inView) {
      return;
    }

    let start = 0;
    const end = parseInt(target, 10);
    if (isNaN(end) || end <= 0) {
      setCount(target);
      return;
    }

    const frameRate = 1000 / 60; // 60 FPS
    const totalFrames = Math.round(duration / frameRate);
    let frame = 0;

    const timer = setInterval(() => {
      frame++;
      const progress = frame / totalFrames;
      // Easing out quadratic function
      const easeProgress = progress * (2 - progress);
      const current = Math.round(easeProgress * end);

      if (frame >= totalFrames) {
        clearInterval(timer);
        setCount(end);
      } else {
        setCount(current);
      }
    }, frameRate);

    return () => {
      clearInterval(timer);
    };
  }, [target, duration, inView]);

  return count;
}
