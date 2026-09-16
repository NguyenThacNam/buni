import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { AlertCircle, ArrowLeft, Check, X } from "lucide-react";

import { api } from "../../api/Api";
import { bocKetQua } from "../../utils/quizHtml";

/**
 * Kết quả một lượt đã nộp. Điểm và đáp án đúng do Moodle trả về, trong phạm vi
 * cài đặt "xem lại" của bài — Moodle không cho xem thì buni cũng không có mà hiện.
 */

// Trong phần tóm tắt Moodle gửi kèm (giờ bắt đầu, thời gian làm, điểm...), buni
// đã tự hiện điểm — chỉ lấy thêm lời nhận xét chung giáo viên cài theo mức điểm.
const TOM_TAT_CAN_HIEN = ["feedback", "overallfeedback"];

export default function QuizKetQuaPage() {
  const { courseId, cmid, attemptId } = useParams();
  const veTrangHoc = `/hoc/${courseId}?muc=${cmid}`;

  const [kq, setKq] = useState(null);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    try {
      const res = await api("get", `/learn/quiz/attempt/${attemptId}/review`);
      setKq(res.data);
    } catch (err) {
      setLoi(err?.response?.data?.error || "Không tải được kết quả. Vui lòng thử lại.");
    } finally {
      setDangTai(false);
    }
  }, [attemptId]);

  useEffect(() => {
    tai();
  }, [tai]);

  const cauHoi = useMemo(
    () => (kq?.cauHoi || []).map((c) => ({ ...c, boc: bocKetQua(c.html) })),
    [kq],
  );

  if (dangTai) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-9 w-9 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (loi) {
    return (
      <div className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center px-4 text-center">
        <AlertCircle className="mb-4 h-12 w-12 text-primary" strokeWidth={1.3} />
        <p className="mb-6 text-gray-600">{loi}</p>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={tai}
            className="rounded-full bg-primary px-6 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark"
          >
            Thử lại
          </button>
          <Link
            to={veTrangHoc}
            className="rounded-full border border-gray-300 px-6 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50"
          >
            Về trang học
          </Link>
        </div>
      </div>
    );
  }

  const soDung = cauHoi.filter((c) => c.trangThai === "gradedright").length;
  const coChamTungCau = cauHoi.some((c) => c.trangThai.startsWith("graded"));
  const tomTat = (kq.tomTat || []).filter((t) => TOM_TAT_CAN_HIEN.includes(t.ma) && t.noiDung.trim());

  return (
    <div className="min-h-screen bg-gray-50 pb-16 pt-6">
      <div className="mx-auto max-w-3xl px-4">
        <Link to={veTrangHoc} className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-primary">
          <ArrowLeft className="h-4 w-4" />
          Về trang học
        </Link>

        {/* Điểm */}
        <div className="mb-6 rounded-2xl border border-gray-200 bg-white p-6 text-center">
          <p className="text-sm text-gray-500">Điểm của bạn</p>
          <p className="font-heading text-5xl font-bold text-primary">{kq.diem || "—"}</p>
          {coChamTungCau && (
            <p className="mt-2 text-sm text-gray-600">
              Đúng <b>{soDung}</b>/{cauHoi.length} câu
            </p>
          )}
          {tomTat.length > 0 && (
            <div className="mt-4 space-y-1 text-sm text-gray-600">
              {tomTat.map((t, i) => (
                <div key={i} dangerouslySetInnerHTML={{ __html: t.noiDung }} />
              ))}
            </div>
          )}
        </div>

        {/* Xem lại từng câu — chỉ có khi cài đặt của bài cho xem */}
        {cauHoi.length === 0 ? (
          <p className="text-center text-sm text-gray-500">
            Bài kiểm tra này không cho xem lại chi tiết từng câu.
          </p>
        ) : (
          <div className="space-y-4">
            {cauHoi.map((c, i) => {
              const dung = c.trangThai === "gradedright";
              const sai = c.trangThai === "gradedwrong" || c.trangThai === "gradedpartial";
              return (
                <div key={c.slot} className="rounded-xl border border-gray-200 bg-white p-5">
                  <div className="mb-2 flex items-center justify-between gap-3">
                    <p className="text-xs font-bold uppercase tracking-wider text-primary">Câu {c.soThuTu || i + 1}</p>
                    {c.trangThai.startsWith("graded") && (
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          dung ? "bg-green-50 text-green-700" : sai ? "bg-red-50 text-red-600" : "bg-gray-100 text-gray-500"
                        }`}
                      >
                        {dung ? <Check className="h-3.5 w-3.5" /> : <X className="h-3.5 w-3.5" />}
                        {dung ? "Đúng" : c.trangThai === "gradedpartial" ? "Đúng một phần" : "Sai"}
                      </span>
                    )}
                    {c.trangThai === "gaveup" && (
                      <span className="rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-semibold text-gray-500">
                        Chưa trả lời
                      </span>
                    )}
                  </div>

                  <div className="noi-dung-lms mb-4 text-gray-900" dangerouslySetInnerHTML={{ __html: c.boc.deBai }} />
                  <div className="space-y-2">
                    {c.boc.luaChon.map((l, j) => (
                      <div
                        key={j}
                        className={`flex items-start gap-3 rounded-lg border px-4 py-3 text-sm ${
                          l.dung
                            ? "border-green-300 bg-green-50"
                            : l.daChon && (l.sai || c.boc.coDapAnDung)
                              ? "border-red-300 bg-red-50"
                              : l.daChon
                                ? "border-primary bg-primary/5"
                                : "border-gray-200"
                        }`}
                      >
                        <span className="font-semibold text-gray-500">{String.fromCharCode(65 + j)}.</span>
                        <span className="flex-1 text-gray-800" dangerouslySetInnerHTML={{ __html: l.nhan }} />
                        {l.daChon && <span className="text-xs font-semibold text-gray-500">Bạn chọn</span>}
                        {l.dung && <Check className="h-4 w-4 flex-shrink-0 text-green-600" />}
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
