import React, { useState, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Pencil, Check, X, CheckCircle, Shield } from "lucide-react";

// Import hàm api chung của bạn
import { api } from "../../api/Api";
import { useAuth } from "../../context/AuthContext";

// ─────────────────────────────────────────
// HELPER: Lấy 2 chữ cái đầu từ tên đầy đủ
// ─────────────────────────────────────────
function getInitials(name = "") {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return "?";
  if (words.length === 1) return words[0][0].toUpperCase();
  return (
    words[words.length - 2][0] + words[words.length - 1][0]
  ).toUpperCase();
}

// ─────────────────────────────────────────
// HELPER: Format birthday → "yyyy-MM-dd" cho input[type=date]
// ─────────────────────────────────────────
function formatBirthday(birthday) {
  if (!birthday) return "";
  const d = new Date(birthday);
  if (isNaN(d.getTime())) return birthday; // Nếu đã là chuỗi dạng yyyy-MM-dd thì giữ nguyên
  return d.toISOString().split("T")[0];
}

// ─────────────────────────────────────────
// HELPER: Format birthday để HIỂN THỊ (dd/mm/yyyy)
// ─────────────────────────────────────────
function displayBirthday(birthday) {
  const iso = formatBirthday(birthday);
  if (!iso || !iso.includes("-")) return birthday || "";
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}

// ─────────────────────────────────────────
// HELPER: Ánh xạ role → nhãn tiếng Việt
// ─────────────────────────────────────────
function getRoleLabel(role) {
  if (role === "ADMIN") return "Quản trị viên";
  return "Học viên";
}

// ─────────────────────────────────────────
// COMPONENT: Toast Notification
// ─────────────────────────────────────────
function Toast({ show, onClose, message = "Cập nhật thành công! 🎉" }) {
  useEffect(() => {
    if (!show) return;
    const t = setTimeout(onClose, 3000);
    return () => clearTimeout(t);
  }, [show, onClose]);

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={{ opacity: 0, x: 80 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 80 }}
          transition={{ duration: 0.3, ease: "easeOut" }}
          style={{
            position: "fixed",
            top: 20,
            right: 20,
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            gap: 10,
            padding: "12px 18px",
            borderRadius: 12,
            background: "#DCFCE7",
            border: "1px solid #86EFAC",
            color: "#166534",
            fontWeight: 600,
            fontSize: 14,
            boxShadow: "0 8px 32px rgba(0,0,0,0.12)",
            minWidth: 260,
          }}
        >
          <CheckCircle style={{ width: 18, height: 18, flexShrink: 0 }} />
          <span>{message}</span>
          <button
            onClick={onClose}
            style={{
              marginLeft: "auto",
              background: "none",
              border: "none",
              cursor: "pointer",
              color: "#166534",
              opacity: 0.6,
              padding: 0,
              display: "flex",
            }}
          >
            <X style={{ width: 14, height: 14 }} />
          </button>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function FieldLabel({ children, sub }) {
  return (
    <div className="flex items-center gap-1.5 mb-1.5 flex-wrap">
      <label className="block text-gray-800 font-bold uppercase tracking-wider text-xs">
        {children}
      </label>
      {sub && (
        <span className="text-gray-500 font-medium normal-case text-xs">
          {sub}
        </span>
      )}
    </div>
  );
}

function ViewValue({ value }) {
  if (!value || value === "") {
    return (
      <p
        className="text-gray-500 italic text-base font-normal"
        style={{ minHeight: 24 }}
      >
        Chưa cập nhật
      </p>
    );
  }
  return (
    <p
      className="text-gray-900 text-base font-semibold"
      style={{ minHeight: 24 }}
    >
      {value}
    </p>
  );
}

// ─────────────────────────────────────────
// MAIN COMPONENT: ProfilePage
// ─────────────────────────────────────────
export default function ProfilePage() {
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const [originalData, setOriginalData] = useState({});
  const [showToast, setShowToast] = useState(false);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  // Hàm gọi API lấy thông tin Profile từ Back-end
  const fetchUserProfile = useCallback(() => {
    setLoading(true);
    api("GET", "user/profile")
      .then((response) => {
        console.log("RAW response:", response); // Xem toàn bộ
        console.log("response.data:", response?.data); // Xem có .data không
        // ✅ Handle cả response trực tiếp lẫn response.data
        const dataBE = response?.data ?? response ?? {};

        const initial = {
          fullname: dataBE.fullname || "",
          email: dataBE.email || "",
          username: dataBE.username || "",
          phone: dataBE.phone || "",
          birthday: dataBE.birthday || "",
          role: dataBE.role || "STUDENT",
          address: dataBE.address || "",
          bio: dataBE.bio || "",
          createDate: dataBE.createDate || "—",
          last_login: dataBE.last_login || "—",
          status: dataBE.status ?? true,
        };
        setFormData(initial);
        setOriginalData(initial);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Lỗi lấy thông tin cá nhân:", error);
        setLoading(false);
      });
  }, []);

  // ── Load data từ API khi mount component ──
  useEffect(() => {
    // ✅ Chỉ fetch khi đã có token trong localStorage
    const token = localStorage.getItem("token");
    if (!token) return; // Chờ đến khi có token

    fetchUserProfile();
  }, [fetchUserProfile, user]); // ← Thêm "user" vào dependency array

  // ── Tính % hoàn thiện hồ sơ ──
  const completionFields = [
    { label: "Thông tin cơ bản", check: () => !!formData.fullname },
    { label: "Số điện thoại", check: () => !!formData.phone },
    { label: "Ngày sinh", check: () => !!formData.birthday },
    { label: "Địa chỉ", check: () => !!formData.address },
    { label: "Giới thiệu bản thân", check: () => !!formData.bio },
  ];
  const completionPercent = Math.round(
    (completionFields.filter((f) => f.check()).length /
      completionFields.length) *
      100,
  );

  // ── Handlers ──
  const handleChange = useCallback((e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleStartEdit = () => {
    setOriginalData({ ...formData });
    setIsEditing(true);
  };

  const handleCancel = () => {
    setFormData({ ...originalData });
    setIsEditing(false);
  };

  const handleSave = () => {
    // Chuẩn bị dữ liệu gửi lên Back-end cập nhật hồ sơ
    const payload = {
      fullname: formData.fullname,
      phone: formData.phone,
      birthday: formData.birthday,
      address: formData.address,
      bio: formData.bio,
    };

    // Gọi API cập nhật thông tin (Thay đổi endpoint/method cho phù hợp với API update của bạn nếu cần)
    api("PUT", "user/profile", payload)
      .then((response) => {
        setOriginalData({ ...formData });
        setIsEditing(false);
        setShowToast(true);
      })
      .catch((error) => {
        console.error("Cập nhật hồ sơ thất bại:", error);
        alert("Có lỗi xảy ra khi lưu thay đổi!");
      });
  };

  const inputClass =
    "w-full px-3.5 py-2.5 rounded-lg border text-base text-gray-900 font-medium bg-white transition-all duration-200 focus:outline-none";
  const inputStyle = { borderColor: "#CBD5E1", borderRadius: 8 };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-500 font-medium">
        Đang tải thông tin hồ sơ...
      </div>
    );
  }

  return (
    <>
      <Toast show={showToast} onClose={() => setShowToast(false)} />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: "easeOut" }}
        className="space-y-6"
      >
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 mb-1">
            👤 Hồ sơ cá nhân
          </h1>
          <p className="text-gray-600 text-sm font-medium">
            Quản lý thông tin cá nhân của bạn
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          {/* CỘT TRÁI */}
          <div className="space-y-4">
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-200 flex flex-col items-center text-center">
              <div
                className="w-20 h-20 rounded-full flex items-center justify-center text-white text-2xl font-extrabold mb-3 shadow-md"
                style={{ background: "#DC2626" }}
              >
                {getInitials(formData.fullname)}
              </div>
              <h2 className="font-extrabold text-gray-900 text-xl leading-tight">
                {formData.fullname || "Chưa cập nhật"}
              </h2>
              <p className="text-sm text-gray-600 font-medium mt-1 truncate max-w-full">
                {formData.email}
              </p>
              <span
                className="mt-3 text-xs font-bold px-3 py-1 rounded-full"
                style={{ background: "#FEE2E2", color: "#DC2626" }}
              >
                {getRoleLabel(formData.role)}
              </span>
            </div>

            <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-200">
              <p className="text-base font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Shield style={{ width: 16, height: 16, color: "#DC2626" }} />
                Mức độ hoàn thiện hồ sơ
              </p>
              <div className="w-full h-2.5 bg-gray-200 rounded-full overflow-hidden mb-1.5">
                <motion.div
                  className="h-full rounded-full"
                  style={{ background: "#DC2626" }}
                  initial={{ width: 0 }}
                  animate={{ width: `${completionPercent}%` }}
                  transition={{ duration: 0.8, ease: "easeOut", delay: 0.3 }}
                />
              </div>
              <p className="text-xs font-bold text-gray-700 mb-4">
                {completionPercent}% hoàn thiện
              </p>
              <ul className="space-y-2.5">
                {completionFields.map((item) => {
                  const done = item.check();
                  return (
                    <li key={item.label} className="flex items-center gap-2.5">
                      <span
                        className="w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0"
                        style={{ background: done ? "#DCFCE7" : "#E2E8F0" }}
                      >
                        {done ? (
                          <Check
                            style={{ width: 12, height: 12, color: "#166534" }}
                          />
                        ) : (
                          <X
                            style={{ width: 12, height: 12, color: "#64748B" }}
                          />
                        )}
                      </span>
                      <span
                        className="text-xs font-bold"
                        style={{ color: done ? "#0F172A" : "#475569" }}
                      >
                        {item.label}
                      </span>
                    </li>
                  );
                })}
              </ul>
            </div>
          </div>

          {/* CỘT PHẢI */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
              <div
                className="flex items-center justify-between px-6 py-4 border-b border-gray-200"
                style={{ background: "#F8FAFC" }}
              >
                <div>
                  <h2 className="font-extrabold text-gray-900 text-base">
                    {isEditing ? "Chỉnh sửa thông tin" : "Thông tin cá nhân"}
                  </h2>
                  <p className="text-xs text-gray-600 font-medium mt-0.5">
                    {isEditing
                      ? 'Nhấn "Lưu thay đổi" để xác nhận'
                      : 'Nhấn "Chỉnh sửa" để cập nhật thông tin'}
                  </p>
                </div>

                <AnimatePresence mode="wait">
                  {!isEditing ? (
                    <motion.button
                      key="edit-btn"
                      initial={{ opacity: 0, scale: 0.9 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.9 }}
                      transition={{ duration: 0.2 }}
                      onClick={handleStartEdit}
                      className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold border-2 transition-all duration-200 hover:bg-red-50"
                      style={{
                        borderColor: "#DC2626",
                        color: "#DC2626",
                        background: "#fff",
                      }}
                    >
                      <Pencil style={{ width: 14, height: 14 }} />
                      Chỉnh sửa
                    </motion.button>
                  ) : (
                    <motion.div
                      key="save-cancel-btns"
                      initial={{ opacity: 0, scale: 0.9 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.9 }}
                      transition={{ duration: 0.2 }}
                      className="flex items-center gap-2"
                    >
                      <button
                        onClick={handleCancel}
                        className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-bold border-2 transition-all duration-200 hover:bg-gray-50"
                        style={{ borderColor: "#CBD5E1", color: "#475569" }}
                      >
                        <X style={{ width: 14, height: 14 }} />
                        Hủy
                      </button>
                      <button
                        onClick={handleSave}
                        className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-bold text-white transition-all duration-200 hover:opacity-90 active:scale-95 shadow-sm"
                        style={{ background: "#DC2626" }}
                      >
                        <Check style={{ width: 14, height: 14 }} />
                        Lưu thay đổi
                      </button>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              <div className="p-6 space-y-5">
                {/* HÀNG 1: Họ và tên | Email */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  <div>
                    <FieldLabel>Họ và tên</FieldLabel>
                    <AnimatePresence mode="wait">
                      {isEditing ? (
                        <motion.input
                          key="fullname-input"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          transition={{ duration: 0.15 }}
                          type="text"
                          name="fullname"
                          value={formData.fullname || ""}
                          onChange={handleChange}
                          placeholder="Nhập họ và tên"
                          className={inputClass}
                          style={inputStyle}
                          onFocus={(e) => {
                            e.target.style.borderColor = "#DC2626";
                            e.target.style.boxShadow =
                              "0 0 0 3px rgba(220,38,38,0.1)";
                          }}
                          onBlur={(e) => {
                            e.target.style.borderColor = "#CBD5E1";
                            e.target.style.boxShadow = "none";
                          }}
                        />
                      ) : (
                        <motion.div
                          key="fullname-view"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          transition={{ duration: 0.15 }}
                        >
                          <ViewValue value={formData.fullname} />
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>

                  <div>
                    <FieldLabel sub="(Không thể thay đổi)">Email</FieldLabel>
                    <p
                      className="text-gray-900 text-base font-semibold"
                      style={{ minHeight: 24 }}
                    >
                      {formData.email || "—"}
                    </p>
                  </div>
                </div>

                {/* HÀNG 2: Số điện thoại | Ngày sinh */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  <div>
                    <FieldLabel>Số điện thoại</FieldLabel>
                    <AnimatePresence mode="wait">
                      {isEditing ? (
                        <motion.input
                          key="phone-input"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          transition={{ duration: 0.15 }}
                          type="tel"
                          name="phone"
                          value={formData.phone || ""}
                          onChange={handleChange}
                          placeholder="0901234567"
                          className={inputClass}
                          style={inputStyle}
                          onFocus={(e) => {
                            e.target.style.borderColor = "#DC2626";
                            e.target.style.boxShadow =
                              "0 0 0 3px rgba(220,38,38,0.1)";
                          }}
                          onBlur={(e) => {
                            e.target.style.borderColor = "#CBD5E1";
                            e.target.style.boxShadow = "none";
                          }}
                        />
                      ) : (
                        <motion.div
                          key="phone-view"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          transition={{ duration: 0.15 }}
                        >
                          <ViewValue value={formData.phone} />
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>

                  <div>
                    <FieldLabel>Ngày sinh</FieldLabel>
                    <AnimatePresence mode="wait">
                      {isEditing ? (
                        <motion.input
                          key="birthday-input"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          transition={{ duration: 0.15 }}
                          type="date"
                          name="birthday"
                          value={formatBirthday(formData.birthday)}
                          onChange={handleChange}
                          className={inputClass}
                          style={inputStyle}
                          onFocus={(e) => {
                            e.target.style.borderColor = "#DC2626";
                            e.target.style.boxShadow =
                              "0 0 0 3px rgba(220,38,38,0.1)";
                          }}
                          onBlur={(e) => {
                            e.target.style.borderColor = "#CBD5E1";
                            e.target.style.boxShadow = "none";
                          }}
                        />
                      ) : (
                        <motion.div
                          key="birthday-view"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          transition={{ duration: 0.15 }}
                        >
                          <ViewValue
                            value={displayBirthday(formData.birthday)}
                          />
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </div>

                {/* HÀNG 3: Địa chỉ */}
                <div>
                  <FieldLabel>Địa chỉ</FieldLabel>
                  <AnimatePresence mode="wait">
                    {isEditing ? (
                      <motion.input
                        key="address-input"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.15 }}
                        type="text"
                        name="address"
                        value={formData.address || ""}
                        onChange={handleChange}
                        placeholder="Cầu Giấy, Hà Nội"
                        className={inputClass}
                        style={inputStyle}
                        onFocus={(e) => {
                          e.target.style.borderColor = "#DC2626";
                          e.target.style.boxShadow =
                            "0 0 0 3px rgba(220,38,38,0.1)";
                        }}
                        onBlur={(e) => {
                          e.target.style.borderColor = "#CBD5E1";
                          e.target.style.boxShadow = "none";
                        }}
                      />
                    ) : (
                      <motion.div
                        key="address-view"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.15 }}
                      >
                        <ViewValue value={formData.address} />
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>

                {/* HÀNG 4: Giới thiệu bản thân */}
                <div>
                  <FieldLabel>Giới thiệu bản thân</FieldLabel>
                  <AnimatePresence mode="wait">
                    {isEditing ? (
                      <motion.textarea
                        key="bio-input"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.15 }}
                        name="bio"
                        value={formData.bio || ""}
                        onChange={handleChange}
                        rows={4}
                        placeholder="Chia sẻ đôi điều về bản thân bạn..."
                        className={inputClass + " resize-none"}
                        style={inputStyle}
                        onFocus={(e) => {
                          e.target.style.borderColor = "#DC2626";
                          e.target.style.boxShadow =
                            "0 0 0 3px rgba(220,38,38,0.1)";
                        }}
                        onBlur={(e) => {
                          e.target.style.borderColor = "#CBD5E1";
                          e.target.style.boxShadow = "none";
                        }}
                      />
                    ) : (
                      <motion.div
                        key="bio-view"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.15 }}
                      >
                        <ViewValue value={formData.bio} />
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>

                {/* Thông tin hệ thống */}
                <div className="border-t border-gray-200 pt-4">
                  <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">
                    Thông tin hệ thống
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <FieldLabel sub="(Không thể thay đổi)">
                        Tên đăng nhập
                      </FieldLabel>
                      <p className="text-gray-900 text-base font-semibold">
                        {formData.username || "—"}
                      </p>
                    </div>
                    <div>
                      <FieldLabel>Vai trò</FieldLabel>
                      <span
                        className="inline-block text-xs font-bold px-3 py-1 rounded-full"
                        style={{ background: "#FEE2E2", color: "#DC2626" }}
                      >
                        {getRoleLabel(formData.role)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </>
  );
}
