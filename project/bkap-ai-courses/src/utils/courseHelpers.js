// Format giá tiền VND
export const formatPrice = (price) => {
  if (!price || price === 0) return "Miễn phí";
  return new Intl.NumberFormat("vi-VN").format(price) + "đ";
};

// Label level tiếng Việt
export const levelLabel = {
  BEGINNER: "Cơ bản",
  INTERMEDIATE: "Trung cấp",
  ADVANCED: "Nâng cao",
};

// Label role tiếng Việt
export const roleLabel = {
  CUSTOMER: "Học viên",
  ADMIN: "Quản trị viên",
};

// Lấy 2 chữ cái đầu từ họ tên để làm avatar
export const getInitials = (name) => {
  if (!name) return "U";
  const parts = name.trim().split(" ").filter(Boolean);
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};

// Chuyển YouTube URL sang embed URL
// Hỗ trợ: youtube.com/watch?v=ID, youtu.be/ID, youtube.com/embed/ID
export const getYoutubeEmbedUrl = (url) => {
  if (!url) return null;
  if (url.includes("/embed/")) return url; // đã là embed rồi
  const match = url.match(/(?:v=|youtu\.be\/)([^&\s?]+)/);
  return match ? `https://www.youtube.com/embed/${match[1]}` : url;
};

// Badge color theo category slug
export const getCategoryColor = (slug) => {
  const map = {
    frontend: "bg-blue-100 text-blue-700",
    backend: "bg-green-100 text-green-700",
    "full-stack": "bg-purple-100 text-purple-700",
    design: "bg-pink-100 text-pink-700",
    devops: "bg-orange-100 text-orange-700",
    mobile: "bg-cyan-100 text-cyan-700",
  };
  return map[slug] || "bg-gray-100 text-gray-700";
};

// Badge color theo level
export const getLevelColor = (level) => {
  const map = {
    BEGINNER: "bg-green-100 text-green-700",
    INTERMEDIATE: "bg-yellow-100 text-yellow-700",
    ADVANCED: "bg-red-100 text-red-700",
  };
  return map[level] || "bg-gray-100 text-gray-600";
};
