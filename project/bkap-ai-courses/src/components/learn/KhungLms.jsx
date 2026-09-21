import React, { useCallback, useEffect, useRef, useState } from "react";
import { AlertCircle, Maximize2, Minimize2, RotateCcw } from "lucide-react";

import { api } from "../../api/Api";
import { LMS_URL } from "../../data/constants";

/**
 * Khung nhúng trình phát của LMS vào trang học.
 *
 * Dùng cho bài giảng SCORM và bài tương tác H5P: hai loại này là gói web do phần
 * mềm soạn bài xuất ra, phải chạy trong trình phát có sẵn cơ chế ghi điểm và ghi
 * vị trí đang học. buni không dựng lại được nên nhúng trình phát của Moodle, còn
 * người học thì vẫn ở trên buni.
 *
 * Nhúng ĐÚNG TRÌNH PHÁT chứ không nhúng trang hoạt động của Moodle (xem nhungUrl
 * ở LmsContentService): nhúng trang hoạt động sẽ kéo theo cả menu, thanh bên,
 * chân trang, nhìn như lồng nguyên website Moodle vào giữa bài học.
 *
 * Về đăng nhập: khung cần phiên đăng nhập Moodle của chính học viên. Đường dẫn
 * đăng nhập một lần CHỈ DÙNG ĐƯỢC MỘT LẦN — nạp lần hai là Moodle bỏ qua đích
 * đến và trả về Bảng điều khiển. Nên ở đây nạp nó một lần trong khung ẩn để tạo
 * phiên, rồi mới mở trình phát bằng đường dẫn thường, và nhớ lại trong phiên
 * trình duyệt để lần sau khỏi làm lại.
 *
 * Bài H5P tự báo chiều cao nội dung về trang cha (cơ chế sẵn có của H5P), nên
 * khung co giãn vừa đúng bài, không để thừa khoảng trắng hay sinh thanh cuộn
 * lồng nhau. SCORM không có cơ chế đó nên giữ chiều cao cố định.
 *
 * Chỉ chạy khi buni và LMS cùng tên miền gốc (buni.vn và lms.buni.vn). Chạy ở
 * localhost thì trình duyệt không gửi phiên Moodle vào khung, khung sẽ hiện màn
 * hình đăng nhập của Moodle.
 */

const CO_PHIEN = "buni.phien-lms";

export default function KhungLms({ courseId, muc }) {
  const boxRef = useRef(null);
  const [urlDangNhap, setUrlDangNhap] = useState(null); // khung ẩn, chỉ để tạo phiên
  const [sanSang, setSanSang] = useState(false);
  const [loi, setLoi] = useState(null);
  const [toanManHinh, setToanManHinh] = useState(false);
  const [chieuCao, setChieuCao] = useState(null); // px, do bài H5P tự báo về

  const nhungUrl = muc.nhungUrl || muc.moodleUrl;

  const coPhien = () => {
    try {
      return sessionStorage.getItem(CO_PHIEN) === "1";
    } catch {
      return false;
    }
  };

  const chuanBi = useCallback(async () => {
    setLoi(null);
    setSanSang(false);
    setUrlDangNhap(null);

    if (coPhien()) {
      setSanSang(true);
      return;
    }
    try {
      const res = await api("post", "/learn/lms-url", {
        courseId: String(courseId),
        cmid: String(muc.cmid),
        loai: muc.type,
      });
      setUrlDangNhap(res.data.loginUrl);
    } catch (err) {
      setLoi(err?.response?.data?.error || "Chưa mở được nội dung này. Vui lòng thử lại.");
    }
  }, [courseId, muc.cmid, muc.type]);

  useEffect(() => {
    chuanBi();
  }, [chuanBi]);

  /** Khung ẩn nạp xong nghĩa là phiên Moodle đã có. */
  const daTaoPhien = () => {
    try {
      sessionStorage.setItem(CO_PHIEN, "1");
    } catch {
      // Trình duyệt chặn lưu thì thôi, lần sau tạo phiên lại.
    }
    setSanSang(true);
  };

  const taiLai = () => {
    setChieuCao(null);
    try {
      sessionStorage.removeItem(CO_PHIEN);
    } catch {
      // Không xóa được thì vẫn nạp lại khung như thường.
    }
    chuanBi();
  };

  /**
   * Bài H5P gửi chiều cao thật qua postMessage. Nhận đúng tin từ LMS rồi đặt lại
   * chiều cao khung; tin nhắn "hello" phải trả lời thì H5P mới gửi tiếp.
   */
  useEffect(() => {
    if (muc.type !== "h5pactivity") return undefined;
    let goc;
    try {
      goc = new URL(LMS_URL).origin;
    } catch {
      return undefined;
    }
    const nhan = (e) => {
      if (e.origin !== goc || e.data?.context !== "h5p") return;
      if (e.data.action === "hello") {
        e.source?.postMessage({ context: "h5p", action: "hello" }, goc);
      }
      const cao = Number(e.data.scrollHeight);
      if (cao > 0) setChieuCao(Math.max(240, Math.round(cao)));
    };
    window.addEventListener("message", nhan);
    return () => window.removeEventListener("message", nhan);
  }, [muc.type]);

  useEffect(() => {
    const doi = () => setToanManHinh(document.fullscreenElement === boxRef.current);
    document.addEventListener("fullscreenchange", doi);
    return () => document.removeEventListener("fullscreenchange", doi);
  }, []);

  const batTatToanManHinh = () => {
    if (document.fullscreenElement) document.exitFullscreen?.();
    else boxRef.current?.requestFullscreen?.();
  };

  if (loi) {
    return (
      <div className="rounded-xl border border-gray-200 bg-gray-50 p-6 text-center">
        <AlertCircle className="mx-auto mb-3 h-10 w-10 text-primary" strokeWidth={1.4} />
        <p className="mb-4 text-sm text-gray-600">{loi}</p>
        <button
          type="button"
          onClick={taiLai}
          className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-white hover:bg-primary-dark"
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div
      ref={boxRef}
      className={`w-full min-w-0 overflow-hidden border border-gray-200 bg-white ${
        toanManHinh ? "flex h-screen flex-col rounded-none" : "rounded-xl"
      }`}
    >
      <div className="flex shrink-0 flex-wrap items-center justify-between gap-2 border-b border-gray-200 bg-gray-50 px-3 py-2">
        <p className="text-xs text-gray-500">
          Nội dung tương tác — kết quả được ghi vào hệ thống của trung tâm
        </p>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={taiLai}
            aria-label="Tải lại"
            title="Tải lại"
            className="rounded-md p-1.5 text-gray-600 hover:bg-gray-200"
          >
            <RotateCcw className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={batTatToanManHinh}
            aria-label={toanManHinh ? "Thoát toàn màn hình" : "Toàn màn hình"}
            title={toanManHinh ? "Thoát toàn màn hình" : "Toàn màn hình"}
            className="rounded-md p-1.5 text-gray-600 hover:bg-gray-200"
          >
            {toanManHinh ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </button>
        </div>
      </div>

      {/* Khung ẩn: nạp đường dẫn đăng nhập một lần để tạo phiên, rồi thôi. */}
      {urlDangNhap && !sanSang && (
        <iframe
          src={urlDangNhap}
          title="Đang chuẩn bị"
          onLoad={daTaoPhien}
          className="h-0 w-0 border-0"
        />
      )}

      {sanSang ? (
        <iframe
          key={nhungUrl}
          src={nhungUrl}
          title={muc.name}
          allow="autoplay; fullscreen; clipboard-write"
          allowFullScreen
          style={!toanManHinh && chieuCao ? { height: `${chieuCao}px` } : undefined}
          className={`w-full border-0 bg-white ${
            toanManHinh ? "flex-1" : chieuCao ? "" : "h-[75vh]"
          }`}
        />
      ) : (
        <div className="flex h-[75vh] items-center justify-center">
          <div className="h-9 w-9 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      )}
    </div>
  );
}
