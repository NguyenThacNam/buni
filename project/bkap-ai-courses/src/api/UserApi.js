import { api } from "./Api";

export const getUserProfileApi = () => {
  return api("GET", "user/profile"); // endpoint nối tiếp với baseURL tạo thành: user/profile
};
