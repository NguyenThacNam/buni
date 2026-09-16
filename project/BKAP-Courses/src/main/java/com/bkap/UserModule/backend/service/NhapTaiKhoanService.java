package com.bkap.UserModule.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bkap.CoursesModule.backend.service.LmsSsoService;
import com.bkap.UserModule.dto.MoodleUserRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Nhập tài khoản học viên hàng loạt từ Excel — tạo thẳng trên LMS.
 *
 * Đây là trang quản trị DUY NHẤT còn lại của buni. Lý do giữ: Moodle không có
 * sẵn cách nhập một danh sách lớp kèm ghi danh chỉ bằng một file, còn trung tâm
 * thì mỗi khóa lại nhập một lớp mới.
 *
 * Khác bản cũ ở chỗ không còn ghi vào CSDL buni: tài khoản tạo bên Moodle, ghi
 * danh cũng bên Moodle. buni chỉ đọc file, kiểm tra dữ liệu và gọi API.
 */
@Service
public class NhapTaiKhoanService {

	/** Tiêu đề cột trong file mẫu, theo đúng thứ tự. */
	public static final String[] TIEU_DE = { "Tên đăng nhập", "Họ tên", "Email", "Số điện thoại (tùy chọn)",
			"Mật khẩu (để trống = tự sinh)" };

	private static final Pattern TEN_DANG_NHAP = Pattern.compile("^[a-z0-9._-]{3,50}$");
	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	/**
	 * Mật khẩu tự sinh phải qua được chính sách mặc định của Moodle: tối thiểu 8
	 * ký tự, có đủ chữ thường, chữ hoa, chữ số và một ký tự đặc biệt.
	 */
	private static final String CHU_THUONG = "abcdefghjkmnpqrstuvwxyz";
	private static final String CHU_HOA = "ABCDEFGHJKMNPQRSTUVWXYZ";
	private static final String CHU_SO = "23456789";
	private static final String DAC_BIET = "@#$%&*";
	private static final int DO_DAI_MAT_KHAU_TOI_THIEU = 8;

	@Autowired
	private IMoodleService moodleService;

	@Autowired
	private MoodleAuthService moodleAuthService;

	@Autowired
	private LmsSsoService lmsSsoService;

	private final SecureRandom ngauNhien = new SecureRandom();
	private final DataFormatter dinhDangO = new DataFormatter();
	private final ObjectMapper objectMapper = new ObjectMapper();

	// ─────────────────────────────────────────────────────────────
	// KẾT QUẢ
	// ─────────────────────────────────────────────────────────────

	public enum TrangThai {
		DA_TAO, DA_CO, LOI
	}

	/** Kết quả xử lý một dòng trong file. */
	public static class DongKetQua implements java.io.Serializable {
		private static final long serialVersionUID = 2L;

		public int soDong;
		public String tenDangNhap = "";
		public String hoTen = "";
		public TrangThai trangThai;
		/** Chỉ có khi hệ thống tự sinh — để admin phát cho học viên. */
		public String matKhauTuSinh;
		public String ghiChu = "";

		public int getSoDong() { return soDong; }
		public String getTenDangNhap() { return tenDangNhap; }
		public String getHoTen() { return hoTen; }
		public TrangThai getTrangThai() { return trangThai; }
		public String getMatKhauTuSinh() { return matKhauTuSinh; }
		public String getGhiChu() { return ghiChu; }
	}

	// ─────────────────────────────────────────────────────────────
	// NHẬP
	// ─────────────────────────────────────────────────────────────

	/**
	 * @param moodleCourseId khóa để ghi danh cả lớp vào, hoặc null nếu chỉ tạo tài
	 *                       khoản
	 * @throws IllegalArgumentException khi cả file hỏng (không phải Excel, thiếu
	 *                                  cột bắt buộc), kèm câu báo cho admin
	 */
	public List<DongKetQua> nhap(InputStream file, Integer moodleCourseId) throws Exception {
		List<DongKetQua> ketQua = new ArrayList<>();

		try (Workbook wb = moFile(file)) {
			Sheet sheet = wb.getSheetAt(0);
			Map<String, Integer> cot = docTieuDe(sheet.getRow(sheet.getFirstRowNum()));

			// Trùng TRONG CHÍNH file: bên LMS chưa có nên hỏi LMS không bắt được,
			// để lọt thì dòng sau bị Moodle từ chối với câu báo khó hiểu.
			Set<String> tenDaGap = new HashSet<>(), emailDaGap = new HashSet<>();

			for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (dongTrong(row)) {
					continue;
				}
				ketQua.add(xuLyDong(row, r + 1, cot, moodleCourseId, tenDaGap, emailDaGap));
			}
		}

		if (ketQua.isEmpty()) {
			throw new IllegalArgumentException("File không có dòng dữ liệu nào bên dưới dòng tiêu đề.");
		}
		return ketQua;
	}

	private Workbook moFile(InputStream file) {
		try {
			return WorkbookFactory.create(file);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Không đọc được file. Hãy dùng file Excel (.xlsx) — tốt nhất là tải file mẫu về rồi điền vào.");
		}
	}

	/**
	 * Nhận cột theo TÊN tiêu đề chứ không theo vị trí, để admin đổi thứ tự cột hay
	 * chèn thêm cột ghi chú riêng mà vẫn nhập được.
	 */
	private Map<String, Integer> docTieuDe(Row tieuDe) {
		if (tieuDe == null) {
			throw new IllegalArgumentException("File trống.");
		}
		Map<String, Integer> cot = new HashMap<>();
		for (Cell c : tieuDe) {
			String ten = khongDau(dinhDangO.formatCellValue(c));
			String khoa = null;
			if (ten.contains("dang nhap") || ten.equals("username") || ten.equals("tai khoan")) {
				khoa = "ten";
			} else if (ten.contains("ho ten") || ten.contains("ho va ten") || ten.equals("fullname")) {
				khoa = "hoten";
			} else if (ten.contains("email") || ten.contains("thu dien tu")) {
				khoa = "email";
			} else if (ten.contains("dien thoai") || ten.equals("sdt") || ten.equals("phone")) {
				khoa = "sdt";
			} else if (ten.contains("mat khau") || ten.equals("password")) {
				khoa = "matkhau";
			}
			if (khoa != null && !cot.containsKey(khoa)) {
				cot.put(khoa, c.getColumnIndex());
			}
		}

		List<String> thieu = new ArrayList<>();
		if (!cot.containsKey("ten")) thieu.add("Tên đăng nhập");
		if (!cot.containsKey("hoten")) thieu.add("Họ tên");
		if (!cot.containsKey("email")) thieu.add("Email");
		if (!thieu.isEmpty()) {
			throw new IllegalArgumentException("Dòng đầu tiên của file thiếu cột: " + String.join(", ", thieu)
					+ ". Hãy tải file mẫu về và điền theo đúng các cột trong đó.");
		}
		return cot;
	}

	private DongKetQua xuLyDong(Row row, int soDong, Map<String, Integer> cot, Integer moodleCourseId,
			Set<String> tenDaGap, Set<String> emailDaGap) {
		DongKetQua kq = new DongKetQua();
		kq.soDong = soDong;

		// Moodle bắt tên đăng nhập viết thường.
		String ten = docChu(row, cot.get("ten")).toLowerCase();
		String hoTen = docChu(row, cot.get("hoten"));
		String email = docChu(row, cot.get("email")).toLowerCase();
		String sdt = docSoDienThoai(row, cot.get("sdt"));
		String matKhau = cot.containsKey("matkhau") ? docChu(row, cot.get("matkhau")) : "";

		kq.tenDangNhap = ten;
		kq.hoTen = hoTen;

		// Trùng trong file phải bắt TRƯỚC khi hỏi LMS: dòng đầu vừa tạo xong thì
		// dòng lặp lại bên dưới sẽ thấy "đã có sẵn", báo sai bản chất.
		if (!ten.isEmpty() && tenDaGap.contains(ten)) {
			kq.trangThai = TrangThai.LOI;
			kq.ghiChu = "Tên đăng nhập bị lặp lại trong file (trùng một dòng phía trên).";
			return kq;
		}

		try {
			// Tài khoản đã có bên LMS: không tạo lại, không đụng thông tin cũ.
			// Vẫn ghi danh nếu admin chọn khóa — dùng khi đưa một lớp cũ vào khóa mới.
			JsonNode daCo = moodleAuthService.timTheoTenDangNhap(ten.isEmpty() ? "___" : ten);
			if (daCo != null) {
				kq.trangThai = TrangThai.DA_CO;
				kq.ghiChu = "Tài khoản đã có trên LMS, giữ nguyên thông tin cũ.";
				tenDaGap.add(ten);
				ghiDanhNeuCan(daCo.path("id").asInt(), moodleCourseId, kq);
				return kq;
			}

			String loi = kiemTra(ten, hoTen, email, matKhau, tenDaGap, emailDaGap);
			if (loi != null) {
				kq.trangThai = TrangThai.LOI;
				kq.ghiChu = loi;
				return kq;
			}
			tenDaGap.add(ten);
			emailDaGap.add(email);

			boolean tuSinh = matKhau.isEmpty();
			if (tuSinh) {
				matKhau = sinhMatKhau();
			}

			MoodleUserRequest dto = new MoodleUserRequest();
			dto.setUsername(ten);
			dto.setEmail(email);
			dto.setPassword(matKhau);
			dto.setPhone(sdt);
			datHoTen(dto, hoTen, ten);

			String phanHoi = moodleService.createMoodleUser(dto);
			JsonNode taoMoi = objectMapper.readTree(phanHoi == null ? "[]" : phanHoi);
			if (!taoMoi.isArray() || taoMoi.size() == 0) {
				kq.trangThai = TrangThai.LOI;
				kq.ghiChu = "LMS từ chối tạo tài khoản. Thường gặp nhất là email đã có người khác dùng; "
						+ "ngoài ra có thể do mật khẩu trong file không đạt chính sách của LMS.";
				return kq;
			}

			kq.trangThai = TrangThai.DA_TAO;
			kq.matKhauTuSinh = tuSinh ? matKhau : null;
			kq.ghiChu = tuSinh ? "Đã tạo trên LMS, mật khẩu do hệ thống sinh."
					: "Đã tạo trên LMS, dùng mật khẩu trong file.";
			ghiDanhNeuCan(taoMoi.get(0).path("id").asInt(), moodleCourseId, kq);

		} catch (Exception e) {
			kq.trangThai = TrangThai.LOI;
			kq.ghiChu = "Lỗi khi gọi LMS: " + e.getMessage();
		}
		return kq;
	}

	/** Trả về câu báo lỗi đầu tiên gặp phải, hoặc null nếu dòng hợp lệ. */
	private String kiemTra(String ten, String hoTen, String email, String matKhau, Set<String> tenDaGap,
			Set<String> emailDaGap) throws Exception {
		if (ten.isEmpty()) return "Thiếu tên đăng nhập.";
		if (!TEN_DANG_NHAP.matcher(ten).matches())
			return "Tên đăng nhập chỉ được dùng chữ thường không dấu, số và . _ - (3–50 ký tự).";

		if (hoTen.isEmpty()) return "Thiếu họ tên.";
		if (hoTen.length() > 100) return "Họ tên dài quá 100 ký tự.";

		if (email.isEmpty()) return "Thiếu email.";
		if (!EMAIL.matcher(email).matches() || email.length() > 150) return "Email không hợp lệ.";
		if (emailDaGap.contains(email)) return "Email bị lặp lại trong file.";
		// Moodle bắt email không trùng giữa các tài khoản.
		//
		// Phép kiểm này chỉ chạy được khi tài khoản dịch vụ được phép XEM email của
		// người dùng. Chưa mở quyền đó thì Moodle trả danh sách rỗng cho mọi email,
		// và dòng trùng sẽ bị bắt muộn hơn — lúc tạo, với câu báo chung chung hơn.
		if (moodleAuthService.timTheoEmail(email) != null) return "Email đã có tài khoản khác dùng trên LMS.";

		if (!matKhau.isEmpty()) {
			String loiMatKhau = kiemTraMatKhau(matKhau);
			if (loiMatKhau != null) return loiMatKhau;
		}
		return null;
	}

	/**
	 * Chính sách mật khẩu mặc định của Moodle: tối thiểu 8 ký tự, có chữ thường,
	 * chữ hoa, chữ số và ký tự đặc biệt. Kiểm trước khi gửi để báo đúng thiếu gì —
	 * để Moodle từ chối thì chỉ nhận được câu báo chung chung.
	 *
	 * @return câu báo lỗi, hoặc null nếu mật khẩu đạt
	 */
	private String kiemTraMatKhau(String matKhau) {
		List<String> thieu = new ArrayList<>();
		if (matKhau.length() < DO_DAI_MAT_KHAU_TOI_THIEU) thieu.add("ít nhất " + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự");
		if (!matKhau.matches(".*[a-z].*")) thieu.add("chữ thường");
		if (!matKhau.matches(".*[A-Z].*")) thieu.add("chữ hoa");
		if (!matKhau.matches(".*[0-9].*")) thieu.add("chữ số");
		if (!matKhau.matches(".*[^a-zA-Z0-9].*")) thieu.add("ký tự đặc biệt");
		if (thieu.isEmpty()) return null;
		return "Mật khẩu trong file chưa đạt, còn thiếu: " + String.join(", ", thieu)
				+ ". (Hoặc để trống để hệ thống tự sinh.)";
	}

	/** Ghi danh vào khóa đã chọn. Hỏng thì ghi chú lại, không làm hỏng cả dòng. */
	private void ghiDanhNeuCan(int moodleUserId, Integer moodleCourseId, DongKetQua kq) {
		if (moodleCourseId == null || moodleUserId <= 0) {
			return;
		}
		try {
			lmsSsoService.ghiDanhBenLms(moodleUserId, moodleCourseId);
			kq.ghiChu += " Đã ghi danh vào khóa.";
		} catch (Exception e) {
			kq.ghiChu += " CHƯA ghi danh được vào khóa (" + e.getMessage() + ").";
		}
	}

	// ─────────────────────────────────────────────────────────────
	// ĐỌC Ô
	// ─────────────────────────────────────────────────────────────

	private String docChu(Row row, Integer cot) {
		if (cot == null || row == null) {
			return "";
		}
		Cell c = row.getCell(cot);
		return c == null ? "" : dinhDangO.formatCellValue(c).trim();
	}

	/**
	 * Excel hay tự biến ô "0901234567" thành SỐ 901234567 — mất số 0 đầu. Gặp ô
	 * kiểu số thì đọc lại dạng nguyên rồi bù số 0 nếu còn thiếu một chữ số.
	 */
	private String docSoDienThoai(Row row, Integer cot) {
		if (cot == null || row == null || row.getCell(cot) == null) {
			return "";
		}
		Cell c = row.getCell(cot);
		String so = c.getCellType() == CellType.NUMERIC ? String.valueOf((long) c.getNumericCellValue())
				: dinhDangO.formatCellValue(c);
		so = so.replaceAll("[\\s.\\-()]", "");
		if (so.startsWith("+84")) {
			so = "0" + so.substring(3);
		} else if (so.startsWith("84") && so.length() == 11) {
			so = "0" + so.substring(2);
		}
		if (so.length() == 9 && !so.startsWith("0")) {
			so = "0" + so;
		}
		return so;
	}

	private boolean dongTrong(Row row) {
		if (row == null) {
			return true;
		}
		for (Cell c : row) {
			if (!dinhDangO.formatCellValue(c).isBlank()) {
				return false;
			}
		}
		return true;
	}

	/** "Tên Đăng Nhập" -> "ten dang nhap", để so tiêu đề không phụ thuộc dấu và hoa thường. */
	private static String khongDau(String s) {
		String bo = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return bo.replace('đ', 'd').replace('Đ', 'D').toLowerCase().replaceAll("\\s+", " ").trim();
	}

	/** Moodle tách họ và tên thành hai trường bắt buộc. */
	private void datHoTen(MoodleUserRequest dto, String fullname, String username) {
		String ten = username, ho = "Học viên";
		if (fullname != null && !fullname.isBlank()) {
			String sach = fullname.trim();
			int viTri = sach.lastIndexOf(' ');
			if (viTri > 0) {
				ten = sach.substring(viTri + 1);
				ho = sach.substring(0, viTri);
			} else {
				ten = sach;
			}
		}
		dto.setFirstname(ten);
		dto.setLastname(ho);
	}

	/**
	 * Mật khẩu ngẫu nhiên đủ mạnh theo chính sách mặc định của Moodle, nhưng vẫn
	 * đọc được từ giấy in: đã bỏ các ký tự dễ nhầm (0/O, 1/l/I).
	 */
	private String sinhMatKhau() {
		StringBuilder sb = new StringBuilder();
		sb.append(CHU_HOA.charAt(ngauNhien.nextInt(CHU_HOA.length())));
		for (int i = 0; i < 5; i++) {
			sb.append(CHU_THUONG.charAt(ngauNhien.nextInt(CHU_THUONG.length())));
		}
		sb.append(CHU_SO.charAt(ngauNhien.nextInt(CHU_SO.length())));
		sb.append(CHU_SO.charAt(ngauNhien.nextInt(CHU_SO.length())));
		sb.append(DAC_BIET.charAt(ngauNhien.nextInt(DAC_BIET.length())));
		return sb.toString();
	}

	// ─────────────────────────────────────────────────────────────
	// XUẤT FILE
	// ─────────────────────────────────────────────────────────────

	/** File mẫu cho admin điền: dòng tiêu đề + một dòng ví dụ. */
	public byte[] taoFileMau() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Sheet sh = wb.createSheet("Hoc vien");
			CellStyle dam = kieuTieuDe(wb);

			// Cột SĐT để dạng CHỮ: không thì Excel tự đổi 0901234567 thành số và
			// nuốt mất số 0 đầu.
			CellStyle kieuChu = wb.createCellStyle();
			kieuChu.setDataFormat(wb.createDataFormat().getFormat("@"));

			Row tieuDe = sh.createRow(0);
			for (int i = 0; i < TIEU_DE.length; i++) {
				Cell c = tieuDe.createCell(i);
				c.setCellValue(TIEU_DE[i]);
				c.setCellStyle(dam);
				sh.setDefaultColumnStyle(i, kieuChu);
			}

			String[] viDu = { "nguyenvana", "Nguyễn Văn A", "nguyenvana@example.com", "0901234567", "" };
			Row r = sh.createRow(1);
			for (int i = 0; i < viDu.length; i++) {
				Cell c = r.createCell(i);
				c.setCellValue(viDu[i]);
				c.setCellStyle(kieuChu);
			}

			int[] rong = { 18, 26, 30, 20, 30 };
			for (int i = 0; i < rong.length; i++) {
				sh.setColumnWidth(i, rong[i] * 256);
			}
			sh.createFreezePane(0, 1);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wb.write(out);
			return out.toByteArray();
		}
	}

	/**
	 * Danh sách tài khoản vừa tạo kèm mật khẩu, để admin in hoặc gửi cho học viên.
	 * Chỉ gồm các dòng tạo mới — tài khoản cũ không có mật khẩu mới để phát.
	 */
	public byte[] xuatTaiKhoan(List<DongKetQua> ketQua) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Sheet sh = wb.createSheet("Tai khoan");
			CellStyle dam = kieuTieuDe(wb);

			String[] cot = { "Họ tên", "Tên đăng nhập", "Mật khẩu", "Ghi chú" };
			Row tieuDe = sh.createRow(0);
			for (int i = 0; i < cot.length; i++) {
				Cell c = tieuDe.createCell(i);
				c.setCellValue(cot[i]);
				c.setCellStyle(dam);
			}

			int r = 1;
			for (DongKetQua k : ketQua) {
				if (k.trangThai != TrangThai.DA_TAO) {
					continue;
				}
				Row row = sh.createRow(r++);
				row.createCell(0).setCellValue(k.hoTen);
				row.createCell(1).setCellValue(k.tenDangNhap);
				row.createCell(2).setCellValue(k.matKhauTuSinh != null ? k.matKhauTuSinh : "(theo file đã nhập)");
				row.createCell(3).setCellValue("Đổi mật khẩu sau lần đăng nhập đầu tiên");
			}

			int[] rong = { 28, 20, 18, 40 };
			for (int i = 0; i < rong.length; i++) {
				sh.setColumnWidth(i, rong[i] * 256);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wb.write(out);
			return out.toByteArray();
		}
	}

	private CellStyle kieuTieuDe(Workbook wb) {
		CellStyle s = wb.createCellStyle();
		Font f = wb.createFont();
		f.setBold(true);
		s.setFont(f);
		return s;
	}
}
