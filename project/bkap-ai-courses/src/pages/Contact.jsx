import React, { useState, useEffect } from "react";
import { toast } from "react-toastify";
import { MapPin, Phone, Mail, Map } from "lucide-react";
import { CONTACT_INFO } from "../data/constants";
import { courses } from "../data/courses";
import Accordion from "../components/ui/Accordion";
import Button from "../components/ui/Button";
import SectionTitle from "../components/ui/SectionTitle";
import {
  validateName,
  validateEmail,
  validatePhone,
  validateMessage,
} from "../utils/validators";

export default function Contact() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    phone: "",
    course: "full-stack",
    message: "",
  });
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    document.title = "Liên hệ & Đăng ký tư vấn | BKAP AI";
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    if (errors[name]) {
      setErrors({ ...errors, [name]: "" });
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    const nameError = validateName(formData.name);
    const emailError = validateEmail(formData.email);
    const phoneError = validatePhone(formData.phone);
    const messageError = validateMessage(formData.message);

    const validationErrors = {};
    if (nameError) validationErrors.name = nameError;
    if (emailError) validationErrors.email = emailError;
    if (phoneError) validationErrors.phone = phoneError;
    if (messageError) validationErrors.message = messageError;

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setIsSubmitting(true);

    // Simulate API request (1.5 seconds)
    setTimeout(() => {
      setIsSubmitting(false);
      toast.success("Thông tin liên hệ đã gửi thành công! BKAP AI sẽ phản hồi bạn sớm nhất.");
      setFormData({
        name: "",
        email: "",
        phone: "",
        course: "full-stack",
        message: "",
      });
      setErrors({});
    }, 1500);
  };

  const faqItems = [
    {
      title: "Mô hình đào tạo 'Làm trước học sau' là gì?",
      content: (
        <p>
          Đây là mô hình đào tạo thực chiến tiên phong của Bachkhoa-Aptech. Học viên được tiếp cận các
          dự án thực tế trước để hiểu rõ nhu cầu của doanh nghiệp, sau đó mới đi sâu phân tích lý
          thuyết tương ứng. Phương pháp này giúp nâng cao 75% hiệu quả ghi nhớ và khả năng giải
          quyết vấn đề độc lập.
        </p>
      ),
    },
    {
      title: "Tôi chưa biết gì về lập trình, có thể tham gia học được không?",
      content: (
        <p>
          Hoàn toàn được. Lộ trình học của Bachkhoa-Aptech được xây dựng bài bản từ cơ bản đến nâng
          cao, phù hợp cho học sinh tốt nghiệp THPT, sinh viên chuyển ngành hoặc người mới bắt đầu
          từ con số 0. BKAP cũng có chính sách hỗ trợ học bù, học lại miễn phí để đảm bảo học viên
          theo kịp chương trình.
        </p>
      ),
    },
    {
      title: "Bằng cấp của Bachkhoa-Aptech có giá trị như thế nào?",
      content: (
        <p>
          Sau khi hoàn thành chương trình đào tạo, học viên sẽ nhận chứng chỉ quốc tế do tập đoàn
          Aptech Ấn Độ cấp. Chứng chỉ này có giá trị toàn cầu, được các doanh nghiệp phần mềm lớn
          trong và ngoài nước ưu tiên tuyển dụng.
        </p>
      ),
    },
    {
      title: "Hệ thống có hỗ trợ đóng học phí trả góp không?",
      content: (
        <p>
          Có. Bachkhoa-Aptech hỗ trợ học viên đóng học phí trả góp với lãi suất 0% liên kết thông qua
          các đối tác tài chính uy tín. Hỗ trợ này giúp giảm bớt gánh nặng kinh tế ban đầu, hỗ trợ các
          bạn học viên chuyên tâm học tập.
        </p>
      ),
    },
    {
      title: "Cam kết hỗ trợ việc làm của Bachkhoa-Aptech cụ thể ra sao?",
      content: (
        <p>
          Ngay khi nhập học, học viên sẽ ký cam kết hỗ trợ giới thiệu việc làm bằng văn bản pháp lý.
          BKAP sở hữu mạng lưới liên kết hơn 500 doanh nghiệp CNTT đối tác (FPT Software, Rikkeisoft,
          Luvina, v.v.), cam kết giới thiệu và sắp xếp phỏng vấn ngay sau khi tốt nghiệp.
        </p>
      ),
    },
  ];

  return (
    <main className="min-h-screen bg-bg">
      {/* Page Title */}
      <section className="bg-navy text-white py-16 relative" aria-label="Liên hệ và tư vấn">
        <div className="absolute inset-0 bg-dot-grid opacity-10 pointer-events-none" />
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 relative z-10 text-center">
          <h1 className="font-heading text-4xl font-extrabold mb-4">
            Liên Hệ & Đăng Ký Tư Vấn
          </h1>
          <p className="font-sans text-base text-gray-300 max-w-xl mx-auto leading-relaxed">
            Bạn có câu hỏi hoặc cần tư vấn lộ trình học tập CNTT? Hãy gửi tin nhắn cho chúng tôi để
            nhận phản hồi ngay lập tức.
          </p>
        </div>
      </section>

      {/* Grid Contact Details & Form */}
      <section className="py-20" aria-label="Địa chỉ và biểu mẫu">
        <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mb-20">
            {/* Contact Details & Map */}
            <div className="space-y-8">
              <div className="bg-white p-8 rounded-card border border-gray-100 shadow-card">
                <h2 className="font-heading text-xl font-bold text-gray-900 mb-6">
                  Thông Tin Liên Hệ
                </h2>

                <address className="space-y-6 font-sans text-sm text-gray-600 not-italic">
                  <div className="flex items-start space-x-4">
                    <div className="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center text-primary flex-shrink-0">
                      <MapPin className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="font-sans font-bold text-gray-900 mb-1">Trụ sở đào tạo</p>
                      <p>{CONTACT_INFO.address}</p>
                    </div>
                  </div>

                  <div className="flex items-start space-x-4">
                    <div className="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center text-primary flex-shrink-0">
                      <Phone className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="font-sans font-bold text-gray-900 mb-1">Hotline tuyển sinh</p>
                      <a
                        href={`tel:${CONTACT_INFO.phone.replace(/\./g, "")}`}
                        className="hover:text-primary transition-colors focus-ring rounded"
                      >
                        {CONTACT_INFO.phone}
                      </a>
                    </div>
                  </div>

                  <div className="flex items-start space-x-4">
                    <div className="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center text-primary flex-shrink-0">
                      <Mail className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="font-sans font-bold text-gray-900 mb-1">Email tiếp nhận</p>
                      <a
                        href={`mailto:${CONTACT_INFO.email}`}
                        className="hover:text-primary transition-colors focus-ring rounded"
                      >
                        {CONTACT_INFO.email}
                      </a>
                    </div>
                  </div>
                </address>
              </div>

              {/* Styled Map Placeholder Div */}
              <div 
                className="bg-white p-6 rounded-card border border-gray-100 shadow-card flex flex-col items-center justify-center text-center min-h-[300px] relative overflow-hidden"
                aria-label="Bản đồ chỉ đường"
              >
                <div className="absolute inset-0 bg-dot-grid opacity-10 pointer-events-none" />
                <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center text-primary mb-4">
                  <Map className="w-8 h-8" />
                </div>
                <h3 className="font-heading font-bold text-gray-900 text-lg mb-2">
                  Bản Đồ Chỉ Đường
                </h3>
                <p className="font-sans text-sm text-gray-500 mb-4 max-w-sm">
                  236 Hoàng Quốc Việt, Cổ Nhuế 1, Bắc Từ Liêm, Hà Nội (Tòa nhà HTC).
                </p>
                <a
                  href="https://maps.google.com/?q=236+Hoang+Quoc+Viet+Cau+Giay+Ha+Noi"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="bg-primary hover:bg-primary-dark text-white font-heading font-bold text-xs px-6 py-3 rounded-btn shadow-btn transition-all duration-300 focus-ring"
                >
                  Mở trên Google Maps
                </a>
              </div>
            </div>

            {/* Controlled Contact Form */}
            <div className="bg-white p-8 rounded-card border border-gray-100 shadow-card">
              <h2 className="font-heading text-xl font-bold text-gray-900 mb-6">
                Gửi Yêu Cầu Tư Vấn Lộ Trình
              </h2>

              <form onSubmit={handleSubmit} className="space-y-4">
                {/* Full name input */}
                <div>
                  <label htmlFor="name" className="block font-sans text-xs font-semibold text-gray-700 uppercase tracking-wide mb-1">
                    Họ và tên <span className="text-primary">*</span>
                  </label>
                  <input
                    id="name"
                    name="name"
                    type="text"
                    placeholder="Nguyễn Văn A"
                    value={formData.name}
                    onChange={handleInputChange}
                    className={`w-full rounded-xl border border-gray-200 p-3 font-sans text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent ${
                      errors.name ? "border-primary" : ""
                    }`}
                    aria-required="true"
                    aria-describedby={errors.name ? "name-error" : undefined}
                  />
                  {errors.name && (
                    <span id="name-error" className="block font-sans text-xs text-primary mt-1" role="alert">
                      {errors.name}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Email input */}
                  <div>
                    <label htmlFor="email" className="block font-sans text-xs font-semibold text-gray-700 uppercase tracking-wide mb-1">
                      Email <span className="text-primary">*</span>
                    </label>
                    <input
                      id="email"
                      name="email"
                      type="email"
                      placeholder="email@example.com"
                      value={formData.email}
                      onChange={handleInputChange}
                      className={`w-full rounded-xl border border-gray-200 p-3 font-sans text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent ${
                        errors.email ? "border-primary" : ""
                      }`}
                      aria-required="true"
                      aria-describedby={errors.email ? "email-error" : undefined}
                    />
                    {errors.email && (
                      <span id="email-error" className="block font-sans text-xs text-primary mt-1" role="alert">
                        {errors.email}
                      </span>
                    )}
                  </div>

                  {/* Phone input */}
                  <div>
                    <label htmlFor="phone" className="block font-sans text-xs font-semibold text-gray-700 uppercase tracking-wide mb-1">
                      Số điện thoại <span className="text-primary">*</span>
                    </label>
                    <input
                      id="phone"
                      name="phone"
                      type="tel"
                      placeholder="0968XXXXXX"
                      value={formData.phone}
                      onChange={handleInputChange}
                      className={`w-full rounded-xl border border-gray-200 p-3 font-sans text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent ${
                        errors.phone ? "border-primary" : ""
                      }`}
                      aria-required="true"
                      aria-describedby={errors.phone ? "phone-error" : undefined}
                    />
                    {errors.phone && (
                      <span id="phone-error" className="block font-sans text-xs text-primary mt-1" role="alert">
                        {errors.phone}
                      </span>
                    )}
                  </div>
                </div>

                {/* Course Selection select */}
                <div>
                  <label htmlFor="course" className="block font-sans text-xs font-semibold text-gray-700 uppercase tracking-wide mb-1">
                    Khóa học bạn quan tâm
                  </label>
                  <select
                    id="course"
                    name="course"
                    value={formData.course}
                    onChange={handleInputChange}
                    className="w-full rounded-xl border border-gray-200 p-3 font-sans text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                  >
                    {courses.map((course) => (
                      <option key={course.id} value={course.id}>
                        {course.title}
                      </option>
                    ))}
                    <option value="khac">Khác / Cần tư vấn thêm</option>
                  </select>
                </div>

                {/* Message input */}
                <div>
                  <label htmlFor="message" className="block font-sans text-xs font-semibold text-gray-700 uppercase tracking-wide mb-1">
                    Nội dung tin nhắn <span className="text-primary">*</span>
                  </label>
                  <textarea
                    id="message"
                    name="message"
                    rows="4"
                    placeholder="Em muốn tìm hiểu cụ thể chương trình học bổng..."
                    value={formData.message}
                    onChange={handleInputChange}
                    className={`w-full rounded-xl border border-gray-200 p-3 font-sans text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent ${
                      errors.message ? "border-primary" : ""
                    }`}
                    aria-required="true"
                    aria-describedby={errors.message ? "message-error" : undefined}
                  />
                  {errors.message && (
                    <span id="message-error" className="block font-sans text-xs text-primary mt-1" role="alert">
                      {errors.message}
                    </span>
                  )}
                </div>

                {/* Submit button */}
                <Button
                  type="submit"
                  variant="primary"
                  className="w-full mt-4"
                  isLoading={isSubmitting}
                >
                  Gửi thông tin tư vấn
                </Button>
              </form>
            </div>
          </div>

          {/* FAQ Accordion list */}
          <div className="max-w-4xl mx-auto">
            <SectionTitle
              title="Câu Hỏi Thường Gặp (FAQs)"
              subtitle="Giải đáp thắc mắc"
              align="center"
            />
            <Accordion items={faqItems} allowMultiple={true} />
          </div>
        </div>
      </section>
    </main>
  );
}
