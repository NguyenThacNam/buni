import React, { useCallback, useEffect, useState } from "react";
import { AlertCircle, CalendarCheck, Hand, QrCode } from "lucide-react";

import { api } from "../../api/Api";

/**
 * Bảng điểm danh của chính học viên trong một hoạt động điểm danh.
 *
 * Giáo viên điểm danh trên LMS; buni hiện lại để học viên tự theo dõi buổi nào
 * có mặt, vắng, muộn và tỷ lệ chuyên cần.
 *
 * Buổi giáo viên cho học viên tự điểm danh, đang trong giờ và KHÔNG kèm mã QR
 * thì có nút "Điểm danh", bấm là ghi ngay trên buni.
 *
 * Buổi kèm mã QR thì buni chỉ báo "quét mã QR trên lớp": mã QR do giáo viên
 * chiếu, học viên quét bằng điện thoại. buni không kiểm được mật khẩu nên không
 * ghi hộ, và cũng không đưa link sang LMS (chốt 21/09/2026).
 */

const THU = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

const doiNgay = (giay) => {
  const d = new Date(giay * 1000);
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  return `${THU[d.getDay()]}, ${dd}/${mm}/${d.getFullYear()}`;
};

const doiGio = (giay, phut) => {
  const bd = new Date(giay * 1000);
  const kt = new Date((giay + (phut || 0) * 60) * 1000);
  const f = (d) =>
    `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  return phut ? `${f(bd)} – ${f(kt)}` : f(bd);
};

/**
 * Màu theo điểm của trạng thái chứ không theo tên: mỗi trung tâm đặt tên khác
 * nhau ("Có mặt", "Present", "P"...), còn điểm thì luôn là đủ / một phần / 0.
 */
const mauTrangThai = (t) => {
  if (!t) return "bg-gray-100 text-gray-500";
  if (t.diemToiDa > 0 && t.diem >= t.diemToiDa)
    return "bg-green-50 text-green-700";
  if (t.diem > 0) return "bg-amber-50 text-amber-700";
  return "bg-red-50 text-red-600";
};

export default function BangDiemDanh({ courseId, cmid }) {
  const [duLieu, setDuLieu] = useState(null);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);
  const [dangDiemDanh, setDangDiemDanh] = useState(null); // id buổi đang gửi
  const [thongBao, setThongBao] = useState(null); // {loai: "ok" | "loi", chu}

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    try {
      const res = await api("get", `/learn/${courseId}/attendance/${cmid}`);
      setDuLieu(res.data);
    } catch (err) {
      setLoi(err?.response?.data?.error || "Chưa tải được dữ liệu điểm danh.");
    } finally {
      setDangTai(false);
    }
  }, [courseId, cmid]);

  useEffect(() => {
    tai();
  }, [tai]);

  const diemDanh = async (buoi) => {
    setDangDiemDanh(buoi.id);
    setThongBao(null);
    try {
      const res = await api(
        "post",
        `/learn/${courseId}/attendance/${cmid}/sessions/${buoi.id}/mark`,
      );
      setDuLieu(res.data);
      const moi = res.data.buoi.find((b) => b.id === buoi.id);
      setThongBao({
        loai: "ok",
        chu: `Đã điểm danh: ${moi?.trangThai?.ten || "thành công"}.`,
      });
    } catch (err) {
      setThongBao({
        loai: "loi",
        chu:
          err?.response?.data?.error ||
          "Chưa điểm danh được. Vui lòng thử lại.",
      });
    } finally {
      setDangDiemDanh(null);
    }
  };

  const nutDiemDanh = (b, lon = false) => {
    if (!b.coTheDiemDanh) return null;
    const co = lon ? "px-5 py-2.5 text-sm" : "px-3 py-1 text-xs";
    return b.canQr ? (
      <span
        className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full bg-gray-100 font-semibold text-gray-500 ${co}`}
      >
        <QrCode className="h-3.5 w-3.5" />
        Quét mã QR trên lớp
      </span>
    ) : (
      <button
        type="button"
        onClick={() => diemDanh(b)}
        disabled={dangDiemDanh !== null}
        className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full bg-primary font-semibold text-white hover:bg-primary-dark disabled:opacity-60 ${co}`}
      >
        <Hand className="h-3.5 w-3.5" />
        {dangDiemDanh === b.id ? "Đang gửi..." : "Điểm danh"}
      </button>
    );
  };

  if (dangTai) {
    return <div className="h-48 animate-pulse rounded-xl bg-gray-100" />;
  }

  if (loi) {
    return (
      <div className="rounded-xl border border-gray-200 bg-gray-50 p-6 text-center">
        <AlertCircle
          className="mx-auto mb-3 h-10 w-10 text-primary"
          strokeWidth={1.4}
        />
        <p className="mb-4 text-sm text-gray-600">{loi}</p>
        <button
          type="button"
          onClick={tai}
          className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-white hover:bg-primary-dark"
        >
          Thử lại
        </button>
      </div>
    );
  }

  if (!duLieu.buoi.length) {
    return (
      <div className="rounded-xl border border-gray-200 bg-gray-50 p-8 text-center">
        <CalendarCheck
          className="mx-auto mb-3 h-10 w-10 text-gray-300"
          strokeWidth={1.4}
        />
        <p className="text-sm text-gray-500">
          Chưa có buổi học nào được lên lịch điểm danh.
        </p>
      </div>
    );
  }

  const tyLe = duLieu.tyLe;
  const buoiDangMo = duLieu.buoi.filter((b) => b.coTheDiemDanh);
  const mauTyLe =
    tyLe === null
      ? "text-gray-400"
      : tyLe >= 80
        ? "text-green-600"
        : tyLe >= 50
          ? "text-amber-600"
          : "text-red-600";

  return (
    <div className="space-y-5">
      {/* Buổi đang mở điểm danh: đưa lên đầu cho học viên thấy ngay. */}
      {buoiDangMo.map((b) => (
        <div
          key={b.id}
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-primary/30 bg-primary/5 p-4"
        >
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-primary">
              Đang mở điểm danh
            </p>
            <p className="font-semibold text-gray-900">
              {doiNgay(b.batDau)} · {doiGio(b.batDau, b.thoiLuongPhut)}
            </p>
            {b.canQr && (
              <p className="mt-0.5 text-xs text-gray-500">
                Buổi này điểm danh bằng mã QR: quét mã giáo viên chiếu trên lớp.
              </p>
            )}
          </div>
          {nutDiemDanh(b, true)}
        </div>
      ))}

      {thongBao && (
        <p
          className={`rounded-lg px-4 py-2.5 text-sm ${
            thongBao.loai === "ok"
              ? "bg-green-50 text-green-700"
              : "bg-red-50 text-red-600"
          }`}
        >
          {thongBao.chu}
        </p>
      )}

      {/* Tổng quan */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="rounded-xl border border-gray-200 p-4">
          <p className="text-xs text-gray-500">Tỷ lệ chuyên cần</p>
          <p
            className={`font-heading text-2xl font-bold tabular-nums ${mauTyLe}`}
          >
            {tyLe === null ? "—" : `${tyLe}%`}
          </p>
        </div>
        <div className="rounded-xl border border-gray-200 p-4">
          <p className="text-xs text-gray-500">Đã điểm danh</p>
          <p className="font-heading text-2xl font-bold tabular-nums text-gray-900">
            {duLieu.soBuoiDaDiemDanh}
            <span className="text-sm font-normal text-gray-400">
              {" "}
              / {duLieu.soBuoi} buổi
            </span>
          </p>
        </div>
        {Object.entries(duLieu.demTheoTrangThai)
          .slice(0, 2)
          .map(([ten, so]) => (
            <div key={ten} className="rounded-xl border border-gray-200 p-4">
              <p className="truncate text-xs text-gray-500">{ten}</p>
              <p className="font-heading text-2xl font-bold tabular-nums text-gray-900">
                {so}
              </p>
            </div>
          ))}
      </div>

      {/* Từng buổi */}
      <div className="overflow-x-auto rounded-xl border border-gray-200">
        <table className="w-full min-w-[520px] text-sm">
          <thead className="bg-gray-50 text-left text-xs uppercase tracking-wider text-gray-500">
            <tr>
              <th className="px-4 py-3 font-semibold">Buổi</th>
              <th className="px-4 py-3 font-semibold">Giờ học</th>
              <th className="px-4 py-3 font-semibold">Trạng thái</th>
              <th className="px-4 py-3 font-semibold">Ghi chú</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {duLieu.buoi.map((b) => (
              <tr key={b.id} className="align-top">
                <td className="px-4 py-3">
                  <p className="font-semibold tabular-nums text-gray-800">
                    {doiNgay(b.batDau)}
                  </p>
                  {b.moTa && (
                    <p className="mt-0.5 text-xs text-gray-500">{b.moTa}</p>
                  )}
                </td>
                <td className="whitespace-nowrap px-4 py-3 tabular-nums text-gray-600">
                  {doiGio(b.batDau, b.thoiLuongPhut)}
                </td>
                <td className="px-4 py-3">
                  {b.coTheDiemDanh ? (
                    nutDiemDanh(b)
                  ) : (
                    <span
                      className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ${mauTrangThai(b.trangThai)}`}
                    >
                      {b.trangThai
                        ? b.trangThai.ten || b.trangThai.kyHieu
                        : b.batDau * 1000 > Date.now()
                          ? "Sắp diễn ra"
                          : "Chưa điểm danh"}
                    </span>
                  )}
                </td>
                <td className="px-4 py-3 text-gray-500">{b.ghiChu || ""}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
