import React, { useCallback, useEffect, useRef, useState } from "react";
import { Bell } from "lucide-react";

import { api } from "../../api/Api";

/**
 * Chuông thông báo, lấy đúng chuông của LMS.
 *
 * Nội dung do Moodle sinh ra: giáo viên đăng thông báo, bài kiểm tra sắp đóng,
 * có người trả lời diễn đàn... buni chỉ hiện lại và đánh dấu đã đọc, nên đọc ở
 * buni thì sang LMS cũng hết báo đỏ.
 *
 * Bỏ qua loại "đăng nhập mới" ở phía máy chủ (xem LmsThongBaoService): chính
 * buni sinh ra nó mỗi lần học viên đăng nhập.
 *
 * Bấm một thông báo thì MỞ NGAY TRONG CHUÔNG. Trung tâm chốt học viên chỉ học
 * trên buni, không đẩy sang LMS, nên ở đây không còn link "Mở trên LMS" (bỏ
 * 21/09/2026) — nội dung thông báo vốn chỉ là mấy dòng chữ, đọc tại chỗ là đủ.
 */

const SO_LUONG = 10;
/** Nhịp hỏi lại số chưa đọc. Thưa thôi — thông báo lớp học không gấp như tin nhắn. */
const NHIP_LAM_MOI_MS = 2 * 60 * 1000;

const doiThoiGian = (giay) => {
  const phut = Math.round((Date.now() / 1000 - giay) / 60);
  if (phut < 1) return "Vừa xong";
  if (phut < 60) return `${phut} phút trước`;
  const gio = Math.round(phut / 60);
  if (gio < 24) return `${gio} giờ trước`;
  const ngay = Math.round(gio / 24);
  if (ngay < 7) return `${ngay} ngày trước`;
  return new Date(giay * 1000).toLocaleDateString("vi-VN");
};

export default function ChuongThongBao() {
  const [mo, setMo] = useState(false);
  const [chuaDoc, setChuaDoc] = useState(0);
  const [ds, setDs] = useState([]);
  const [dangTai, setDangTai] = useState(false);
  const [loi, setLoi] = useState(null);
  const [dangXem, setDangXem] = useState(null); // id thông báo đang mở rộng
  const khungRef = useRef(null);

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    try {
      const res = await api("get", `/notifications?soLuong=${SO_LUONG}`);
      setDs(res.data.thongBao || []);
      setChuaDoc(res.data.chuaDoc || 0);
    } catch (err) {
      setLoi(
        err?.response?.data?.ma === "CAN_DANG_NHAP_LAI"
          ? "Đăng xuất rồi đăng nhập lại để xem thông báo."
          : "Chưa tải được thông báo.",
      );
    } finally {
      setDangTai(false);
    }
  }, []);

  // Chỉ hỏi số chưa đọc cho nhẹ; danh sách chỉ tải khi mở chuông.
  useEffect(() => {
    let conDung = true;
    const dem = () =>
      api("get", "/notifications?soLuong=1")
        .then((res) => conDung && setChuaDoc(res.data.chuaDoc || 0))
        .catch(() => {});
    dem();
    const id = setInterval(dem, NHIP_LAM_MOI_MS);
    return () => {
      conDung = false;
      clearInterval(id);
    };
  }, []);

  // Bấm ra ngoài thì đóng.
  useEffect(() => {
    const ngoai = (e) => {
      if (khungRef.current && !khungRef.current.contains(e.target)) setMo(false);
    };
    document.addEventListener("mousedown", ngoai);
    return () => document.removeEventListener("mousedown", ngoai);
  }, []);

  const bamChuong = () => {
    const sapMo = !mo;
    setMo(sapMo);
    if (sapMo) tai();
  };

  /** Bấm vào một dòng: mở/đóng nội dung ngay tại chỗ và đánh dấu đã đọc. */
  const bamThongBao = async (tb) => {
    setDangXem((cu) => (cu === tb.id ? null : tb.id));
    if (tb.daDoc) return;

    setDs((cu) => cu.map((x) => (x.id === tb.id ? { ...x, daDoc: true } : x)));
    try {
      // Không gửi kèm đường dẫn: chỉ cần đánh dấu đã đọc, không mở gì cả.
      const res = await api("post", `/notifications/${tb.id}/open`);
      setChuaDoc(res.data.chuaDoc || 0);
    } catch {
      // Đánh dấu hỏng thì thôi, lần mở chuông sau sẽ đúng lại.
    }
  };

  return (
    <div className="relative" ref={khungRef}>
      <button
        type="button"
        onClick={bamChuong}
        aria-label="Thông báo"
        className="relative flex h-9 w-9 items-center justify-center rounded-full text-gray-500 transition-colors hover:bg-gray-100 hover:text-primary"
      >
        <Bell className="h-5 w-5" />
        {chuaDoc > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-white">
            {chuaDoc > 99 ? "99+" : chuaDoc}
          </span>
        )}
      </button>

      {mo && (
        <div className="absolute right-0 top-11 z-50 w-80 overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-xl sm:w-96">
          <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
            <p className="font-heading text-sm font-bold text-gray-900">Thông báo</p>
            {chuaDoc > 0 && (
              <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                {chuaDoc} chưa đọc
              </span>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {dangTai ? (
              <div className="space-y-2 p-4">
                {[0, 1, 2].map((i) => (
                  <div key={i} className="h-12 animate-pulse rounded-lg bg-gray-100" />
                ))}
              </div>
            ) : loi ? (
              <p className="px-4 py-8 text-center text-sm text-gray-500">{loi}</p>
            ) : ds.length === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-gray-500">
                Chưa có thông báo nào.
              </p>
            ) : (
              <ul className="divide-y divide-gray-50">
                {ds.map((tb) => (
                  <li key={tb.id}>
                    <button
                      type="button"
                      onClick={() => bamThongBao(tb)}
                      className={`flex w-full gap-3 px-4 py-3 text-left transition-colors hover:bg-gray-50 ${
                        tb.daDoc ? "" : "bg-primary/5"
                      }`}
                    >
                      <span
                        className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${
                          tb.daDoc ? "bg-transparent" : "bg-primary"
                        }`}
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm font-semibold leading-snug text-gray-900">
                          {tb.tieuDe}
                        </span>
                        {tb.tomTat && (
                          <span className="mt-0.5 block line-clamp-2 text-xs text-gray-500">
                            {tb.tomTat}
                          </span>
                        )}
                        <span className="mt-1 block text-xs text-gray-400">
                          {doiThoiGian(tb.thoiGian)}
                        </span>

                        {/* Nội dung đầy đủ, mở ngay tại chỗ khi bấm. */}
                        {dangXem === tb.id && (
                          <span className="mt-2 block border-t border-gray-100 pt-2">
                            {tb.noiDung && (
                              <span className="block whitespace-pre-line text-xs leading-relaxed text-gray-600">
                                {tb.noiDung}
                              </span>
                            )}
                          </span>
                        )}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
