import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AlertCircle, ArrowLeft, Clock, Send } from "lucide-react";

import { api } from "../../api/Api";
import { bocCauHoi, goiDapAn } from "../../utils/quizHtml";

/**
 * Làm một lượt bài kiểm tra trên buni.
 *
 * Mọi thứ quyết định kết quả nằm bên LMS: buni chỉ gửi lựa chọn, Moodle lưu,
 * chấm và khóa bài khi hết giờ. Đồng hồ ở đây chỉ để người học nhìn — kể cả
 * sửa giờ máy tính cũng không kéo dài được bài, vì Moodle tự tính theo giờ của
 * nó (xem hetGioLuc / gioMayChu ở LmsQuizService.cauHoi).
 */

const TU_LUU_SAU_MS = 1500;

const doiDongHo = (giay) => {
  const s = Math.max(0, giay);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const ss = String(s % 60).padStart(2, "0");
  return h > 0 ? `${h}:${String(m).padStart(2, "0")}:${ss}` : `${m}:${ss}`;
};

export default function QuizLamBaiPage() {
  const { courseId, cmid, attemptId } = useParams();
  const navigate = useNavigate();
  const veTrangHoc = `/hoc/${courseId}?muc=${cmid}`;

  const [cauHoi, setCauHoi] = useState([]);
  const [dapAn, setDapAn] = useState({});
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);
  const [conLai, setConLai] = useState(null); // giây; null = không giới hạn
  const [trangThaiLuu, setTrangThaiLuu] = useState(""); // "" | "dang" | "da" | "loi"
  const [dangNop, setDangNop] = useState(false);
  const [hoiNop, setHoiNop] = useState(false);

  // Ref để đồng hồ và bộ tự lưu luôn đọc được dữ liệu mới nhất mà không phải
  // dựng lại interval / hàm mỗi lần người học bấm chọn.
  const dapAnRef = useRef(dapAn);
  const cauHoiRef = useRef(cauHoi);
  const mocKetThucRef = useRef(null); // mốc hết giờ theo đồng hồ trình duyệt (ms)
  const henLuuRef = useRef(null); // setTimeout của lần tự lưu sắp tới
  const luuDangChayRef = useRef(Promise.resolve()); // xếp hàng các lần lưu
  const daNopRef = useRef(false);
  dapAnRef.current = dapAn;
  cauHoiRef.current = cauHoi;

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    try {
      const res = await api("get", `/learn/quiz/attempt/${attemptId}`);
      const d = res.data;
      if (d.trangThai === "finished") {
        navigate(`/hoc/${courseId}/kiem-tra/${cmid}/ket-qua/${attemptId}`, { replace: true });
        return;
      }
      const ds = d.cauHoi.map((c) => ({ ...c, boc: bocCauHoi(c.html) }));
      const daChon = {};
      ds.forEach((c) => {
        const chon = c.boc?.luaChon.find((l) => l.dangChon);
        if (chon) daChon[c.slot] = chon.value;
      });
      setCauHoi(ds);
      setDapAn(daChon);
      daNopRef.current = false;

      if (d.hetGioLuc > 0) {
        // Giờ còn lại tính trên cùng một đồng hồ (của Moodle), rồi quy ra mốc
        // theo đồng hồ trình duyệt để đếm lùi — lệch giờ máy không ảnh hưởng.
        mocKetThucRef.current = Date.now() + (d.hetGioLuc - d.gioMayChu) * 1000;
        setConLai(d.hetGioLuc - d.gioMayChu);
      } else {
        mocKetThucRef.current = null;
        setConLai(null);
      }
    } catch (err) {
      setLoi(err?.response?.data?.error || "Không tải được bài làm. Vui lòng thử lại.");
    } finally {
      setDangTai(false);
    }
  }, [attemptId, courseId, cmid, navigate]);

  useEffect(() => {
    tai();
  }, [tai]);

  /**
   * Tự lưu nháp. Moodle tăng sequencecheck của câu vừa đổi đáp án, nên nhận số
   * mới về và ghi đè — gửi số cũ ở lần sau là bị từ chối.
   * Các lần lưu xếp hàng nối tiếp nhau để không lần nào mang số đã cũ.
   */
  const luuNhap = useCallback(() => {
    luuDangChayRef.current = luuDangChayRef.current.then(async () => {
      if (daNopRef.current) return;
      setTrangThaiLuu("dang");
      try {
        const res = await api("put", `/learn/quiz/attempt/${attemptId}`, {
          data: goiDapAn(cauHoiRef.current, dapAnRef.current),
        });
        const moi = res.data?.sequencecheck || {};
        const ds = cauHoiRef.current.map((c) =>
          c.boc && moi[c.slot] !== undefined ? { ...c, boc: { ...c.boc, sequence: moi[c.slot] } } : c,
        );
        cauHoiRef.current = ds;
        setCauHoi(ds);
        setTrangThaiLuu("da");
      } catch {
        setTrangThaiLuu("loi");
      }
    });
    return luuDangChayRef.current;
  }, [attemptId]);

  const nop = useCallback(
    async (hetGio = false) => {
      if (daNopRef.current) return;
      clearTimeout(henLuuRef.current);
      setDangNop(true);
      setHoiNop(false);
      // Chờ lần lưu đang chạy (nếu có) xong để nộp bằng sequencecheck mới nhất.
      await luuDangChayRef.current;
      daNopRef.current = true;
      try {
        await api("post", `/learn/quiz/attempt/${attemptId}/submit`, {
          data: goiDapAn(cauHoiRef.current, dapAnRef.current),
          hetGio,
        });
        navigate(`/hoc/${courseId}/kiem-tra/${cmid}/ket-qua/${attemptId}`, { replace: true });
      } catch (err) {
        // Lượt đã bị Moodle đóng (hết giờ phía máy chủ, hoặc đã nộp ở tab khác):
        // bài đã được chấm, cứ sang trang kết quả.
        if (["attemptalreadyclosed", "notinprogress"].includes(err?.response?.data?.ma)) {
          navigate(`/hoc/${courseId}/kiem-tra/${cmid}/ket-qua/${attemptId}`, { replace: true });
          return;
        }
        daNopRef.current = false;
        setDangNop(false);
        setLoi(err?.response?.data?.error || "Chưa nộp được bài. Vui lòng thử lại.");
      }
    },
    [attemptId, courseId, cmid, navigate],
  );

  // Đồng hồ đếm lùi; về 0 thì tự nộp.
  useEffect(() => {
    if (dangTai || mocKetThucRef.current === null) return undefined;
    const id = setInterval(() => {
      const giay = Math.round((mocKetThucRef.current - Date.now()) / 1000);
      setConLai(giay);
      if (giay <= 0) {
        clearInterval(id);
        nop(true);
      }
    }, 1000);
    return () => clearInterval(id);
  }, [dangTai, nop]);

  const chon = (slot, value) => {
    setDapAn((cu) => ({ ...cu, [slot]: value }));
    dapAnRef.current = { ...dapAnRef.current, [slot]: value };
    clearTimeout(henLuuRef.current);
    henLuuRef.current = setTimeout(luuNhap, TU_LUU_SAU_MS);
  };

  useEffect(() => () => clearTimeout(henLuuRef.current), []);

  const soDaLam = useMemo(() => cauHoi.filter((c) => dapAn[c.slot] !== undefined).length, [cauHoi, dapAn]);
  const coCauKhongHoTro = cauHoi.some((c) => !c.boc);

  const denCau = (slot) => {
    document.getElementById(`cau-${slot}`)?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  if (dangTai) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-9 w-9 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (loi && cauHoi.length === 0) {
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

  const sapHetGio = conLai !== null && conLai <= 60;

  return (
    <div className="min-h-screen bg-gray-50 pb-16">
      {/* Thanh trên cùng: đồng hồ, tiến độ, nút nộp */}
      <div className="sticky top-20 z-20 border-b border-gray-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <Link to={veTrangHoc} className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-primary">
            <ArrowLeft className="h-4 w-4" />
            Về trang học
          </Link>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-500">
              Đã làm <b className="text-gray-900">{soDaLam}</b>/{cauHoi.length}
            </span>
            {conLai !== null && (
              <span
                className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 font-mono text-sm font-bold ${
                  sapHetGio ? "animate-pulse bg-primary text-white" : "bg-gray-100 text-gray-800"
                }`}
              >
                <Clock className="h-4 w-4" />
                {doiDongHo(conLai)}
              </span>
            )}
            <button
              type="button"
              onClick={() => setHoiNop(true)}
              disabled={dangNop}
              className="inline-flex items-center gap-2 rounded-full bg-primary px-5 py-2 text-sm font-bold text-white hover:bg-primary-dark disabled:opacity-60"
            >
              <Send className="h-4 w-4" />
              {dangNop ? "Đang nộp..." : "Nộp bài"}
            </button>
          </div>
        </div>
      </div>

      <div className="mx-auto grid max-w-5xl grid-cols-1 gap-6 px-4 pt-6 lg:grid-cols-[1fr_220px]">
        {/* Danh sách câu hỏi */}
        <div className="space-y-4">
          {coCauKhongHoTro && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
              Bài này có dạng câu hỏi buni chưa hiển thị được. Vui lòng báo giáo viên để được
              hỗ trợ.
            </div>
          )}

          {cauHoi.map((c, i) => (
            <div key={c.slot} id={`cau-${c.slot}`} className="scroll-mt-44 rounded-xl border border-gray-200 bg-white p-5">
              <p className="mb-2 text-xs font-bold uppercase tracking-wider text-primary">
                Câu {c.soThuTu || i + 1}
              </p>
              {c.boc ? (
                <>
                  <div className="noi-dung-lms mb-4 text-gray-900" dangerouslySetInnerHTML={{ __html: c.boc.deBai }} />
                  <div className="space-y-2">
                    {c.boc.luaChon.map((l, j) => {
                      const dangChon = dapAn[c.slot] === l.value;
                      return (
                        <label
                          key={l.value}
                          className={`flex cursor-pointer items-start gap-3 rounded-lg border px-4 py-3 text-sm transition-colors ${
                            dangChon ? "border-primary bg-primary/5" : "border-gray-200 hover:border-gray-300 hover:bg-gray-50"
                          }`}
                        >
                          <input
                            type="radio"
                            name={`cau-${c.slot}`}
                            value={l.value}
                            checked={dangChon}
                            onChange={() => chon(c.slot, l.value)}
                            disabled={dangNop}
                            className="mt-0.5 accent-primary"
                          />
                          <span className="font-semibold text-gray-500">{String.fromCharCode(65 + j)}.</span>
                          <span className="flex-1 text-gray-800" dangerouslySetInnerHTML={{ __html: l.nhan }} />
                        </label>
                      );
                    })}
                  </div>
                </>
              ) : (
                <p className="text-sm text-gray-500">Dạng câu hỏi này chưa hỗ trợ trên buni.</p>
              )}
            </div>
          ))}
        </div>

        {/* Ô chọn câu nhanh */}
        <aside className="hidden lg:block">
          <div className="sticky top-44 rounded-xl border border-gray-200 bg-white p-4">
            <p className="mb-3 text-sm font-bold text-gray-900">Danh sách câu</p>
            <div className="grid grid-cols-5 gap-2">
              {cauHoi.map((c, i) => (
                <button
                  key={c.slot}
                  type="button"
                  onClick={() => denCau(c.slot)}
                  className={`h-9 rounded-md text-sm font-semibold ${
                    dapAn[c.slot] !== undefined ? "bg-primary text-white" : "border border-gray-300 text-gray-600 hover:border-primary"
                  }`}
                >
                  {c.soThuTu || i + 1}
                </button>
              ))}
            </div>
            <p className="mt-4 text-xs text-gray-400">
              {trangThaiLuu === "dang" && "Đang lưu..."}
              {trangThaiLuu === "da" && "Đã tự lưu bài làm"}
              {trangThaiLuu === "loi" && <span className="text-primary">Chưa lưu được — bài vẫn nộp bình thường</span>}
            </p>
          </div>
        </aside>
      </div>

      {loi && (
        <p className="mx-auto mt-4 max-w-5xl px-4 text-center text-sm text-primary">{loi}</p>
      )}

      {/* Hộp xác nhận nộp */}
      {hoiNop && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 text-center shadow-xl">
            <p className="mb-2 font-heading text-lg font-bold text-gray-900">Nộp bài?</p>
            <p className="mb-5 text-sm text-gray-500">
              {soDaLam < cauHoi.length
                ? `Bạn còn ${cauHoi.length - soDaLam} câu chưa trả lời. Nộp rồi thì không sửa được nữa.`
                : "Nộp rồi thì không sửa được nữa."}
            </p>
            <div className="flex justify-center gap-3">
              <button
                type="button"
                onClick={() => setHoiNop(false)}
                className="rounded-full border border-gray-300 px-5 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
              >
                Làm tiếp
              </button>
              <button
                type="button"
                onClick={() => nop(false)}
                className="rounded-full bg-primary px-5 py-2 text-sm font-bold text-white hover:bg-primary-dark"
              >
                Nộp bài
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
