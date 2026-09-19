import { api } from "./Api";

// Lấy toàn bộ danh sách khóa học
export const getAllCoursesApi = () => api("GET", "courses");

// Lấy chi tiết 1 khóa học theo ID
export const getCourseByIdApi = (id) => api("GET", `courses/${id}`);

// Chương trình học (tên chương, tên bài) cho trang giới thiệu khóa.
export const getCourseOutlineApi = (id) => api("GET", `courses/${id}/outline`);

// Lấy khóa học theo danh mục (slug)
export const getCoursesByCategoryApi = (slug) =>
  api("GET", `courses/category/${slug}`);

// Lấy danh sách danh mục
export const getAllCategoriesApi = () => api("GET", "categories");
