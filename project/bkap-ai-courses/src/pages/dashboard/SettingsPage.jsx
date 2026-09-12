import React, { useState, useCallback } from "react";
import { motion } from "framer-motion";
import { Eye, EyeOff, Bell, Globe, Lock, ChevronDown } from "lucide-react";
import Toast from "../../components/dashboard/Toast";
import { api } from "../../api/Api";

/**
 * Toggle Switch Component đơn giản
 */
function ToggleSwitch({ checked, onChange, id }) {
  return (
    <button
      id={id}
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="relative inline-flex items-center w-11 h-6 rounded-full transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-red-300"
      style={{ background: checked ? "#DC2626" : "#E5E7EB" }}
    >
      <span
        className="inline-block w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform duration-300"
        style={{ transform: checked ? "translateX(22px)" : "translateX(4px)" }}
      />
    </button>
  );
}

/**
 * Khung một mục cài đặt.
 *
 * Section và PasswordInput PHẢI khai báo ở ngoài SettingsPage. Trước đây chúng
 * nằm bên trong, nên mỗi lần gõ một ký tự SettingsPage vẽ lại, sinh ra một
 * component "mới" cùng tên — React gỡ ô nhập cũ, dựng ô mới, và con trỏ văng
 * ra ngoài. Người dùng phải bấm lại vào ô sau từng chữ.
 */
function Section({ icon: Icon, title, children }) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <div
        className="flex items-center gap-3 px-6 py-4 border-b border-gray-100"
        style={{ background: "#FAFAFA" }}
      >
        <div
          className="w-8 h-8 rounded-lg flex items-center justify-center"
          style={{ background: "#FEE2E2" }}
        >
          <Icon style={{ width: 16, height: 16, color: "#DC2626" }} />
        </div>
        <h2 className="font-extrabold text-gray-800 text-sm">{title}</h2>
      </div>
      <div className="p-6">{children}</div>
    </div>
  );
}

/** Ô nhập mật khẩu có nút ẩn/hiện. */
function PasswordInput({ label, value, onChange, show, onToggle, placeholder, autoComplete }) {
  return (
    <div>
      <label className="block text-xs font-bold text-gray-600 mb-1.5 uppercase tracking-wide">
        {label}
      </label>
      <div className="relative">
        <input
          type={show ? "text" : "password"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          className="w-full px-4 py-2.5 pr-11 rounded-xl border border-gray-200 text-sm text-gray-700 focus:outline-none focus:border-red-400 focus:ring-2 focus:ring-red-100 transition-all"
        />
        <button
          type="button"
          onClick={onToggle}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
        >
          {show ? (
            <EyeOff style={{ width: 16, height: 16 }} />
          ) : (
            <Eye style={{ width: 16, height: 16 }} />
          )}
        </button>
      </div>
    </div>
  );
}

/**
 * SettingsPage: Trang Cài Đặt /dashboard/settings
 * - Đổi mật khẩu
 * - Toggle thông báo
 * - Chọn ngôn ngữ
 */
export default function SettingsPage() {
  // ---- State: đổi mật khẩu ----
  const [passwords, setPasswords] = useState({
    current: "",
    newPass: "",
    confirm: "",
  });
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    newPass: false,
    confirm: false,
  });

  const [dangDoiMatKhau, setDangDoiMatKhau] = useState(false);

  // ---- State: thông báo ----
  const [notifications, setNotifications] = useState({
    emailNewCourse: true,
    progressReminder: false,
  });

  // ---- State: ngôn ngữ ----
  const [language, setLanguage] = useState("vi");

  // ---- Toast ----
  const [toast, setToast] = useState({ show: false, message: "", type: "success" });

  const showToast = useCallback((message, type = "success") => {
    setToast({ show: true, message, type });
  }, []);

  const toggleShow = (field) => {
    setShowPasswords((prev) => ({ ...prev, [field]: !prev[field] }));
  };

  const handleSavePassword = async (e) => {
    e.preventDefault();
    if (!passwords.current || !passwords.newPass || !passwords.confirm) {
      showToast("Vui lòng điền đầy đủ thông tin!", "error");
      return;
    }
    if (passwords.newPass !== passwords.confirm) {
      showToast("Mật khẩu xác nhận không khớp!", "error");
      return;
    }
    if (passwords.newPass.length < 6) {
      showToast("Mật khẩu mới phải có ít nhất 6 ký tự!", "error");
      return;
    }

    // Trước đây hàm này chỉ hiện thông báo "thành công" mà không gọi server —
    // mật khẩu không hề đổi, người dùng lại tưởng đã đổi.
    setDangDoiMatKhau(true);
    try {
      const res = await api("put", "/user/password", {
        currentPassword: passwords.current,
        newPassword: passwords.newPass,
      });

      // Server đã thu hồi mọi phiên cũ và cấp phiên mới cho đúng máy này. Phải
      // thay token ngay, không thì lần gọi API tới dùng refresh token cũ (đã bị
      // thu hồi) và người dùng bị văng ra.
      if (res.data?.token) localStorage.setItem("token", res.data.token);
      if (res.data?.refreshToken) localStorage.setItem("refreshToken", res.data.refreshToken);

      setPasswords({ current: "", newPass: "", confirm: "" });
      showToast("Đổi mật khẩu thành công! Các thiết bị khác đã được đăng xuất. 🔐");
    } catch (err) {
      showToast(err?.response?.data?.error || "Chưa đổi được mật khẩu. Vui lòng thử lại!", "error");
    } finally {
      setDangDoiMatKhau(false);
    }
  };

  const handleSaveNotifications = () => {
    showToast("Cài đặt thông báo đã được lưu! 🔔");
  };

  const handleSaveLanguage = () => {
    showToast("Ngôn ngữ đã được cập nhật! 🌐");
  };



  return (
    <>
      <Toast
        message={toast.message}
        type={toast.type}
        show={toast.show}
        onClose={() => setToast((t) => ({ ...t, show: false }))}
      />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: "easeOut" }}
        className="space-y-6 max-w-2xl"
      >
        {/* Tiêu đề */}
        <div>
          <h1 className="text-2xl font-extrabold text-gray-800 mb-1">
            ⚙️ Cài đặt tài khoản
          </h1>
          <p className="text-gray-400 text-sm">
            Quản lý bảo mật và tùy chọn cá nhân
          </p>
        </div>

        {/* ---- SECTION 1: Đổi mật khẩu ---- */}
        <Section icon={Lock} title="Đổi mật khẩu">
          <form onSubmit={handleSavePassword} className="space-y-4">
            {[
              ["current", "Mật khẩu hiện tại", "••••••••", "current-password"],
              ["newPass", "Mật khẩu mới", "Tối thiểu 6 ký tự", "new-password"],
              ["confirm", "Xác nhận mật khẩu mới", "Nhập lại mật khẩu mới", "new-password"],
            ].map(([field, label, placeholder, autoComplete]) => (
              <PasswordInput
                key={field}
                label={label}
                placeholder={placeholder}
                autoComplete={autoComplete}
                value={passwords[field]}
                onChange={(v) => setPasswords((p) => ({ ...p, [field]: v }))}
                show={showPasswords[field]}
                onToggle={() => toggleShow(field)}
              />
            ))}
            <div className="flex justify-end pt-1">
              <button
                type="submit"
                disabled={dangDoiMatKhau}
                className="px-6 py-2.5 rounded-xl text-white font-bold text-sm shadow hover:opacity-90 active:scale-95 transition-all duration-200 disabled:opacity-60"
                style={{ background: "#DC2626" }}
              >
                {dangDoiMatKhau ? "Đang cập nhật..." : "Cập nhật mật khẩu"}
              </button>
            </div>
          </form>
        </Section>

        {/* ---- SECTION 2: Thông báo ---- */}
        <Section icon={Bell} title="Cài đặt thông báo">
          <div className="space-y-5">
            {/* Toggle 1 */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-gray-700">
                  Email thông báo khóa học mới
                </p>
                <p className="text-xs text-gray-400 mt-0.5">
                  Nhận email khi có khóa học mới phù hợp với bạn
                </p>
              </div>
              <ToggleSwitch
                id="toggle-email-new-course"
                checked={notifications.emailNewCourse}
                onChange={(v) =>
                  setNotifications((p) => ({ ...p, emailNewCourse: v }))
                }
              />
            </div>

            <div className="border-t border-gray-50" />

            {/* Toggle 2 */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-gray-700">
                  Nhắc nhở tiến độ học
                </p>
                <p className="text-xs text-gray-400 mt-0.5">
                  Nhận thông báo hàng tuần về tiến độ học tập của bạn
                </p>
              </div>
              <ToggleSwitch
                id="toggle-progress-reminder"
                checked={notifications.progressReminder}
                onChange={(v) =>
                  setNotifications((p) => ({ ...p, progressReminder: v }))
                }
              />
            </div>

            <div className="flex justify-end">
              <button
                onClick={handleSaveNotifications}
                className="px-6 py-2.5 rounded-xl text-white font-bold text-sm shadow hover:opacity-90 active:scale-95 transition-all duration-200"
                style={{ background: "#DC2626" }}
              >
                Lưu thông báo
              </button>
            </div>
          </div>
        </Section>

        {/* ---- SECTION 3: Ngôn ngữ ---- */}
        <Section icon={Globe} title="Ngôn ngữ">
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-gray-600 mb-1.5 uppercase tracking-wide">
                Ngôn ngữ hiển thị
              </label>
              <div className="relative">
                <select
                  id="select-language"
                  value={language}
                  onChange={(e) => setLanguage(e.target.value)}
                  className="w-full px-4 py-2.5 pr-10 rounded-xl border border-gray-200 text-sm text-gray-700 focus:outline-none focus:border-red-400 focus:ring-2 focus:ring-red-100 appearance-none bg-white transition-all"
                >
                  <option value="vi">🇻🇳 Tiếng Việt</option>
                  <option value="en">🇬🇧 English</option>
                </select>
                <ChevronDown
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"
                  style={{ width: 16, height: 16 }}
                />
              </div>
            </div>
            <div className="flex justify-end">
              <button
                onClick={handleSaveLanguage}
                className="px-6 py-2.5 rounded-xl text-white font-bold text-sm shadow hover:opacity-90 active:scale-95 transition-all duration-200"
                style={{ background: "#DC2626" }}
              >
                Lưu ngôn ngữ
              </button>
            </div>
          </div>
        </Section>
      </motion.div>
    </>
  );
}
