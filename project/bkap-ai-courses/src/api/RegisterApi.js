import { api } from "./Api";

export const registerApi = (fullName, username, email, phone, birthday, password) => {
  const parameters = {
    fullname: fullName,   // Họ và tên
    username: username,   // Tên đăng nhập
    email: email,
    phone: phone,
    birthday: birthday,   // Ngày sinh (format: YYYY-MM-DD)
    password: password,
  };

  // Gọi POST tới endpoint đăng ký
  return api("POST", "usersRegister", parameters);
};



