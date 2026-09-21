import React, { useCallback, useEffect, useState } from "react";
import { AlertCircle, ChevronDown, ChevronUp, MessageSquare } from "lucide-react";

import { api } from "../../api/Api";

/**
 * Bảng tin của khóa — mục "Các thông báo" bên LMS.
 *
 * buni cho ĐỌC ngay tại chỗ: bấm một bài là mở nội dung và các trả lời.
 *
 * Không có lối sang LMS (bỏ 21/09/2026): trung tâm chốt học viên chỉ ở trên
 * buni. Viết bài và trả lời là việc của giáo viên, làm bên Moodle.
 */

const doiThoiGian = (giay) => {
  const d = new Date(giay * 1000);
  return `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
};

export default function BangTinKhoa({ courseId, muc, ganGocVaoHtml }) {
  const [ds, setDs] = useState([]);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);
  const [dangMo, setDangMo] = useState(null); // id chủ đề đang mở
  const [traLoi, setTraLoi] = useState({}); // id chủ đề -> danh sách bài

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    try {
      const res = await api("get", `/learn/${courseId}/forum/${muc.cmid}`);
      setDs(res.data.baiDang || []);
    } catch (err) {
      setLoi(err?.response?.data?.error || "Chưa tải được thông báo của khóa.");
    } finally {
      setDangTai(false);
    }
  }, [courseId, muc.cmid]);

  useEffect(() => {
    tai();
  }, [tai]);

  const moChuDe = async (bai) => {
    const dong = dangMo === bai.id;
    setDangMo(dong ? null : bai.id);
    if (dong || traLoi[bai.id]) return;

    // Bài mở đầu đã có sẵn nội dung; chỉ cần gọi thêm khi chủ đề có trả lời.
    if (!bai.soTraLoi) return;
    try {
      const res = await api(
        "get",
        `/learn/${courseId}/forum/${muc.cmid}/discussions/${bai.id}`,
      );
      setTraLoi((cu) => ({ ...cu, [bai.id]: res.data.bai || [] }));
    } catch {
      setTraLoi((cu) => ({ ...cu, [bai.id]: [] }));
    }
  };

  if (dangTai) {
    return <div className="h-40 animate-pulse rounded-xl bg-gray-100" />;
  }

  if (loi) {
    return (
      <div className="rounded-xl border border-gray-200 bg-gray-50 p-6 text-center">
        <AlertCircle className="mx-auto mb-3 h-10 w-10 text-primary" strokeWidth={1.4} />
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

  return (
    <div className="space-y-4">
      {ds.length === 0 ? (
        <div className="rounded-xl border border-gray-200 bg-gray-50 p-8 text-center">
          <MessageSquare className="mx-auto mb-3 h-10 w-10 text-gray-300" strokeWidth={1.4} />
          <p className="text-sm text-gray-500">Chưa có thông báo nào từ giáo viên.</p>
        </div>
      ) : (
        <ul className="divide-y divide-gray-100 overflow-hidden rounded-xl border border-gray-200">
          {ds.map((bai) => (
            <li key={bai.id}>
              <button
                type="button"
                onClick={() => moChuDe(bai)}
                className="flex w-full items-start justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50"
              >
                <span className="min-w-0">
                  <span className="block font-semibold text-gray-900">{bai.tieuDe}</span>
                  <span className="mt-0.5 block text-xs text-gray-500">
                    {bai.nguoiDang} · {doiThoiGian(bai.thoiGian)}
                    {bai.soTraLoi > 0 && ` · ${bai.soTraLoi} trả lời`}
                  </span>
                </span>
                {dangMo === bai.id ? (
                  <ChevronUp className="mt-1 h-4 w-4 shrink-0 text-gray-400" />
                ) : (
                  <ChevronDown className="mt-1 h-4 w-4 shrink-0 text-gray-400" />
                )}
              </button>

              {dangMo === bai.id && (
                <div className="space-y-4 border-t border-gray-100 bg-gray-50/60 px-4 py-4">
                  <div
                    className="noi-dung-lms text-sm text-gray-700"
                    dangerouslySetInnerHTML={{ __html: ganGocVaoHtml(bai.noiDung) }}
                  />

                  {(traLoi[bai.id] || [])
                    .filter((p) => p.laTraLoi)
                    .map((p) => (
                      <div key={p.id} className="rounded-lg border border-gray-200 bg-white p-3">
                        <p className="mb-1 text-xs font-semibold text-gray-600">
                          {p.nguoiDang} · {doiThoiGian(p.thoiGian)}
                        </p>
                        <div
                          className="noi-dung-lms text-sm text-gray-700"
                          dangerouslySetInnerHTML={{ __html: ganGocVaoHtml(p.noiDung) }}
                        />
                      </div>
                    ))}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

    </div>
  );
}
