// Form validation helpers for registration and contact forms

/**
 * Validates a full name.
 * Must be non-empty and at least 2 characters long.
 */
export const validateName = (name) => {
  if (!name || name.trim().length < 2) {
    return "Họ tên phải có ít nhất 2 ký tự.";
  }
  return "";
};

/**
 * Validates an email address using regex.
 */
export const validateEmail = (email) => {
  if (!email || !email.trim()) {
    return "Email không được để trống.";
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return "Địa chỉ email không hợp lệ.";
  }
  return "";
};

/**
 * Validates a Vietnamese phone number.
 * Must contain only digits and be between 10 and 11 digits long.
 */
export const validatePhone = (phone) => {
  if (!phone || !phone.trim()) {
    return "Số điện thoại không được để trống.";
  }
  const cleanPhone = phone.trim();
  const digitRegex = /^[0-9]+$/;
  if (!digitRegex.test(cleanPhone)) {
    return "Số điện thoại chỉ được chứa các chữ số.";
  }
  if (cleanPhone.length < 10 || cleanPhone.length > 11) {
    return "Số điện thoại phải từ 10 đến 11 chữ số.";
  }
  return "";
};

/**
 * Validates a message textarea.
 * Must be non-empty and at least 10 characters long.
 */
export const validateMessage = (message) => {
  if (!message || message.trim().length < 10) {
    return "Nội dung tin nhắn phải từ 10 ký tự trở lên.";
  }
  return "";
};

// src/utils/validators.js – tạo mới nếu chưa có, hoặc thêm hàm validateForm

export function validateForm(form) {
  const errors = {};

  if (!form.name || form.name.trim().length < 2) {
    errors.name = "Họ tên phải có ít nhất 2 ký tự";
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!form.email || !emailRegex.test(form.email)) {
    errors.email = "Email không hợp lệ";
  }

  const phoneRegex = /^[0-9]{10,11}$/;
  if (!form.phone || !phoneRegex.test(form.phone.replace(/\./g, ""))) {
    errors.phone = "Số điện thoại phải có 10-11 chữ số";
  }

  if (form.message !== undefined && form.message.trim().length < 10) {
    errors.message = "Tin nhắn phải có ít nhất 10 ký tự";
  }

  return errors;
}
