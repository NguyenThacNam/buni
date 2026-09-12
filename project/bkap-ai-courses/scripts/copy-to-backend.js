/**
 * Copy thư mục build/ của CRA sang src/main/resources/static của Spring Boot.
 * Chạy qua `npm run build:be` (đã gọi `npm run build` trước đó).
 *
 * Thư mục static cũ bị xóa sạch trước khi copy — cần thiết vì tên file chunk
 * có hash, không xóa thì các bản build cũ sẽ tồn đọng lại mãi.
 */
const fs = require("fs");
const path = require("path");

const SOURCE = path.resolve(__dirname, "..", "build");
const TARGET = path.resolve(
  __dirname,
  "..",
  "..",
  "BKAP-Courses",
  "src",
  "main",
  "resources",
  "static",
);

if (!fs.existsSync(SOURCE)) {
  console.error(`[copy-to-backend] Không tìm thấy thư mục build: ${SOURCE}`);
  console.error("[copy-to-backend] Chạy `npm run build` trước.");
  process.exit(1);
}

// Chốt chặn: chỉ cho phép xóa đúng thư mục .../resources/static
if (
  path.basename(TARGET) !== "static" ||
  path.basename(path.dirname(TARGET)) !== "resources"
) {
  console.error(`[copy-to-backend] Đường dẫn đích không hợp lệ: ${TARGET}`);
  process.exit(1);
}

fs.rmSync(TARGET, { recursive: true, force: true });
fs.cpSync(SOURCE, TARGET, { recursive: true });

console.log(`[copy-to-backend] Đã copy ${SOURCE}`);
console.log(`[copy-to-backend]        -> ${TARGET}`);
