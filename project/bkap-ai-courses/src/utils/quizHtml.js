/**
 * Bóc HTML câu hỏi do Moodle dựng sẵn thành dữ liệu gọn để buni tự vẽ lại.
 *
 * Moodle trả mỗi câu hỏi dưới dạng một khối HTML hoàn chỉnh (kèm class, nút cắm
 * cờ, ô ẩn...). Nhét nguyên khối đó vào trang thì giao diện lẫn lộn kiểu Moodle;
 * nên chỉ lấy đúng phần cần: đề bài, các lựa chọn, và các trường Moodle bắt gửi
 * lại khi lưu.
 *
 * Dùng DOMParser: dựng cây DOM mà không chạy script, không tải ảnh — an toàn hơn
 * gán innerHTML, và đọc cấu trúc chắc chắn hơn tách chuỗi bằng regex.
 *
 * Hiện chỉ hỗ trợ trắc nghiệm chọn MỘT đáp án — toàn bộ 105 câu trong các bài
 * hiện có đều thuộc dạng này. Dạng khác thì trả về null để giao diện đưa người
 * học sang làm trên LMS.
 */

const parse = (html) => new DOMParser().parseFromString(html || "", "text/html");

/** Nhãn lựa chọn: phần chữ bên cạnh ô tròn, bỏ tiền tố "A. ". */
const nhanLuaChon = (khoi) =>
  (khoi.querySelector(".flex-fill") || khoi.querySelector("label") || khoi).innerHTML;

const chuTron = (s) => (s || "").replace(/\s+/g, " ").trim();

/**
 * Câu hỏi đang làm.
 *
 * @returns {{deBai, tenDapAn, tenSequence, sequence, luaChon: Array<{value, nhan, dangChon}>} | null}
 */
export function bocCauHoi(html) {
  const doc = parse(html);
  const deBai = doc.querySelector(".qtext");
  const seq = doc.querySelector('input[type="hidden"][name$="_:sequencecheck"]');
  const oChon = [...doc.querySelectorAll('.answer input[type="radio"]')];
  if (!deBai || !seq || oChon.length === 0) return null;

  // Moodle chèn thêm một ô "bỏ chọn" có value -1, không phải đáp án thật.
  const luaChon = oChon
    .filter((o) => o.value !== "-1")
    .map((o) => {
      const khoi = o.closest(".answer > div") || o.parentElement;
      return { value: o.value, nhan: nhanLuaChon(khoi), dangChon: o.checked };
    });

  return {
    deBai: deBai.innerHTML,
    tenDapAn: oChon[0].name, // ví dụ "q16:1_answer"
    tenSequence: seq.name, // ví dụ "q16:1_:sequencecheck"
    sequence: seq.value,
    luaChon,
  };
}

/**
 * Câu hỏi trong trang kết quả. Moodle chỉ gắn class correct/incorrect vào lựa
 * chọn NGƯỜI HỌC ĐÃ CHỌN; đáp án đúng nằm riêng trong câu "Đáp án đúng là: ...",
 * nên đối chiếu chữ để tô xanh lựa chọn đúng.
 *
 * Nếu cài đặt xem lại của bài không cho hiện đáp án đúng thì khối .rightanswer
 * không có — khi đó chỉ tô được lựa chọn của người học.
 */
export function bocKetQua(html) {
  const doc = parse(html);
  const deBai = doc.querySelector(".qtext");
  const khoiDung = doc.querySelector(".rightanswer");
  const chuDapAnDung = khoiDung ? chuTron(khoiDung.textContent) : "";

  const luaChon = [...doc.querySelectorAll('.answer input[type="radio"]')]
    .filter((o) => o.value !== "-1")
    .map((o) => {
      const khoi = o.closest(".answer > div") || o.parentElement;
      const nhan = nhanLuaChon(khoi);
      const chuNhan = chuTron(parse(nhan).body.textContent);
      return {
        nhan,
        daChon: o.checked,
        sai: khoi.classList.contains("incorrect"),
        // Đúng nếu Moodle tự đánh dấu, hoặc chữ nhãn khớp với câu đáp án đúng.
        dung:
          khoi.classList.contains("correct") ||
          (chuNhan.length > 0 && chuDapAnDung.endsWith(chuNhan)),
      };
    });

  return {
    deBai: deBai ? deBai.innerHTML : "",
    luaChon,
    coDapAnDung: Boolean(khoiDung),
  };
}

/**
 * Gói đáp án thành dạng Moodle nhận: danh sách slot trên trang, sequencecheck của
 * từng câu, và giá trị đã chọn. Câu chưa chọn thì không gửi dòng đáp án.
 */
export function goiDapAn(cauHoi, dapAn) {
  const du = [{ name: "slots", value: cauHoi.map((c) => c.slot).join(",") }];
  for (const c of cauHoi) {
    if (!c.boc) continue;
    du.push({ name: c.boc.tenSequence, value: String(c.boc.sequence) });
    const chon = dapAn[c.slot];
    if (chon !== undefined && chon !== null) {
      du.push({ name: c.boc.tenDapAn, value: String(chon) });
    }
  }
  return du;
}
