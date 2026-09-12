module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#C8102E",
        "primary-dark": "#A50E26",
        navy: "#1A1A2E",
        accent: "#F59E0B",
        bg: "#F8F9FA",
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
        heading: ["Montserrat", "sans-serif"],
      },
      borderRadius: {
        card: "1rem",        // 16px – dùng cho tất cả card
        btn: "9999px",       // rounded-full – dùng cho tất cả button
      },
      boxShadow: {
        card: "0 4px 6px -1px rgba(0,0,0,0.1)",
        "card-hover": "0 20px 25px -5px rgba(0,0,0,0.15)",
        btn: "0 10px 15px -3px rgba(200,16,46,0.3)",
        "btn-hover": "0 20px 25px -5px rgba(200,16,46,0.4)",
      },
      transitionDuration: {
        DEFAULT: "300ms",
      },
      transitionTimingFunction: {
        DEFAULT: "cubic-bezier(0.4, 0, 0.2, 1)",
      },
    },
  },
  plugins: [require("@tailwindcss/forms")],
};
