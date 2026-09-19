import React, { useCallback, useEffect, useRef, useState } from "react";
import { Maximize2, Minimize2, Minus, Plus } from "lucide-react";
import * as pdfjs from "pdfjs-dist/legacy/build/pdf";

/**
 * Trình đọc PDF của riêng buni.
 *
 * Trước đây nhúng file bằng thẻ iframe, tức là để trình duyệt tự mở bằng trình
 * xem PDF có sẵn — trình xem đó luôn kèm nút Tải về và In, buni không tắt được.
 * Ở đây tự vẽ bằng pdf.js nên thanh công cụ là của mình: phóng to và toàn màn hình.
 *
 * Các trang xếp dọc và cuộn liên tục như đọc tài liệu bình thường, không bắt bấm
 * lật từng trang. Trang chỉ được vẽ khi sắp lọt vào tầm nhìn, và được xóa khỏi bộ
 * nhớ khi cuộn đi xa — tài liệu 60 trang mà vẽ hết một lượt thì rất nặng máy.
 *
 * Vẫn không chặn được tuyệt đối: trình duyệt phải tải file về bộ nhớ mới hiện
 * được, người rành máy tính vẫn lấy được qua công cụ nhà phát triển. Mục tiêu là
 * bỏ lối tải xuống hiển nhiên, không phải chống sao chép.
 */

// Tệp worker copy sẵn vào thư mục public (xem public/pdf.worker.min.js) — pdf.js
// tách phần dựng trang sang luồng riêng để không làm đơ giao diện.
pdfjs.GlobalWorkerOptions.workerSrc = `${process.env.PUBLIC_URL || ""}/pdf.worker.min.js`;

const PHONG_TO = [0.75, 1, 1.25, 1.5, 2, 2.5];
/** Vẽ trước/giữ lại các trang cách tầm nhìn chừng này. */
const LE_VE = "600px";
const LE_GIU = 1500;

export default function XemPdf({ url, ten }) {
  const boxRef = useRef(null); // khung ngoài, dùng cho toàn màn hình
  const khungRef = useRef(null); // vùng cuộn
  const trangRef = useRef([]); // các thẻ <div> bọc từng trang
  const taiLieuRef = useRef(null);
  const dangVeRef = useRef(new Map()); // số trang -> việc vẽ đang chạy
  const daVeRef = useRef(new Set());

  const [soTrang, setSoTrang] = useState(0);
  const [trangHienTai, setTrangHienTai] = useState(1);
  const [mucPhong, setMucPhong] = useState(1);
  const [tyLeTrang, setTyLeTrang] = useState(1.414); // cao/rộng, để dựng chỗ trống
  const [toanManHinh, setToanManHinh] = useState(false);
  const [dangTai, setDangTai] = useState(true);
  const [loi, setLoi] = useState(null);

  // ── Tải tài liệu ────────────────────────────────────────────
  useEffect(() => {
    let huy = false;
    setDangTai(true);
    setLoi(null);
    setSoTrang(0);
    setTrangHienTai(1);
    daVeRef.current = new Set();

    const viec = pdfjs.getDocument({ url });
    viec.promise.then(
      async (tl) => {
        if (huy) {
          tl.destroy();
          return;
        }
        taiLieuRef.current = tl;
        // Kích thước trang đầu dùng cho mọi chỗ trống, để thanh cuộn đúng ngay
        // từ đầu thay vì giật lên giật xuống khi từng trang vẽ xong.
        const p1 = await tl.getPage(1);
        const v = p1.getViewport({ scale: 1 });
        if (huy) return;
        setTyLeTrang(v.height / v.width);
        setSoTrang(tl.numPages);
        setDangTai(false);
      },
      (e) => {
        if (!huy) {
          console.error("[XemPdf]", e);
          setLoi("Không mở được tài liệu này. Vui lòng tải lại trang.");
          setDangTai(false);
        }
      },
    );

    return () => {
      huy = true;
      viec.destroy?.();
      taiLieuRef.current = null;
    };
  }, [url]);

  /** Bề ngang một trang theo khung hiện tại và mức phóng. */
  const beNgangTrang = useCallback(() => {
    const khung = khungRef.current?.clientWidth || 800;
    return Math.max(240, (khung - 32) * mucPhong);
  }, [mucPhong]);

  /**
   * Trang chưa vẽ vẫn phải chiếm đúng chỗ, nếu không thanh cuộn nhảy loạn mỗi
   * khi một trang vẽ xong.
   */
  const datChoTrong = useCallback(
    (so) => {
      const canvas = trangRef.current[so - 1]?.querySelector("canvas");
      if (!canvas || daVeRef.current.has(so)) return;
      const w = beNgangTrang();
      canvas.style.width = `${Math.floor(w)}px`;
      canvas.style.height = `${Math.floor(w * tyLeTrang)}px`;
    },
    [beNgangTrang, tyLeTrang],
  );

  // ── Vẽ / xóa một trang ──────────────────────────────────────
  const veTrang = useCallback(
    async (so) => {
      const tl = taiLieuRef.current;
      const boc = trangRef.current[so - 1];
      if (!tl || !boc || daVeRef.current.has(so) || dangVeRef.current.has(so)) {
        return;
      }
      daVeRef.current.add(so);

      const p = await tl.getPage(so);
      const goc = p.getViewport({ scale: 1 });
      const viewport = p.getViewport({ scale: beNgangTrang() / goc.width });

      // Nhân theo mật độ điểm ảnh của màn hình, không thì chữ bị rỗ trên màn Retina.
      const tyLe = Math.min(window.devicePixelRatio || 1, 2);
      const canvas = boc.querySelector("canvas");
      if (!canvas) return;
      canvas.width = Math.floor(viewport.width * tyLe);
      canvas.height = Math.floor(viewport.height * tyLe);
      canvas.style.width = `${Math.floor(viewport.width)}px`;
      canvas.style.height = `${Math.floor(viewport.height)}px`;

      const viec = p.render({
        canvasContext: canvas.getContext("2d"),
        viewport,
        transform: tyLe === 1 ? null : [tyLe, 0, 0, tyLe, 0, 0],
      });
      dangVeRef.current.set(so, viec);
      try {
        await viec.promise;
      } catch (e) {
        // Hủy giữa chừng là chuyện bình thường khi người đọc cuộn nhanh.
        daVeRef.current.delete(so);
      } finally {
        dangVeRef.current.delete(so);
      }
    },
    [beNgangTrang],
  );

  const xoaTrang = useCallback(
    (so) => {
      dangVeRef.current.get(so)?.cancel();
      dangVeRef.current.delete(so);
      daVeRef.current.delete(so);
      const canvas = trangRef.current[so - 1]?.querySelector("canvas");
      if (canvas) {
        // Đặt lại kích thước là cách duy nhất trả bộ nhớ canvas về cho trình duyệt.
        canvas.width = 0;
        canvas.height = 0;
        datChoTrong(so);
      }
    },
    [datChoTrong],
  );

  // Dựng chỗ trống cho mọi trang chưa vẽ.
  useEffect(() => {
    if (!soTrang || dangTai) return;
    for (let i = 1; i <= soTrang; i++) datChoTrong(i);
  }, [soTrang, dangTai, datChoTrong]);

  // ── Vẽ theo tầm nhìn ────────────────────────────────────────
  useEffect(() => {
    if (!soTrang || dangTai) return undefined;
    const khung = khungRef.current;

    const canVe = new IntersectionObserver(
      (cac) => {
        cac.forEach((e) => {
          const so = Number(e.target.dataset.trang);
          if (e.isIntersecting) veTrang(so);
        });
      },
      { root: khung, rootMargin: LE_VE },
    );

    const canXoa = new IntersectionObserver(
      (cac) => {
        cac.forEach((e) => {
          const so = Number(e.target.dataset.trang);
          if (!e.isIntersecting) xoaTrang(so);
        });
      },
      { root: khung, rootMargin: `${LE_GIU}px` },
    );

    // Trang nào chiếm phần lớn tầm nhìn thì coi là trang đang đọc.
    const demTrang = new IntersectionObserver(
      (cac) => {
        const thay = cac.filter((e) => e.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (thay) setTrangHienTai(Number(thay.target.dataset.trang));
      },
      { root: khung, threshold: [0.1, 0.5, 0.9] },
    );

    trangRef.current.slice(0, soTrang).forEach((el) => {
      if (!el) return;
      canVe.observe(el);
      canXoa.observe(el);
      demTrang.observe(el);
    });

    return () => {
      canVe.disconnect();
      canXoa.disconnect();
      demTrang.disconnect();
    };
  }, [soTrang, dangTai, veTrang, xoaTrang]);

  /** Vẽ lại trang đang hiện và đặt lại chỗ trống cho trang chưa vẽ. */
  const veLaiTatCa = useCallback(() => {
    const dang = [...daVeRef.current];
    dang.forEach(xoaTrang);
    for (let i = 1; i <= soTrang; i++) datChoTrong(i);
    dang.forEach(veTrang);
  }, [veTrang, xoaTrang, datChoTrong, soTrang]);

  /**
   * Bám theo bề ngang thật của khung đọc.
   *
   * Vào/ra toàn màn hình hay đổi mức phóng thì phải vẽ lại theo bề ngang mới.
   * Đo ngay lúc React cập nhật thì vẫn còn là bề ngang cũ — trình duyệt chưa kịp
   * dựng lại bố cục sau khi thoát toàn màn hình, nên trang giữ nguyên cỡ to cho
   * tới khi có việc gì khác làm trang vẽ lại. ResizeObserver báo đúng lúc kích
   * thước đã đổi thật.
   */
  const beNgangCuRef = useRef(0);
  useEffect(() => {
    const khung = khungRef.current;
    if (!khung || dangTai || !soTrang) return undefined;

    let hen;
    const quanSat = new ResizeObserver(() => {
      const moi = khung.clientWidth;
      // Bỏ qua thay đổi vụn (thanh cuộn hiện/ẩn), tránh vẽ đi vẽ lại vô tận.
      if (Math.abs(moi - beNgangCuRef.current) < 4) return;
      beNgangCuRef.current = moi;
      clearTimeout(hen);
      hen = setTimeout(veLaiTatCa, 120);
    });
    quanSat.observe(khung);

    return () => {
      clearTimeout(hen);
      quanSat.disconnect();
    };
  }, [dangTai, soTrang, veLaiTatCa]);

  // Đổi mức phóng: khung không đổi kích thước nên phải tự vẽ lại.
  useEffect(() => {
    if (!dangTai && soTrang) veLaiTatCa();
    // Chỉ chạy khi đổi mức phóng, không chạy mỗi lần cuộn.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mucPhong]);

  // ── Toàn màn hình ───────────────────────────────────────────
  useEffect(() => {
    const doi = () => setToanManHinh(document.fullscreenElement === boxRef.current);
    document.addEventListener("fullscreenchange", doi);
    return () => document.removeEventListener("fullscreenchange", doi);
  }, []);

  const batTatToanManHinh = () => {
    if (document.fullscreenElement) {
      document.exitFullscreen?.();
    } else {
      boxRef.current?.requestFullscreen?.();
    }
  };

  const doiPhong = (buoc) => {
    const i = PHONG_TO.indexOf(mucPhong);
    setMucPhong(PHONG_TO[Math.min(Math.max(0, i + buoc), PHONG_TO.length - 1)]);
  };

  if (loi) {
    return (
      <div className="rounded-xl border border-gray-200 bg-gray-50 p-8 text-center text-sm text-gray-500">
        {loi}
      </div>
    );
  }

  return (
    <div
      ref={boxRef}
      className={`w-full min-w-0 max-w-full overflow-hidden border border-gray-200 bg-gray-100 ${
        toanManHinh ? "flex h-screen flex-col rounded-none" : "rounded-xl"
      }`}
    >
      {/* Thanh công cụ của buni: không có nút tải về hay in. */}
      <div className="flex shrink-0 flex-wrap items-center justify-between gap-2 border-b border-gray-200 bg-white px-3 py-2">
        <span className="min-w-[80px] text-sm tabular-nums text-gray-600">
          {dangTai ? "Đang mở…" : `Trang ${trangHienTai} / ${soTrang}`}
        </span>

        <p className="hidden min-w-0 flex-1 truncate px-2 text-center text-sm text-gray-400 sm:block">
          {ten}
        </p>

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => doiPhong(-1)}
            disabled={mucPhong === PHONG_TO[0]}
            aria-label="Thu nhỏ"
            className="rounded-md p-1.5 text-gray-600 hover:bg-gray-100 disabled:opacity-40"
          >
            <Minus className="h-4 w-4" />
          </button>
          <span className="min-w-[48px] text-center text-sm tabular-nums text-gray-600">
            {Math.round(mucPhong * 100)}%
          </span>
          <button
            type="button"
            onClick={() => doiPhong(1)}
            disabled={mucPhong === PHONG_TO[PHONG_TO.length - 1]}
            aria-label="Phóng to"
            className="rounded-md p-1.5 text-gray-600 hover:bg-gray-100 disabled:opacity-40"
          >
            <Plus className="h-4 w-4" />
          </button>
          <span className="mx-1 h-5 w-px bg-gray-200" />
          <button
            type="button"
            onClick={batTatToanManHinh}
            aria-label={toanManHinh ? "Thoát toàn màn hình" : "Toàn màn hình"}
            title={toanManHinh ? "Thoát toàn màn hình" : "Toàn màn hình"}
            className="rounded-md p-1.5 text-gray-600 hover:bg-gray-100"
          >
            {toanManHinh ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </button>
        </div>
      </div>

      <div
        ref={khungRef}
        className={`w-full overflow-auto bg-gray-100 p-4 ${toanManHinh ? "flex-1" : "max-h-[75vh]"}`}
      >
        {dangTai ? (
          <div className="flex h-[60vh] items-center justify-center">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : (
          <div className="flex w-fit min-w-full flex-col items-center gap-4">
            {Array.from({ length: soTrang }).map((_, i) => (
              <div
                key={i}
                data-trang={i + 1}
                ref={(el) => {
                  trangRef.current[i] = el;
                }}
                className="bg-white shadow-sm"
              >
                <canvas className="block" />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
