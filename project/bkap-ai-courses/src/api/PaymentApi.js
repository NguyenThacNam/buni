import { api } from "./Api";

// Tạo đơn hàng thanh toán
export const createOrderApi = (data) =>
  api("POST", "payment/create-order", data);

// Kiểm tra mã voucher
export const checkVoucherApi = (code) => api("GET", `payment/voucher/${code}`);

// Kiểm tra trạng thái giao dịch
export const checkPaymentStatusApi = (orderId) =>
  api("GET", `payment/status/${orderId}`);
