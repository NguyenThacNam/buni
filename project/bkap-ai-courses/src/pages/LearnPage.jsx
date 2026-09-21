import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  CalendarCheck,
  Check,
  CheckCircle2,
  Circle,
  ClipboardList,
  ExternalLink,
  FileText,
  Lock,
  MessageSquare,
  PlayCircle,
  Video,
} from "lucide-react";

import { api } from "../api/Api";
import { useAuth } from "../context/AuthContext";
import QuizGioiThieu from "../components/quiz/QuizGioiThieu";
import XemPdf from "../components/learn/XemPdf";
import BangDiemDanh from "../components/learn/BangDiemDanh";
import BangTinKhoa from "../components/learn/BangTinKhoa";
import KhungLms from "../components/learn/KhungLms";
import { API_BASE_URL, CONTACT_INFO } from "../data/constants";

/**
 * Trang học trên buni.
 *
 * Nội dung lấy từ LMS qua backend chứ không gọi thẳng Moodle — token dịch vụ
 * mở được toàn bộ API nên phải giữ ở phía máy chủ.
 *
 * Phân vai giữa hai hệ thống: buni lo toàn bộ phần học viên nhìn thấy; Moodle
 * lo chấm điểm, lưu lượt làm, ghi tiến độ qua API.
 *
 * Học viên KHÔNG bị đẩy sang LMS (chốt 21/09/2026). Loại nội dung buni chưa dựng
 * lại được (SCORM, H5P) thì nhúng trình phát của Moodle ngay trong trang; loại
 * chưa hỗ trợ thì báo để học viên hỏi giáo viên, chứ không đưa link sang LMS.
 */

/**
 * Đường dẫn file backend trả về có dạng "/api/v1/learn/file?t=..." — đúng khi
 * frontend và backend cùng origin (bản build nhúng trong Spring Boot). Lúc chạy
 * dev thì frontend ở cổng 3001 còn backend ở 8080, nên phải ghép thêm origin.
 */
const GOC_API = API_BASE_URL.replace(/\/api\/v1\/?$/, "");
const duongDanDayDu = (duongDan) => (duongDan ? GOC_API + duongDan : null);

/**
 * Làm việc tương tự cho các link nằm bên trong HTML của bài đọc.
 *
 * Backend ký sẵn link file trong nội dung bài học dưới dạng đường dẫn tương
 * đối — đúng với bản chạy thật, nơi trang và API cùng origin. Nhưng lúc chạy
 * dev thì trình duyệt hiểu đường dẫn đó là của cổng 3001, trúng dev server
 * React chứ không phải backend, nên ảnh và video trong bài đọc đứng im.
 *
 * GOC_API rỗng khi chạy thật, nên hàm này lúc đó không đổi gì cả.
 */
const ganGocVaoHtml = (html) => {
  if (!html || !GOC_API) return html;
  return html.split('"/api/v1/learn/file').join(`"${GOC_API}/api/v1/learn/file`);
};

const doiKichThuoc = (soByte) => {
  if (!soByte) return "";
  const mb = soByte / (1024 * 1024);
  return mb >= 1 ? `${mb.toFixed(1)} MB` : `${Math.round(soByte / 1024)} KB`;
};

const laVideo = (muc) => (muc.mimetype || "").startsWith("video/");
const laPdf = (muc) => (muc.mimetype || "") === "application/pdf";

/** Biểu tượng gợi ý loại nội dung, để nhìn danh sách là biết ngay xem hay làm bài. */
function BieuTuongMuc({ muc, className }) {
  if (muc.type === "resource") {
    if (laVideo(muc)) return <Video className={className} />;
    return <FileText className={className} />;
  }
  if (muc.type === "page") {
    // Trang có thể là video nhúng YouTube, video tải lên Moodle, hoặc bài đọc
    // thuần chữ — nhìn biểu tượng là biết ngay phải xem hay phải đọc.
    if (muc.youtubeId) return <PlayCircle className={className} />;
    if ((muc.html || "").includes("<video")) return <Video className={className} />;
    return <FileText className={className} />;
  }
  if (muc.type === "quiz") return <ClipboardList className={className} />;
  if (muc.type === "forum") return <MessageSquare className={className} />;
  if (muc.type === "attendance") return <CalendarCheck className={className} />;
  return <ExternalLink className={className} />;
}

const NHAN_LOAI = {
  resource: "Tài liệu",
  page: "Bài học",
  quiz: "Bài kiểm tra",
  forum: "Bảng tin khóa học",
  scorm: "Bài giảng tương tác",
  h5pactivity: "Bài tương tác",
  attendance: "Điểm danh",
  assign: "Bài tập nộp",
};

// ─────────────────────────────────────────────────────────────
// KHUNG NỘI DUNG CHÍNH
// ─────────────────────────────────────────────────────────────

function NoiDungMuc({ muc, courseId, onDanhDau }) {
  const [dangDanhDau, setDangDanhDau] = useState(false);
  const [loiDanhDau, setLoiDanhDau] = useState(null);

  // Đổi sang mục khác thì xóa lỗi của mục cũ.
  useEffect(() => {
    setLoiDanhDau(null);
  }, [muc?.cmid]);

  const danhDau = async () => {
    setDangDanhDau(true);
    setLoiDanhDau(null);
    try {
      await onDanhDau(muc, !muc.daHoanThanh);
    } catch (err) {
      setLoiDanhDau(err?.response?.data?.error || "Chưa lưu được. Vui lòng thử lại.");
    } finally {
      setDangDanhDau(false);
    }
  };

  // Guard đặt SAU các hook: React yêu cầu mọi hook chạy đủ và đúng thứ tự ở
  // mỗi lần render. Để return sớm lên trên thì lúc chưa chọn mục nào component
  // chạy 0 hook, chọn rồi lại chạy 2 — React báo lỗi và dừng hẳn trang.
  if (!muc) {
    return (
      <div className="flex h-full min-h-[400px] flex-col items-center justify-center text-center text-gray-400">
        <PlayCircle className="mb-3 h-12 w-12" strokeWidth={1.2} />
        <p className="text-sm">Chọn một mục ở danh sách bên trái để bắt đầu học</p>
      </div>
    );
  }

  // Mục chưa đủ điều kiện mở (Hạn chế truy cập bên LMS): chỉ báo lý do.
  if (muc.khoa) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center px-4 text-center">
        <Lock className="mb-4 h-12 w-12 text-gray-300" strokeWidth={1.3} />
        <p className="mb-1 font-heading text-lg font-bold text-gray-900">{muc.name}</p>
        <p className="max-w-md text-sm text-gray-500">
          <span className="font-semibold text-gray-600">Điều kiện mở: </span>
          {muc.lyDoKhoa}
        </p>
      </div>
    );
  }

  const fileUrl = duongDanDayDu(muc.fileUrl);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">
            {NHAN_LOAI[muc.type] || "Nội dung"}
          </p>
          <h2 className="font-heading text-xl font-bold text-gray-900 md:text-2xl">
            {muc.name}
          </h2>
        </div>

        <div className="flex shrink-0 flex-wrap items-center gap-2">
          {/* Học liệu chỉ xem tại chỗ, không cho tải về máy. Nút tải đã bỏ; video
              cũng đã tắt mục tải trong trình phát bằng controlsList. */}
          {muc.filesize ? (
            <span className="rounded-full bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-500">
              {doiKichThuoc(muc.filesize)}
            </span>
          ) : null}

          {/* Mục cài "học viên tự đánh dấu" bên LMS: bấm để đánh dấu / bỏ đánh dấu. */}
          {muc.tuDanhDau && (
            <button
              type="button"
              onClick={danhDau}
              disabled={dangDanhDau}
              className={`inline-flex items-center gap-1.5 rounded-full border px-4 py-1.5 text-sm font-semibold transition-colors disabled:opacity-60 ${
                muc.daHoanThanh
                  ? "border-green-600 bg-green-50 text-green-700 hover:bg-green-100"
                  : "border-gray-300 text-gray-700 hover:border-primary hover:text-primary"
              }`}
            >
              {muc.daHoanThanh ? <CheckCircle2 className="h-4 w-4" /> : <Circle className="h-4 w-4" />}
              {dangDanhDau ? "Đang lưu..." : muc.daHoanThanh ? "Đã học xong" : "Đánh dấu đã học"}
            </button>
          )}

          {/* Mục tự động: chỉ báo trạng thái, không bấm được. */}
          {!muc.tuDanhDau && muc.daHoanThanh && (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-green-50 px-3 py-1.5 text-xs font-semibold text-green-700">
              <CheckCircle2 className="h-4 w-4" />
              Đã hoàn thành
            </span>
          )}
        </div>
      </div>
      {loiDanhDau && <p className="text-right text-sm text-primary">{loiDanhDau}</p>}

      {/* Video tải lên Moodle: phát thẳng trong trang. Backend chuyển tiếp
          header Range nên kéo thanh thời gian vẫn nhảy đúng. */}
      {muc.type === "resource" && laVideo(muc) && (
        <video
          key={fileUrl}
          src={fileUrl}
          controls
          controlsList="nodownload"
          className="w-full rounded-xl bg-black shadow-lg"
        >
          Trình duyệt không phát được video này.
        </video>
      )}

      {/* PDF/slide: buni tự dựng từng trang bằng pdf.js. Dùng iframe thì trình
          xem PDF của trình duyệt luôn kèm nút Tải về và In, không tắt được. */}
      {muc.type === "resource" && laPdf(muc) && (
        <XemPdf key={fileUrl} url={fileUrl} ten={muc.filename || muc.name} />
      )}

      {/* Định dạng còn lại (docx, xlsx, zip...): trình duyệt không xem trực tiếp
          được, chỉ đưa nút tải. */}
      {muc.type === "resource" && !laVideo(muc) && !laPdf(muc) && (
        <div className="flex items-center gap-4 rounded-xl border border-gray-200 bg-gray-50 p-6">
          <FileText className="h-10 w-10 shrink-0 text-primary" strokeWidth={1.4} />
          <div className="min-w-0">
            <p className="truncate font-semibold text-gray-900">{muc.filename}</p>
            {/* Link "Mở trên LMS" đã bỏ (21/09/2026): học viên chỉ học trên buni.
                Định dạng này trình duyệt không mở sẵn được, nên nhờ giáo viên đăng
                lại dạng PDF hoặc video. */}
            <p className="mt-0.5 text-sm text-gray-500">
              Trình duyệt không mở được định dạng này. Vui lòng báo giáo viên đăng lại
              dưới dạng PDF hoặc video.
            </p>
          </div>
        </div>
      )}

      {/* Bài giảng nhúng YouTube. */}
      {muc.type === "page" && muc.youtubeId && (
        <div className="aspect-video w-full overflow-hidden rounded-xl bg-black shadow-lg">
          <iframe
            key={muc.youtubeId}
            src={`https://www.youtube.com/embed/${muc.youtubeId}?rel=0`}
            title={muc.name}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
            className="h-full w-full border-0"
          />
        </div>
      )}

      {/* Bài đọc soạn bằng trình soạn thảo của Moodle. Nếu trang chỉ chứa mỗi
          video thì phần chữ thường rỗng — không hiện khung trống làm gì. */}
      {muc.type === "page" && muc.html && muc.html.trim() && (
        <div
          className="noi-dung-lms rounded-xl border border-gray-200 bg-white p-6"
          dangerouslySetInnerHTML={{ __html: ganGocVaoHtml(muc.html) }}
        />
      )}

      {/* Bài kiểm tra: làm ngay trên buni, điểm do LMS chấm. */}
      {/* Điểm danh: bảng chuyên cần của chính học viên, lấy từ LMS. */}
      {muc.type === "attendance" && <BangDiemDanh courseId={courseId} cmid={muc.cmid} />}

      {/* Bảng tin khóa học: đọc bài đăng của giáo viên ngay trên buni. */}
      {muc.type === "forum" && (
        <BangTinKhoa courseId={courseId} muc={muc} ganGocVaoHtml={ganGocVaoHtml} />
      )}

      {/* Bài giảng SCORM, bài tương tác H5P: nhúng trình phát của Moodle. */}
      {muc.nhungLms && <KhungLms courseId={courseId} muc={muc} />}

      {muc.type === "quiz" && <QuizGioiThieu courseId={courseId} muc={muc} />}

      {/* Loại nội dung buni chưa dựng lại được (bài tập nộp, wiki...). Trước đây
          chỗ này có nút "Mở trên LMS"; đã bỏ (21/09/2026) vì trung tâm chốt học
          viên chỉ học trên buni. */}
      {muc.moodleUrl && !["quiz", "forum"].includes(muc.type) && !muc.nhungLms && (
        <div className="rounded-xl border border-gray-200 bg-gray-50 p-6 text-center">
          <BieuTuongMuc muc={muc} className="mx-auto mb-3 h-10 w-10 text-primary" />
          <p className="mb-1 font-semibold text-gray-900">
            Nội dung này chưa hiển thị được trên buni
          </p>
          <p className="text-sm text-gray-500">
            Vui lòng liên hệ giáo viên phụ trách lớp để được hướng dẫn.
          </p>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// TRANG
// ─────────────────────────────────────────────────────────────

export default function LearnPage() {
  const { courseId } = useParams();
  // ?muc=<cmid>: quay về từ trang làm bài / kết quả thì mở lại đúng bài đó.
  const [thamSoUrl] = useSearchParams();
  const mucTrenUrl = Number(thamSoUrl.get("muc")) || null;

  const [duLieu, setDuLieu] = useState(null);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);
  // 403 = chưa được ghi danh. Thử lại cũng vô ích, phải liên hệ trung tâm.
  const [biChan, setBiChan] = useState(false);
  // Phiên đăng nhập cũ không mang token LMS — phải đăng nhập lại.
  const [canDangNhapLai, setCanDangNhapLai] = useState(false);
  const { logout, openLogin } = useAuth();
  const [cmidDangChon, setCmidDangChon] = useState(null);

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    setBiChan(false);
    setCanDangNhapLai(false);
    try {
      const res = await api("get", `/learn/${courseId}`);
      setDuLieu(res.data);

      // Mở mục ghi trên URL nếu có, không thì tự mở mục đầu tiên để người học
      // không phải bấm thêm một nhịp.
      const coMucTrenUrl = (res.data?.sections || []).some((c) =>
        c.modules.some((m) => m.cmid === mucTrenUrl),
      );
      const mucDau = (res.data?.sections || [])
        .flatMap((c) => c.modules)
        .find((m) => !m.khoa);
      setCmidDangChon(coMucTrenUrl ? mucTrenUrl : mucDau ? mucDau.cmid : null);
    } catch (err) {
      const laDangNhapLai = err?.response?.data?.ma === "CAN_DANG_NHAP_LAI";
      setCanDangNhapLai(laDangNhapLai);
      setBiChan(err?.response?.status === 403 && !laDangNhapLai);
      setLoi(
        err?.response?.data?.error ||
          "Không tải được nội dung khóa học. Vui lòng thử lại.",
      );
    } finally {
      setDangTai(false);
    }
  }, [courseId, mucTrenUrl]);

  useEffect(() => {
    tai();
  }, [tai]);

  const mucDangChon = useMemo(() => {
    if (!duLieu || cmidDangChon == null) return null;
    for (const chuong of duLieu.sections) {
      const tim = chuong.modules.find((m) => m.cmid === cmidDangChon);
      if (tim) return tim;
    }
    return null;
  }, [duLieu, cmidDangChon]);

  /** Ghi trạng thái hoàn thành mới (cmid -> {daHoanThanh, daXem}) vào mục lục. */
  const apDungHoanThanh = useCallback((hoanThanh) => {
    if (!hoanThanh) return;
    // Bản đồ trả về có đủ mọi mục theo dõi của khóa, kể cả mục trong chương khóa.
    const ds = Object.values(hoanThanh);
    const tienDoMoi =
      ds.length && ds.every((t) => t.daHoanThanh !== undefined)
        ? { xong: ds.filter((t) => t.daHoanThanh).length, tong: ds.length }
        : null;
    setDuLieu((cu) =>
      cu && {
        ...cu,
        tienDo: tienDoMoi || cu.tienDo,
        sections: cu.sections.map((c) => ({
          ...c,
          modules: c.modules.map((m) => (hoanThanh[m.cmid] ? { ...m, ...hoanThanh[m.cmid] } : m)),
        })),
      },
    );
  }, []);

  /**
   * Mở một mục có điều kiện "phải xem" thì báo LMS là đã xem.
   *
   * Học viên học trên buni nên Moodle không tự biết họ đã mở tài liệu. Mỗi mục
   * chỉ báo một lần trong phiên trang; lỗi thì bỏ qua, không làm phiền người học.
   */
  const daBaoXem = useRef(new Set());
  useEffect(() => {
    const muc = mucDangChon;
    if (!muc || muc.khoa || !muc.canXem || muc.daXem || daBaoXem.current.has(muc.cmid)) return;
    daBaoXem.current.add(muc.cmid);
    api("post", `/learn/${courseId}/modules/${muc.cmid}/viewed`)
      .then((res) => apDungHoanThanh(res.data?.hoanThanh))
      .catch(() => {});
  }, [mucDangChon, courseId, apDungHoanThanh]);

  const danhDauThuCong = useCallback(
    async (muc, daXong) => {
      const res = await api("put", `/learn/${courseId}/modules/${muc.cmid}/completion`, {
        completed: daXong,
      });
      apDungHoanThanh(res.data?.hoanThanh || { [muc.cmid]: { daHoanThanh: daXong } });
    },
    [courseId, apDungHoanThanh],
  );

  // Backend đếm trên toàn khóa (cả bài trong chương đang khóa). Không có thì mới
  // tự đếm các mục đang hiện.
  const tienDo = useMemo(() => {
    if (duLieu?.tienDo) return duLieu.tienDo;
    const ds = (duLieu?.sections || []).flatMap((c) => c.modules).filter((m) => m.daHoanThanh !== undefined);
    return ds.length ? { xong: ds.filter((m) => m.daHoanThanh).length, tong: ds.length } : null;
  }, [duLieu]);

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
          {canDangNhapLai ? (
            <button
              type="button"
              onClick={async () => {
                await logout();
                openLogin();
              }}
              className="rounded-full bg-primary px-6 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark"
            >
              Đăng nhập lại
            </button>
          ) : biChan ? (
            <a
              href={`tel:${CONTACT_INFO.phone.replace(/\./g, "")}`}
              className="rounded-full bg-primary px-6 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark"
            >
              Gọi {CONTACT_INFO.phone}
            </a>
          ) : (
            <button
              type="button"
              onClick={tai}
              className="rounded-full bg-primary px-6 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark"
            >
              Thử lại
            </button>
          )}
          <Link
            to={`/courses/${courseId}`}
            className="rounded-full border border-gray-300 px-6 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50"
          >
            Về trang khóa học
          </Link>
        </div>
      </div>
    );
  }

  const coNoiDung = duLieu?.sections?.length > 0;

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 md:px-6 lg:px-8">
      <Link
        to={`/courses/${courseId}`}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 transition-colors hover:text-primary"
      >
        <ArrowLeft className="h-4 w-4" />
        Quay lại khóa học
      </Link>

      <h1 className="mb-2 font-heading text-2xl font-extrabold text-gray-900 md:text-3xl">
        {duLieu.title}
      </h1>

      {/* Tiến độ do LMS tính. Khóa chưa bật theo dõi hoàn thành thì không hiện. */}
      {tienDo ? (
        <div className="mb-6 flex max-w-md items-center gap-3">
          <div className="h-2 flex-1 overflow-hidden rounded-full bg-gray-200">
            <div
              className="h-full rounded-full bg-green-600 transition-all"
              style={{ width: `${Math.round((tienDo.xong * 100) / tienDo.tong)}%` }}
            />
          </div>
          <span className="shrink-0 text-sm text-gray-500">
            Đã hoàn thành <b className="text-gray-900">{tienDo.xong}</b>/{tienDo.tong}
          </span>
        </div>
      ) : (
        <div className="mb-6" />
      )}

      {!coNoiDung ? (
        <div className="rounded-xl border border-gray-200 bg-gray-50 p-10 text-center text-gray-500">
          Khóa học chưa có bài giảng nào. Nội dung sẽ được cập nhật sớm.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-[320px_1fr]">
          {/* Mục lục */}
          <aside className="lg:max-h-[80vh] lg:overflow-y-auto lg:pr-1">
            <div className="space-y-5">
              {duLieu.sections.map((chuong, i) => (
                <div key={i}>
                  <p
                    className={`mb-2 px-1 font-heading text-sm font-bold ${
                      chuong.khoa ? "text-gray-400" : "text-gray-900"
                    }`}
                  >
                    {chuong.name}
                  </p>

                  {/* Chương bị khóa: Moodle không cho biết bên trong có bài gì, chỉ
                      báo điều kiện để mở. */}
                  {chuong.khoa && (
                    <div className="mx-1 mb-2 flex items-start gap-2 rounded-lg border border-dashed border-gray-200 bg-gray-50/70 px-3 py-2 text-xs leading-relaxed text-gray-500">
                      <Lock className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gray-400" />
                      <span>
                        <span className="font-semibold text-gray-600">Điều kiện mở: </span>
                        {chuong.lyDoKhoa}
                      </span>
                    </div>
                  )}
                  <ul className="space-y-1">
                    {chuong.modules.map((muc) => {
                      const dangChon = muc.cmid === cmidDangChon;
                      return (
                        <li key={muc.cmid}>
                          <button
                            type="button"
                            onClick={() => setCmidDangChon(muc.cmid)}
                            className={`flex w-full items-start gap-2.5 rounded-lg px-3 py-2.5 text-left text-sm transition-colors ${
                              dangChon
                                ? "bg-primary/10 font-semibold text-primary"
                                : "text-gray-600 hover:bg-gray-100"
                            }`}
                          >
                            <BieuTuongMuc
                              muc={muc}
                              className="mt-0.5 h-4 w-4 shrink-0"
                            />
                            <span className={`min-w-0 flex-1 ${muc.khoa ? "text-gray-400" : ""}`}>
                              {muc.name}
                            </span>

                            {/* Chưa đủ điều kiện mở (Hạn chế truy cập bên LMS). */}
                            {muc.khoa && (
                              <Lock className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
                            )}

                            {/* Cờ hoàn thành do Moodle ghi nhận. Khóa nào chưa bật
                                theo dõi hoàn thành thì không có trường này. */}
                            {muc.daHoanThanh && (
                              <Check
                                className="mt-0.5 h-4 w-4 shrink-0 text-green-600"
                                strokeWidth={3}
                              />
                            )}
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              ))}
            </div>
          </aside>

          {/* Khung học */}
          {/* min-w-0: ô lưới mặc định rộng theo nội dung bên trong. Trang PDF
              phóng to 250% mà thiếu dòng này thì kéo cả cột phình ra ngoài trang. */}
          <main className="min-w-0 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm md:p-7">
            <NoiDungMuc muc={mucDangChon} courseId={courseId} onDanhDau={danhDauThuCong} />
          </main>
        </div>
      )}
    </div>
  );
}
