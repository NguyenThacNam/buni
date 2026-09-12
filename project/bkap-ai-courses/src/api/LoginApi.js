import { api } from "./Api";

const loginApi = (username, password) => {
  const loginData = {
    username: username,
    password: password,
  };
  // Gọi POST tới endpoint api/v1/login (đã có tiền tố trong api.js)
  return api("POST", "login", loginData);
};

export { loginApi };

