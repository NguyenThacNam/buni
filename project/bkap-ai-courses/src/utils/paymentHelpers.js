export const formatPrice = (price) =>
  new Intl.NumberFormat("vi-VN").format(price) + "đ";

export const PAYMENT_METHODS = [
  {
    id: "vnpay",
    name: "VNPay",
    desc: "Thanh toán qua cổng VNPay (ATM, QR Code)",
    icon: "🏦",
    color: "#005BAA",
  },
  {
    id: "momo",
    name: "MoMo",
    desc: "Ví điện tử MoMo",
    icon: "💜",
    color: "#A50064",
  },
  {
    id: "zalopay",
    name: "ZaloPay",
    desc: "Ví ZaloPay / Chuyển khoản",
    icon: "💙",
    color: "#0068FF",
  },
];
