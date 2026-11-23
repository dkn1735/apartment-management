package vn.edu.hust.chungcu.auth.service;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa số điện thoại Việt Nam về dạng {@code 0} cộng 9 chữ số, dùng chung cho đăng
 * nhập (spec 0004) và quản lý tài khoản (spec 0008, AC-1).
 */
public final class SoDienThoaiChuanHoa {

	/** Khoảng trắng ASCII và Unicode, dấu chấm, gạch nối: bỏ khỏi số điện thoại. */
	static final Pattern KY_TU_PHAN_CACH = Pattern.compile("[\\s\\p{Z}.\\-]");

	static final Pattern SO_DIEN_THOAI = Pattern.compile("^0[0-9]{9}$");

	private SoDienThoaiChuanHoa() {
	}

	/**
	 * Bỏ ký tự phân cách và đổi đầu số {@code +84} hay {@code 84} thành {@code 0}, chưa
	 * kiểm định dạng; {@code null} cho ra chuỗi rỗng.
	 */
	public static String boPhanCach(String raw) {
		if (raw == null) {
			return "";
		}
		String digits = KY_TU_PHAN_CACH.matcher(raw.strip()).replaceAll("");
		if (digits.startsWith("+84")) {
			return "0" + digits.substring(3);
		}
		if (digits.startsWith("84") && digits.length() == 11) {
			return "0" + digits.substring(2);
		}
		return digits;
	}

	/** Số đã chuẩn hóa nếu đúng dạng {@code ^0[0-9]{9}$}, ngược lại rỗng. */
	public static Optional<String> chuanHoa(String raw) {
		String digits = boPhanCach(raw);
		return SO_DIEN_THOAI.matcher(digits).matches() ? Optional.of(digits) : Optional.empty();
	}

}
