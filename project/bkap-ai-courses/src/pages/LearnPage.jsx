import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  ClipboardList,
  ExternalLink,
  FileText,
  MessageSquare,
  PlayCircle,
  Video,
} from "lucide-react";

import { api } from "../api/Api";
import { API_BASE_URL, CONTACT_INFO, LMS_URL } from "../data/constants";

/**
 * Trang học trên buni.
 *
 * Nội dung lấy từ LMS qua backend chứ không gọi thẳng Moodle — token dịch vụ
 * mở được toàn bộ API nên phải giữ ở phía máy chủ.
 *
 * Phân vai giữa hai hệ thống: buni lo phần đọc/xem (PDF, tài liệu, video),
 * còn bài kiểm tra và diễn đàn vẫn để Moodle làm, vì chấm điểm và lưu lượt
 * làm bài nằm bên đó.
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
  return <ExternalLink className={className} />;
}

const NHAN_LOAI = {
  resource: "Tài liệu",
  page: "Bài học",
  quiz: "Bài kiểm tra",
  forum: "Diễn đàn",
  assign: "Bài tập nộp",
};

// ─────────────────────────────────────────────────────────────
// KHUNG NỘI DUNG CHÍNH
// ─────────────────────────────────────────────────────────────

function NoiDungMuc({ muc, courseId }) {
  const [dangMoLms, setDangMoLms] = useState(false);
  const [loiLms, setLoiLms] = useState(null);

  /**
   * Xin đường dẫn đăng nhập một lần rồi mở sang LMS.
   *
   * Không dùng thẻ <a> trỏ thẳng sang Moodle nữa: người học chưa có phiên đăng
   * nhập bên đó sẽ rơi vào màn hình đăng nhập, còn nếu máy đang đăng nhập bằng
   * tài khoản khác thì bài làm ghi sang tên người khác.
   */
  const moTrenLms = async () => {
    if (!muc) return;
    setDangMoLms(true);
    setLoiLms(null);

    // Mở tab TRƯỚC khi gọi mạng. Gọi xong mới mở thì trình duyệt coi đó là
    // cửa sổ tự bật và chặn, vì đã rời khỏi nhịp bấm chuột của người dùng.
    const tab = window.open("", "_blank");
    try {
      const res = await api("post", "/learn/lms-url", {
        courseId: String(courseId),
        cmid: String(muc.cmid),
        loai: muc.type,
      });
      if (tab) tab.location.href = res.data.loginUrl;
      else window.location.href = res.data.loginUrl;
    } catch (err) {
      if (tab) tab.close();
      setLoiLms(
        err?.response?.data?.error ||
          "Chưa mở được nội dung trên LMS. Vui lòng thử lại.",
      );
    } finally {
      setDangMoLms(false);
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

        {/* Học liệu chỉ xem tại chỗ, không cho tải về máy. Nút tải đã bỏ; video
            cũng đã tắt mục tải trong trình phát bằng controlsList. */}
        {muc.filesize ? (
          <span className="shrink-0 rounded-full bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-500">
            {doiKichThuoc(muc.filesize)}
          </span>
        ) : null}
      </div>

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

      {/* PDF/slide: nhúng để đọc ngay, khỏi phải tải rồi mở bằng phần mềm khác. */}
      {muc.type === "resource" && laPdf(muc) && (
        <iframe
          key={fileUrl}
          src={fileUrl}
          title={muc.name}
          className="h-[75vh] w-full rounded-xl border border-gray-200 bg-gray-50"
        />
      )}

      {/* Định dạng còn lại (docx, xlsx, zip...): trình duyệt không xem trực tiếp
          được, chỉ đưa nút tải. */}
      {muc.type === "resource" && !laVideo(muc) && !laPdf(muc) && (
        <div className="flex items-center gap-4 rounded-xl border border-gray-200 bg-gray-50 p-6">
          <FileText className="h-10 w-10 shrink-0 text-primary" strokeWidth={1.4} />
          <div className="min-w-0">
            <p className="truncate font-semibold text-gray-900">{muc.filename}</p>
            <p className="mt-0.5 text-sm text-gray-500">
              Trình duyệt không mở được định dạng này.{" "}
              <a
                href={`${LMS_URL}/mod/resource/view.php?id=${muc.cmid}`}
                target="_blank"
                rel="noreferrer"
                className="font-medium text-primary underline"
              >
                Mở trên LMS
              </a>
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

      {/* Kiểm tra, diễn đàn, bài nộp: mở sang LMS. Điểm và lượt làm bài do
          Moodle quản lý, làm lại ở buni sẽ lệch dữ liệu. */}
      {muc.moodleUrl && (
        <div className="rounded-xl border border-gray-200 bg-gray-50 p-6 text-center">
          <BieuTuongMuc
            muc={muc}
            className="mx-auto mb-3 h-10 w-10 text-primary"
          />
          <p className="mb-1 font-semibold text-gray-900">
            {muc.type === "quiz"
              ? "Bài kiểm tra được thực hiện trên hệ thống LMS"
              : "Nội dung này nằm trên hệ thống LMS"}
          </p>
          <p className="mb-5 text-sm text-gray-500">
            Bấm mở là vào thẳng bằng chính tài khoản của bạn, không phải
            đăng nhập lại.
          </p>
          <button
            type="button"
            onClick={moTrenLms}
            disabled={dangMoLms}
            className="inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-sm font-bold uppercase tracking-wider text-white shadow-md transition-colors hover:bg-primary-dark disabled:opacity-60"
          >
            {dangMoLms ? "Đang mở..." : "Mở trên LMS"}
            <ExternalLink className="h-4 w-4" />
          </button>

          {loiLms && <p className="mt-3 text-sm text-primary">{loiLms}</p>}
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

  const [duLieu, setDuLieu] = useState(null);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);
  // 403 = chưa được ghi danh. Thử lại cũng vô ích, phải liên hệ trung tâm.
  const [biChan, setBiChan] = useState(false);
  const [cmidDangChon, setCmidDangChon] = useState(null);

  const tai = useCallback(async () => {
    setDangTai(true);
    setLoi(null);
    setBiChan(false);
    try {
      const res = await api("get", `/learn/${courseId}`);
      setDuLieu(res.data);

      // Tự mở mục đầu tiên để người học không phải bấm thêm một nhịp.
      const mucDau = res.data?.sections?.[0]?.modules?.[0];
      setCmidDangChon(mucDau ? mucDau.cmid : null);
    } catch (err) {
      setBiChan(err?.response?.status === 403);
      setLoi(
        err?.response?.data?.error ||
          "Không tải được nội dung khóa học. Vui lòng thử lại.",
      );
    } finally {
      setDangTai(false);
    }
  }, [courseId]);

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
          {biChan ? (
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

      <h1 className="mb-6 font-heading text-2xl font-extrabold text-gray-900 md:text-3xl">
        {duLieu.title}
      </h1>

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
                  <p className="mb-2 px-1 font-heading text-sm font-bold text-gray-900">
                    {chuong.name}
                  </p>
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
                            <span className="min-w-0 flex-1">{muc.name}</span>
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
          <main className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm md:p-7">
            <NoiDungMuc muc={mucDangChon} courseId={courseId} />
          </main>
        </div>
      )}
    </div>
  );
}
