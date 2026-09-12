import React from "react";
import { Link } from "react-router-dom";
import { MapPin, Phone, Mail, Globe } from "lucide-react";
import {
  CONTACT_INFO,
  NAV_LINKS,
  SITE_NAME,
  SITE_SLOGAN,
} from "../../data/constants";

const FacebookIcon = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="20"
    height="20"
    fill="currentColor"
    viewBox="0 0 24 24"
  >
    <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z" />
  </svg>
);

export default function Footer() {
  return (
    <footer className="bg-navy text-white pt-16 pb-8" aria-label="Chân trang">
      <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-12 mb-12">
          {/* Column 1: Brand Info */}
          <div>
            <div className="flex items-center space-x-2 mb-4">
              <span className="font-heading text-2xl font-extrabold text-primary tracking-tight">
                {SITE_NAME}
              </span>
              <span className="h-6 w-[2px] bg-gray-600" />
              <span className="font-sans text-xs font-semibold text-gray-400 tracking-wider uppercase">
                APTECH
              </span>
            </div>
            <p className="font-sans text-sm text-gray-300 mb-6 leading-relaxed">
              {SITE_SLOGAN}
            </p>
            <div className="flex space-x-4">
              <a
                href={CONTACT_INFO.facebook}
                target="_blank"
                rel="noopener noreferrer"
                className="w-10 h-10 rounded-full bg-gray-800 flex items-center justify-center text-gray-300 hover:text-white hover:bg-primary transition-all duration-300 focus-ring"
                aria-label="Facebook Bachkhoa-Aptech"
              >
                <FacebookIcon />
              </a>
              <a
                href="https://bachkhoa-aptech.edu.vn"
                target="_blank"
                rel="noopener noreferrer"
                className="w-10 h-10 rounded-full bg-gray-800 flex items-center justify-center text-gray-300 hover:text-white hover:bg-primary transition-all duration-300 focus-ring"
                aria-label="Website Bachkhoa-Aptech"
              >
                <Globe className="w-5 h-5" />
              </a>
            </div>
          </div>

          {/* Column 2: Quick Links */}
          <div>
            <h3 className="font-heading text-lg font-bold mb-4 relative inline-block pb-2">
              Liên kết nhanh
              <span className="absolute bottom-0 left-0 w-8 h-[2px] bg-primary" />
            </h3>
            <ul className="space-y-3 font-sans text-sm text-gray-300">
              {NAV_LINKS.map((link) => (
                <li key={link.path}>
                  <Link
                    to={link.path}
                    className="hover:text-primary transition-colors flex items-center space-x-2 focus-ring rounded"
                  >
                    <span>&rsaquo;</span>
                    <span>{link.label}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Column 3: Contact Info */}
          <div>
            <h3 className="font-heading text-lg font-bold mb-4 relative inline-block pb-2">
              Thông tin liên hệ
              <span className="absolute bottom-0 left-0 w-8 h-[2px] bg-primary" />
            </h3>
            <address className="space-y-4 font-sans text-sm text-gray-300 not-italic">
              <div className="flex items-start space-x-3">
                <MapPin className="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
                <span>{CONTACT_INFO.address}</span>
              </div>
              <div className="flex items-center space-x-3">
                <Phone className="w-5 h-5 text-primary flex-shrink-0" />
                <a
                  href={`tel:${CONTACT_INFO.phone.replace(/\./g, "")}`}
                  className="hover:text-primary transition-colors focus-ring rounded"
                >
                  {CONTACT_INFO.phone}
                </a>
              </div>
              <div className="flex items-center space-x-3">
                <Mail className="w-5 h-5 text-primary flex-shrink-0" />
                <a
                  href={`mailto:${CONTACT_INFO.email}`}
                  className="hover:text-primary transition-colors focus-ring rounded"
                >
                  {CONTACT_INFO.email}
                </a>
              </div>
            </address>
          </div>
        </div>

        {/* Divider & Copyright */}
        <div className="border-t border-gray-800 pt-8 text-center font-sans text-xs text-gray-400">
          <p>
            &copy; {new Date().getFullYear()} {SITE_NAME}. Tất cả quyền được bảo
            lưu. Đào tạo CNTT Quốc tế Bachkhoa-Aptech.
          </p>
        </div>
      </div>
    </footer>
  );
}
