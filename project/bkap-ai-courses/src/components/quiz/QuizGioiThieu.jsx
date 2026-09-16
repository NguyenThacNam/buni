import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertCircle, ClipboardList, Clock, ExternalLink, RotateCcw, Trophy } from "lucide-react";

import { api } from "../../api/Api";

/**
 * Thẻ giới thiệu một bài kiểm tra, nằm ngay trong trang học.
 *
 * Trước đây bấm vào bài kiểm tra là mở sang LMS. Giờ làm bài và xem kết quả
 * ngay trên buni; điểm vẫn do LMS chấm. Nút "Mở trên LMS" vẫn giữ làm đường lui
 * — cho bài có dạng câu hỏi buni chưa hỗ trợ, hoặc khi có trục trặc.
 */

const doiPhut = (giay) => (giay > 0 ? `${Math.round(giay / 60)} phút` : "Không giới hạn");

const doiDiem = (d) => (d === null || d === undefined ? "—" : Number(d).toFixed(2).replace(/\.?0+$/, ""));

const doiNgayGio = (giay) =>
  giay ? new Date(giay * 1000).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "";

export default function QuizGioiThieu({ courseId, muc, onMoTrenLms, dangMoLms }) {
  const navigate = useNavigate();
  const [bai, setBai] = useState(null);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);
  const [canDangNhapLai, setCanDangNhapLai] = useState(false);
  const [dangBatDau, setDangBatDau] = useState(false);

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    try {
      const res = await api("get", `/learn/quiz/${muc.cmid}?courseId=${courseId}`);
      setBai(res.data);
    } catch (err) {
      setCanDangNhapLai(err?.response?.data?.ma === "CAN_DANG_NHAP_LAI");
      setLoi(err?.response?.data?.error || "Chưa tải được thông tin bài kiểm tra.");
    } finally {
      setDangTai(false);
    }
  }, [courseId, muc.cmid]);

  useEffect(() => {
    tai();
  }, [tai]);

  const batDau = async () => {
    setDangBatDau(true);
    try {
      const res = await api("post", `/learn/quiz/${muc.cmid}/start?courseId=${courseId}`);
      navigate(`/hoc/${courseId}/kiem-tra/${muc.cmid}/lam/${res.data.attemptId}`);
    } catch (err) {
      setLoi(err?.response?.data?.error || "Chưa bắt đầu được bài kiểm tra.");
      setDangBatDau(false);
    }
  };

  if (dangTai) {
    return <div className="h-40 animate-pulse rounded-xl bg-gray-100" />;
  }

  const duongLui = (
    <button
      type="button"
      onClick={onMoTrenLms}
      disabled={dangMoLms}
      className="inline-flex items-center gap-1.5 text-xs text-gray-400 underline-offset-2 hover:text-primary hover:underline disabled:opacity-60"
    >
      {dangMoLms ? "Đang mở..." : "Làm trên hệ thống LMS"}
      <ExternalLink className="h-3.5 w-3.5" />
    </button>
  );

  if (loi && !bai) {
    return (
      <div className="rounded-xl border border-gray-200 bg-gray-50 p-6 text-center">
        <AlertCircle className="mx-auto mb-3 h-10 w-10 text-primary" strokeWidth={1.4} />
        <p className="mb-4 text-sm text-gray-600">{loi}</p>
        {canDangNhapLai ? (
          <p className="text-xs text-gray-400">
            Phiên đăng nhập của bạn tạo từ trước khi có tính năng làm bài trên buni.
          </p>
        ) : (
          <button
            type="button"
            onClick={tai}
            className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-white hover:bg-primary-dark"
          >
            Thử lại
          </button>
        )}
        <div className="mt-4">{duongLui}</div>
      </div>
    );
  }

  const daNop = bai.cacLuot.filter((l) => l.trangThai === "finished");
  const soLanConLai = bai.soLanToiDa > 0 ? Math.max(0, bai.soLanToiDa - daNop.length) : null;
  const dat = bai.diemDat && bai.diemCaoNhat !== null && bai.diemCaoNhat >= bai.diemDat;

  return (
    <div className="space-y-5">
      {/* Thông số chính */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div className="rounded-xl border border-gray-200 p-4">
          <Clock className="mb-2 h-5 w-5 text-primary" />
          <p className="text-xs text-gray-500">Thời gian làm bài</p>
          <p className="font-heading text-lg font-bold text-gray-900">{doiPhut(bai.thoiGianGiay)}</p>
        </div>
        <div className="rounded-xl border border-gray-200 p-4">
          <RotateCcw className="mb-2 h-5 w-5 text-primary" />
          <p className="text-xs text-gray-500">Số lần làm</p>
          <p className="font-heading text-lg font-bold text-gray-900">
            {soLanConLai === null ? "Không giới hạn" : `Còn ${soLanConLai}/${bai.soLanToiDa} lần`}
          </p>
        </div>
        <div className="rounded-xl border border-gray-200 p-4">
          <Trophy className="mb-2 h-5 w-5 text-primary" />
          <p className="text-xs text-gray-500">Điểm cao nhất</p>
          <p className="font-heading text-lg font-bold text-gray-900">
            {doiDiem(bai.diemCaoNhat)}
            <span className="text-sm font-normal text-gray-400"> / {doiDiem(bai.diemToiDa)}</span>
          </p>
          {bai.diemDat ? (
            <p className={`mt-0.5 text-xs font-semibold ${dat ? "text-green-600" : "text-gray-400"}`}>
              {dat ? "Đã đạt" : `Cần ${doiDiem(bai.diemDat)} điểm để đạt`}
            </p>
          ) : null}
        </div>
      </div>

      {/* Nút chính */}
      <div className="rounded-xl border border-gray-200 bg-gray-50 p-6 text-center">
        <ClipboardList className="mx-auto mb-3 h-10 w-10 text-primary" strokeWidth={1.4} />
        {bai.lyDoChan.length > 0 ? (
          <div className="mb-2 space-y-1 text-sm text-gray-600">
            {bai.lyDoChan.map((l, i) => (
              <p key={i}>{l}</p>
            ))}
          </div>
        ) : null}

        {bai.luotDangDo ? (
          <button
            type="button"
            onClick={() => navigate(`/hoc/${courseId}/kiem-tra/${muc.cmid}/lam/${bai.luotDangDo}`)}
            className="rounded-full bg-primary px-8 py-3 text-sm font-bold uppercase tracking-wider text-white shadow-md hover:bg-primary-dark"
          >
            Tiếp tục làm bài
          </button>
        ) : bai.duocLam && soLanConLai !== 0 ? (
          <button
            type="button"
            onClick={batDau}
            disabled={dangBatDau}
            className="rounded-full bg-primary px-8 py-3 text-sm font-bold uppercase tracking-wider text-white shadow-md hover:bg-primary-dark disabled:opacity-60"
          >
            {dangBatDau ? "Đang chuẩn bị..." : daNop.length > 0 ? "Làm lại" : "Bắt đầu làm bài"}
          </button>
        ) : (
          <p className="text-sm font-semibold text-gray-500">Bạn đã hết lượt làm bài này.</p>
        )}

        {bai.thoiGianGiay > 0 && !bai.luotDangDo ? (
          <p className="mt-3 text-xs text-gray-400">
            Đồng hồ bắt đầu chạy ngay khi bấm, và bài tự nộp khi hết giờ.
          </p>
        ) : null}
        {loi ? <p className="mt-3 text-sm text-primary">{loi}</p> : null}
      </div>

      {/* Các lượt đã nộp */}
      {daNop.length > 0 && (
        <div>
          <p className="mb-2 text-sm font-bold text-gray-900">Các lần đã làm</p>
          <ul className="divide-y divide-gray-100 rounded-xl border border-gray-200">
            {daNop
              .slice()
              .reverse()
              .map((l) => (
                <li key={l.id} className="flex items-center justify-between gap-3 px-4 py-3 text-sm">
                  <div>
                    <p className="font-semibold text-gray-800">
                      Lần {l.lanThu}
                      {l.xemTruoc ? <span className="ml-2 text-xs font-normal text-gray-400">(xem trước)</span> : null}
                    </p>
                    <p className="text-xs text-gray-400">{doiNgayGio(l.ketThuc)}</p>
                  </div>
                  {bai.xemLaiDuoc ? (
                    <button
                      type="button"
                      onClick={() => navigate(`/hoc/${courseId}/kiem-tra/${muc.cmid}/ket-qua/${l.id}`)}
                      className="text-sm font-semibold text-primary hover:underline"
                    >
                      Xem kết quả
                    </button>
                  ) : null}
                </li>
              ))}
          </ul>
        </div>
      )}

      <div className="text-center">{duongLui}</div>
    </div>
  );
}
