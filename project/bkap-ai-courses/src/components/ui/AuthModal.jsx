import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  X,
  Lock,
  Eye,
  EyeOff,
  AlertCircle,
  CheckCircle,
  AtSign,
  Phone,
} from "lucide-react";
// Mail, User, Calendar chỉ form đăng ký dùng — form đó đã tắt.
import { useAuth } from "../../context/AuthContext";
import { CONTACT_INFO } from "../../data/constants";

function InputField({
  label,
  id,
  type = "text",
  value,
  onChange,
  placeholder,
  icon: Icon,
  error,
  maxLength,
}) {
  const [showPass, setShowPass] = useState(false);
  const isPassword = type === "password";
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold text-gray-700">
        {label}
      </label>
      <div className="relative">
        {Icon && (
          <Icon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        )}
        <input
          id={id}
          type={isPassword && showPass ? "text" : type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          maxLength={maxLength}
          className={`w-full ${Icon ? "pl-10" : "pl-4"} ${isPassword ? "pr-10" : "pr-4"} py-3 border rounded-xl text-sm font-medium outline-none transition-all duration-200
            ${
              error
                ? "border-red-400 bg-red-50 focus:border-red-500 focus:ring-2 focus:ring-red-100"
                : "border-gray-200 bg-gray-50 focus:border-primary focus:ring-2 focus:ring-primary/10 hover:border-gray-300"
            }`}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPass((s) => !s)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            tabIndex={-1}
          >
            {showPass ? (
              <EyeOff className="w-4 h-4" />
            ) : (
              <Eye className="w-4 h-4" />
            )}
          </button>
        )}
      </div>
      {error && (
        <p className="text-xs text-red-500 flex items-center gap-1">
          <AlertCircle className="w-3 h-3 flex-shrink-0" /> {error}
        </p>
      )}
    </div>
  );
}

// ─── LOGIN FORM (dùng Username + mật khẩu) ────────────────────────────────────
function LoginForm() {
  const { login } = useAuth();
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [form, setForm] = useState({
    username: "",
    password: "",
  });

  const validate = () => {
    const errs = {};
    if (!form.username.trim()) errs.username = "Vui lòng nhập tên đăng nhập.";
    else if (form.username.trim().length < 3)
      errs.username = "Tên đăng nhập tối thiểu 3 ký tự.";
    if (!form.password) errs.password = "Vui lòng nhập mật khẩu.";
    else if (form.password.length < 6)
      errs.password = "Mật khẩu tối thiểu 6 ký tự.";
    return errs;
  };

  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: "" }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }
    setLoading(true);

    const result = await login(form.username, form.password);

    if (result.success) {
      setSuccess(true);
    } else {
      setLoading(false);
      setErrors({ username: result.message });
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5">
      <div className="text-center mb-1">
        <h2 className="font-heading text-2xl font-extrabold text-gray-900 mb-1">
          Chào mừng trở lại!
        </h2>
        <p className="text-sm text-gray-500">Đăng nhập để tiếp tục học tập</p>
      </div>

      <InputField
        label="Tên đăng nhập"
        id="login-username"
        type="text"
        value={form.username}
        onChange={(e) => handleChange("username", e.target.value)}
        placeholder="Nhập tên đăng nhập của bạn"
        icon={AtSign}
        error={errors.username}
      />

      <InputField
        label="Mật khẩu"
        id="login-password"
        type="password"
        value={form.password}
        onChange={(e) => handleChange("password", e.target.value)}
        placeholder="Nhập mật khẩu"
        icon={Lock}
        error={errors.password}
      />

      <div className="flex items-center justify-between text-sm">
        <label className="flex items-center gap-2 cursor-pointer text-gray-600">
          <input type="checkbox" className="accent-primary rounded" />
          Ghi nhớ đăng nhập
        </label>
        <button
          type="button"
          className="text-primary font-semibold hover:underline"
        >
          Quên mật khẩu?
        </button>
      </div>

      <button
        type="submit"
        disabled={loading || success}
        className="w-full py-3 rounded-xl font-heading font-bold text-white text-sm transition-all duration-300 flex items-center justify-center gap-2"
        style={{
          background: success
            ? "#16a34a"
            : "linear-gradient(135deg, #C8102E, #a00d24)",
        }}
      >
        {loading && !success && (
          <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
        )}
        {success && <CheckCircle className="w-4 h-4" />}
        {success
          ? "Đăng nhập thành công!"
          : loading
            ? "Đang xử lý..."
            : "Đăng nhập"}
      </button>

      {/* Tài khoản do trung tâm cấp, không tự đăng ký. Chỉ đường cho người
          chưa có tài khoản thay vì để họ bí. */}
      <p className="text-center text-sm text-gray-500">
        Chưa có tài khoản? Liên hệ trung tâm để được cấp:{" "}
        <a
          href={`tel:${CONTACT_INFO.phone.replace(/\./g, "")}`}
          className="inline-flex items-center gap-1 text-primary font-bold hover:underline"
        >
          <Phone className="w-3.5 h-3.5" />
          {CONTACT_INFO.phone}
        </a>
      </p>
    </form>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// FORM ĐĂNG KÝ ĐÃ TẮT (11/09/2026)
//
// Trung tâm chuyển sang cấp tài khoản cho học viên ở trang quản trị
// (/admin/user), người ngoài không tự tạo tài khoản nữa. Endpoint
// /api/v1/usersRegister phía backend cũng đã tắt, nên dù form này còn thì
// bấm gửi cũng bị từ chối.
// ═══════════════════════════════════════════════════════════════════════════
// // ─── REGISTER FORM (có Username & Birthday) ───────────────────────────────────
// function RegisterForm({ onSwitch }) {
//   const { register } = useAuth();

//   const [form, setForm] = useState({
//     name: "",
//     username: "",
//     email: "",
//     phone: "",
//     birthday: "",
//     password: "",
//     confirm: "",
//   });
//   const [errors, setErrors] = useState({});
//   const [loading, setLoading] = useState(false);
//   const [success, setSuccess] = useState(false);

//   const validate = () => {
//     const errs = {};
//     if (!form.name.trim()) errs.name = "Vui lòng nhập họ tên.";
//     if (!form.username.trim()) errs.username = "Vui lòng nhập tên đăng nhập.";
//     else if (form.username.trim().length < 3)
//       errs.username = "Tên đăng nhập tối thiểu 3 ký tự.";
//     else if (!/^[a-zA-Z0-9_]+$/.test(form.username))
//       errs.username = "Chỉ dùng chữ, số và dấu gạch dưới (_).";
//     if (!form.email.trim()) errs.email = "Vui lòng nhập email.";
//     else if (!/\S+@\S+\.\S+/.test(form.email))
//       errs.email = "Email không hợp lệ.";
//     if (!form.phone.trim()) errs.phone = "Vui lòng nhập số điện thoại.";
//     else if (!/^(0|\+84)[0-9]{9}$/.test(form.phone.replace(/\s/g, "")))
//       errs.phone = "Số điện thoại không hợp lệ.";
//     if (!form.birthday) errs.birthday = "Vui lòng chọn ngày sinh.";
//     else {
//       const birthYear = new Date(form.birthday).getFullYear();
//       const currentYear = new Date().getFullYear();
//       if (currentYear - birthYear < 10)
//         errs.birthday = "Bạn phải ít nhất 10 tuổi.";
//     }
//     if (!form.password) errs.password = "Vui lòng nhập mật khẩu.";
//     else if (form.password.length < 6)
//       errs.password = "Mật khẩu tối thiểu 6 ký tự.";
//     if (form.confirm !== form.password)
//       errs.confirm = "Mật khẩu xác nhận không khớp.";
//     return errs;
//   };

//   const handleSubmit = async (e) => {
//     e.preventDefault();
//     const errs = validate();
//     if (Object.keys(errs).length) {
//       setErrors(errs);
//       return;
//     }

//     setLoading(true);

//     const result = await register(
//       form.name,
//       form.username,
//       form.email,
//       form.phone,
//       form.birthday,
//       form.password,
//     );

//     if (result.success) {
//       setSuccess(true);
//       setLoading(false);
//       setTimeout(() => {
//         onSwitch();
//       }, 1500);
//     } else {
//       setLoading(false);
//       setErrors({ username: result.message });
//     }
//   };

//   const field = (key) => ({
//     value: form[key],
//     onChange: (e) => {
//       setForm((f) => ({ ...f, [key]: e.target.value }));
//       setErrors((er) => ({ ...er, [key]: "" }));
//     },
//     error: errors[key],
//   });

//   return (
//     <form onSubmit={handleSubmit} className="flex flex-col gap-4">
//       <div className="text-center mb-1">
//         <h2 className="font-heading text-2xl font-extrabold text-gray-900 mb-1">
//           Tạo tài khoản
//         </h2>
//         <p className="text-sm text-gray-500">
//           Tham gia cộng đồng học viên BACHKHOA EDUCATION
//         </p>
//       </div>

//       <InputField
//         label="Họ và tên"
//         id="reg-name"
//         type="text"
//         placeholder="Nguyễn Văn A"
//         icon={User}
//         {...field("name")}
//       />
//       <InputField
//         label="Tên đăng nhập"
//         id="reg-username"
//         type="text"
//         placeholder="VD: nguyen_van_a"
//         icon={AtSign}
//         {...field("username")}
//       />
//       <InputField
//         label="Email"
//         id="reg-email"
//         type="email"
//         placeholder="you@example.com"
//         icon={Mail}
//         {...field("email")}
//       />
//       <InputField
//         label="Số điện thoại"
//         id="reg-phone"
//         type="tel"
//         placeholder="0912 345 678"
//         icon={Phone}
//         maxLength={11}
//         {...field("phone")}
//       />
//       <InputField
//         label="Ngày sinh"
//         id="reg-birthday"
//         type="date"
//         placeholder=""
//         icon={Calendar}
//         {...field("birthday")}
//       />
//       <InputField
//         label="Mật khẩu"
//         id="reg-password"
//         type="password"
//         placeholder="Tối thiểu 6 ký tự"
//         icon={Lock}
//         {...field("password")}
//       />
//       <InputField
//         label="Xác nhận mật khẩu"
//         id="reg-confirm"
//         type="password"
//         placeholder="Nhập lại mật khẩu"
//         icon={Lock}
//         {...field("confirm")}
//       />

//       {/* Ghi chú nhỏ */}
//       <p className="text-xs text-gray-400 flex items-center gap-1.5 -mt-1">
//         <AtSign className="w-3 h-3 flex-shrink-0" />
//         Tên đăng nhập sẽ được dùng để đăng nhập vào hệ thống
//       </p>

//       <button
//         type="submit"
//         disabled={loading || success}
//         className="w-full py-3 rounded-xl font-heading font-bold text-white text-sm transition-all duration-300 flex items-center justify-center gap-2"
//         style={{
//           background: success
//             ? "#16a34a"
//             : "linear-gradient(135deg, #C8102E, #a00d24)",
//         }}
//       >
//         {loading && (
//           <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
//         )}
//         {success && <CheckCircle className="w-4 h-4" />}
//         {success
//           ? "Đăng ký thành công!"
//           : loading
//             ? "Đang xử lý..."
//             : "Đăng ký"}
//       </button>

//       <p className="text-center text-sm text-gray-500">
//         Đã có tài khoản?{" "}
//         <button
//           type="button"
//           onClick={onSwitch}
//           className="text-primary font-bold hover:underline"
//         >
//           Đăng nhập
//         </button>
//       </p>
//     </form>
//   );
// }

// ─── MAIN MODAL WRAPPER ───────────────────────────────────────────────────────
export default function AuthModal() {
  const { authModal, closeModal } = useAuth();

  useEffect(() => {
    if (authModal) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [authModal]);

  return (
    <AnimatePresence>
      {authModal && (
        <motion.div
          className="fixed inset-0 z-[9999] flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          {/* Backdrop */}
          <motion.div
            className="absolute inset-0 bg-black/50 backdrop-blur-sm"
            onClick={closeModal}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          />

          {/* Modal Card */}
          <motion.div
            className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden max-h-[95vh] flex flex-col"
            initial={{ opacity: 0, scale: 0.92, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
          >
            {/* Decorative top bar */}
            <div className="h-1.5 w-full bg-gradient-to-r from-primary via-red-400 to-accent flex-shrink-0" />

            {/* Close button */}
            <button
              onClick={closeModal}
              className="absolute top-4 right-4 w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 flex items-center justify-center text-gray-500 hover:text-gray-800 transition-colors z-10"
              aria-label="Đóng"
            >
              <X className="w-4 h-4" />
            </button>

            {/* Logo mini */}
            <div className="px-8 pt-7 pb-0 flex items-center gap-2 flex-shrink-0">
              <span className="font-heading text-lg font-extrabold text-primary">
                BACHKHOA EDUCATION
              </span>
              <span className="h-4 w-[2px] bg-gray-200" />
              <span className="text-xs text-gray-400 uppercase tracking-wider font-semibold">
                Aptech
              </span>
            </div>

            <div className="p-8 pt-5 overflow-y-auto">
              <AnimatePresence mode="wait">
                <motion.div
                  key="login"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  transition={{ duration: 0.25 }}
                >
                  <LoginForm />
                </motion.div>
              </AnimatePresence>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
