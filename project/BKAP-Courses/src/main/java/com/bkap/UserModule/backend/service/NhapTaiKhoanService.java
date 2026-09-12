package com.bkap.UserModule.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
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
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bkap.CoursesModule.backend.service.IEnrollmentService;
import com.bkap.CoursesModule.backend.service.LmsSsoService;
import com.bkap.CoursesModule.entity.Course;
import com.bkap.CoursesModule.entity.Enrollment;
import com.bkap.UserModule.entity.User;
import com.bkap.UserModule.entity.UserRole;

/**
 * Nhập tài khoản học viên hàng loạt từ file Excel.
 *
 * Trung tâm cấp tài khoản cho cả lớp một lượt thay vì gõ tay từng người. Mỗi
 * dòng được xử lý độc lập: dòng lỗi thì bỏ qua kèm lý do, dòng đúng vẫn được
 * tạo — admin sửa mấy dòng lỗi rồi nhập lại cả file cũng không sao, vì tài
 * khoản đã có sẽ được nhận ra và bỏ qua.
 */
@Service
public class NhapTaiKhoanService {

	/** Tiêu đề cột trong file mẫu, theo đúng thứ tự. */
	public static final String[] TIEU_DE = { "Tên đăng nhập", "Họ tên", "Email", "Số điện thoại", "Ngày sinh",
			"Mật khẩu (để trống = tự sinh)" };

	private static final Pattern TEN_DANG_NHAP = Pattern.compile("^[a-z0-9._-]{3,50}$");
	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Pattern SO_DIEN_THOAI = Pattern.compile("^0\\d{9}$");

	/** Bỏ chữ dễ nhầm (0/O, 1/l/I) để học viên đọc từ giấy in không gõ sai. */
	private static final String BANG_CHU = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
	private static final int DO_DAI_MAT_KHAU_TU_SINH = 8;
	private static final int DO_DAI_MAT_KHAU_TOI_THIEU = 6;

	private static final DateTimeFormatter DINH_DANG_LUU = DateTimeFormatter.ISO_LOCAL_DATE; // 2000-01-31

	@Autowired
	private IUserService userService;

	@Autowired
	private IEnrollmentService enrollmentService;

	@Autowired
	private LmsSsoService lmsSsoService;

	private final SecureRandom ngauNhien = new SecureRandom();
	private final DataFormatter dinhDangO = new DataFormatter();

	// ─────────────────────────────────────────────────────────────
	// KẾT QUẢ
	// ─────────────────────────────────────────────────────────────

	public enum TrangThai {
		DA_TAO, DA_CO, LOI
	}

	/** Kết quả xử lý một dòng trong file. */
	public static class DongKetQua implements java.io.Serializable {
		private static final long serialVersionUID = 1L;

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
	 * @param khoaGhiDanh không bắt buộc — có thì ghi danh luôn cả lớp vào khóa này,
	 *                    kể cả những tài khoản đã có sẵn
	 * @throws IllegalArgumentException khi cả file hỏng (không phải Excel, thiếu
	 *                                  cột bắt buộc), kèm câu báo cho admin
	 */
	public List<DongKetQua> nhap(InputStream file, Course khoaGhiDanh) throws Exception {
		List<DongKetQua> ketQua = new ArrayList<>();

		try (Workbook wb = moFile(file)) {
			Sheet sheet = wb.getSheetAt(0);
			Map<String, Integer> cot = docTieuDe(sheet.getRow(sheet.getFirstRowNum()));

			// Trùng TRONG CHÍNH file: CSDL chưa có nên kiểm tra CSDL không bắt được,
			// để lọt thì dòng sau hỏng vì ràng buộc UNIQUE mà báo lỗi khó hiểu.
			Set<String> tenDaGap = new HashSet<>(), emailDaGap = new HashSet<>(), sdtDaGap = new HashSet<>();

			for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (dongTrong(row)) {
					continue;
				}
				ketQua.add(xuLyDong(row, r + 1, cot, khoaGhiDanh, tenDaGap, emailDaGap, sdtDaGap));
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
	 * Nhận cột theo TÊN tiêu đề chứ không theo vị trí, để admin đổi thứ tự cột
	 * hay chèn thêm cột ghi chú riêng mà vẫn nhập được.
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
			} else if (ten.contains("ngay sinh") || ten.equals("birthday")) {
				khoa = "ngaysinh";
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
		if (!cot.containsKey("sdt")) thieu.add("Số điện thoại");
		if (!cot.containsKey("ngaysinh")) thieu.add("Ngày sinh");
		if (!thieu.isEmpty()) {
			throw new IllegalArgumentException("Dòng đầu tiên của file thiếu cột: " + String.join(", ", thieu)
					+ ". Hãy tải file mẫu về và điền theo đúng các cột trong đó.");
		}
		return cot;
	}

	private DongKetQua xuLyDong(Row row, int soDong, Map<String, Integer> cot, Course khoaGhiDanh,
			Set<String> tenDaGap, Set<String> emailDaGap, Set<String> sdtDaGap) {
		DongKetQua kq = new DongKetQua();
		kq.soDong = soDong;

		// Moodle bắt tên đăng nhập viết thường; chuẩn hóa ngay từ đầu để hai bên khớp.
		String ten = docChu(row, cot.get("ten")).toLowerCase();
		String hoTen = docChu(row, cot.get("hoten"));
		String email = docChu(row, cot.get("email")).toLowerCase();
		String sdt = docSoDienThoai(row, cot.get("sdt"));
		String ngaySinh = docNgay(row, cot.get("ngaysinh"));
		String matKhau = cot.containsKey("matkhau") ? docChu(row, cot.get("matkhau")) : "";

		kq.tenDangNhap = ten;
		kq.hoTen = hoTen;

		// Trùng trong file phải bắt TRƯỚC khi hỏi CSDL. Nếu hỏi CSDL trước thì dòng
		// đầu vừa tạo tài khoản xong, dòng lặp lại bên dưới lại thấy "đã có sẵn" —
		// báo sai bản chất, và admin không biết file mình có dòng trùng.
		if (!ten.isEmpty() && tenDaGap.contains(ten)) {
			kq.trangThai = TrangThai.LOI;
			kq.ghiChu = "Tên đăng nhập bị lặp lại trong file (trùng một dòng phía trên).";
			return kq;
		}

		// Tài khoản đã có: không tạo lại, không đụng tới thông tin cũ. Vẫn ghi danh
		// nếu admin chọn khóa — dùng khi đưa một lớp cũ vào khóa mới.
		if (!ten.isEmpty() && userService.existsByUsername(ten)) {
			kq.trangThai = TrangThai.DA_CO;
			kq.ghiChu = "Tài khoản đã có sẵn, giữ nguyên thông tin cũ.";
			tenDaGap.add(ten);
			ghiDanhNeuCan(userService.findByUsername(ten), khoaGhiDanh, kq);
			return kq;
		}

		String loi = kiemTra(ten, hoTen, email, sdt, ngaySinh, matKhau, tenDaGap, emailDaGap, sdtDaGap);
		if (loi != null) {
			kq.trangThai = TrangThai.LOI;
			kq.ghiChu = loi;
			return kq;
		}
		tenDaGap.add(ten);
		emailDaGap.add(email);
		sdtDaGap.add(sdt);

		boolean tuSinh = matKhau.isEmpty();
		if (tuSinh) {
			matKhau = sinhMatKhau();
		}

		User user = new User();
		user.setUsername(ten);
		user.setFullname(hoTen);
		user.setEmail(email);
		user.setPhone(sdt);
		user.setBirthday(ngaySinh);
		user.setRole(UserRole.STUDENT);

		try {
			user = userService.saveFromAdmin(user, matKhau);
		} catch (Exception e) {
			kq.trangThai = TrangThai.LOI;
			kq.ghiChu = "Không lưu được (có thể trùng tên đăng nhập, email hoặc số điện thoại).";
			return kq;
		}

		kq.trangThai = TrangThai.DA_TAO;
		kq.matKhauTuSinh = tuSinh ? matKhau : null;
		kq.ghiChu = tuSinh ? "Đã tạo, mật khẩu do hệ thống sinh." : "Đã tạo, dùng mật khẩu trong file.";
		ghiDanhNeuCan(user, khoaGhiDanh, kq);
		return kq;
	}

	/** Trả về câu báo lỗi đầu tiên gặp phải, hoặc null nếu dòng hợp lệ. */
	private String kiemTra(String ten, String hoTen, String email, String sdt, String ngaySinh, String matKhau,
			Set<String> tenDaGap, Set<String> emailDaGap, Set<String> sdtDaGap) {
		if (ten.isEmpty()) return "Thiếu tên đăng nhập.";
		if (!TEN_DANG_NHAP.matcher(ten).matches())
			return "Tên đăng nhập chỉ được dùng chữ thường không dấu, số và . _ - (3–50 ký tự).";
		if (tenDaGap.contains(ten)) return "Tên đăng nhập bị lặp lại trong file.";

		if (hoTen.isEmpty()) return "Thiếu họ tên.";
		if (hoTen.length() > 100) return "Họ tên dài quá 100 ký tự.";

		if (email.isEmpty()) return "Thiếu email.";
		if (!EMAIL.matcher(email).matches() || email.length() > 150) return "Email không hợp lệ.";
		if (emailDaGap.contains(email)) return "Email bị lặp lại trong file.";
		if (userService.existsByEmail(email)) return "Email đã được tài khoản khác sử dụng.";

		if (sdt.isEmpty()) return "Thiếu số điện thoại.";
		if (!SO_DIEN_THOAI.matcher(sdt).matches()) return "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.";
		if (sdtDaGap.contains(sdt)) return "Số điện thoại bị lặp lại trong file.";
		if (userService.existsByPhone(sdt)) return "Số điện thoại đã được tài khoản khác sử dụng.";

		if (ngaySinh == null) return "Ngày sinh trống hoặc sai định dạng (dùng dd/mm/yyyy).";

		if (!matKhau.isEmpty() && matKhau.length() < DO_DAI_MAT_KHAU_TOI_THIEU)
			return "Mật khẩu trong file phải có ít nhất " + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự (hoặc để trống).";
		return null;
	}

	/**
	 * Ghi danh vào khóa đã chọn, rồi đẩy sang LMS. LMS lỗi không làm hỏng dòng:
	 * tài khoản và ghi danh bên buni vẫn giữ, lần đầu học viên bấm bài kiểm tra hệ
	 * thống sẽ tự ghi danh bù.
	 */
	private void ghiDanhNeuCan(User user, Course khoa, DongKetQua kq) {
		if (khoa == null || user == null) {
			return;
		}

		if (!enrollmentService.daGhiDanh(user.getUsername(), khoa.getId())) {
			Enrollment e = new Enrollment();
			e.setUser(user);
			e.setCourse(khoa);
			try {
				enrollmentService.save(e);
			} catch (Exception ex) {
				kq.ghiChu += " Chưa ghi danh được vào khóa.";
				return;
			}
		}

		if (khoa.getMoodleCourseId() == null) {
			kq.ghiChu += " Đã ghi danh (khóa chưa nối LMS).";
			return;
		}
		try {
			lmsSsoService.ghiDanhBenLms(user, khoa);
			kq.ghiChu += " Đã ghi danh, cả bên LMS.";
		} catch (Exception ex) {
			kq.ghiChu += " Đã ghi danh bên buni, CHƯA đẩy được sang LMS (" + ex.getMessage() + ").";
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
		String so;
		if (c.getCellType() == CellType.NUMERIC) {
			so = String.valueOf((long) c.getNumericCellValue());
		} else {
			so = dinhDangO.formatCellValue(c);
		}
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

	/**
	 * Ngày sinh có thể là ô ngày tháng thật của Excel, hoặc chữ gõ tay kiểu
	 * 31/01/2000, 31-1-2000, 2000-01-31. Trả về dạng lưu trong CSDL, hoặc null
	 * nếu không hiểu được.
	 */
	private String docNgay(Row row, Integer cot) {
		if (cot == null || row == null || row.getCell(cot) == null) {
			return null;
		}
		Cell c = row.getCell(cot);
		try {
			if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
				LocalDate d = c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				return d.format(DINH_DANG_LUU);
			}
		} catch (Exception ignored) {
			// rơi xuống đọc dạng chữ
		}

		String chu = dinhDangO.formatCellValue(c).trim();
		if (chu.isEmpty()) {
			return null;
		}
		// STRICT: mặc định Java "làm tròn" ngày không tồn tại — 31/02 thành 29/02 —
		// rồi lưu im lặng một ngày sinh sai. Chế độ nghiêm ngặt thì từ chối hẳn.
		// (Ở chế độ này phải viết năm là uuuu; yyyy đòi kèm cả kỷ nguyên.)
		for (String mau : new String[] { "d/M/uuuu", "d-M-uuuu", "d.M.uuuu", "uuuu-M-d", "uuuu/M/d" }) {
			try {
				LocalDate d = LocalDate.parse(chu, DateTimeFormatter.ofPattern(mau).withResolverStyle(ResolverStyle.STRICT));
				if (d.isAfter(LocalDate.now()) || d.getYear() < 1900) {
					return null;
				}
				return d.format(DINH_DANG_LUU);
			} catch (Exception ignored) {
				// thử mẫu kế tiếp
			}
		}
		return null;
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

	private String sinhMatKhau() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < DO_DAI_MAT_KHAU_TU_SINH; i++) {
			sb.append(BANG_CHU.charAt(ngauNhien.nextInt(BANG_CHU.length())));
		}
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

			// Cột SĐT và ngày sinh để dạng CHỮ: không thì Excel tự đổi 0901234567
			// thành số và nuốt mất số 0 đầu, còn 01/02/2000 bị hiểu thành 2 tháng 1.
			CellStyle kieuChu = wb.createCellStyle();
			kieuChu.setDataFormat(wb.createDataFormat().getFormat("@"));

			Row tieuDe = sh.createRow(0);
			for (int i = 0; i < TIEU_DE.length; i++) {
				Cell c = tieuDe.createCell(i);
				c.setCellValue(TIEU_DE[i]);
				c.setCellStyle(dam);
				sh.setDefaultColumnStyle(i, kieuChu);
			}

			String[] viDu = { "nguyenvana", "Nguyễn Văn A", "nguyenvana@example.com", "0901234567", "15/03/2001", "" };
			Row r = sh.createRow(1);
			for (int i = 0; i < viDu.length; i++) {
				Cell c = r.createCell(i);
				c.setCellValue(viDu[i]);
				c.setCellStyle(kieuChu);
			}

			int[] rong = { 18, 26, 30, 16, 14, 30 };
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
	 * Danh sách tài khoản vừa tạo kèm mật khẩu, để admin in hoặc gửi cho học
	 * viên. Chỉ gồm các dòng tạo mới — tài khoản cũ không có mật khẩu mới để phát.
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
