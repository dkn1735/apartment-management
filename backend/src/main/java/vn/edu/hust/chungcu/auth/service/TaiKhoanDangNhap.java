package vn.edu.hust.chungcu.auth.service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Giá trị {@code taiKhoan} người dùng gõ, đã chuẩn hóa thành email hoặc số điện thoại
 * (spec 0004, AC-2 và bảng ví dụ ở mục Luồng đăng nhập).
 */
public sealed interface TaiKhoanDangNhap {

	/** Khoảng trắng ASCII và Unicode, dấu chấm, gạch nối: bỏ khỏi số điện thoại. */
	Pattern KY_TU_PHAN_CACH = Pattern.compile("[\\s\\p{Z}.\\-]");

	Pattern SO_DIEN_THOAI = Pattern.compile("^0[0-9]{9}$");

	record Email(String value) implements TaiKhoanDangNhap {
	}

	record SoDienThoai(String value) implements TaiKhoanDangNhap {
	}

	/** Chuẩn hóa; không ra được email hay số điện thoại hợp lệ thì rỗng. */
	static Optional<TaiKhoanDangNhap> parse(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		String value = raw.strip();
		if (value.isEmpty()) {
			return Optional.empty();
		}
		if (value.indexOf('@') >= 0) {
			return Optional.of(new Email(value.toLowerCase(Locale.ROOT)));
		}
		String digits = KY_TU_PHAN_CACH.matcher(value).replaceAll("");
		if (digits.startsWith("+84")) {
			digits = "0" + digits.substring(3);
		}
		else if (digits.startsWith("84") && digits.length() == 11) {
			digits = "0" + digits.substring(2);
		}
		return SO_DIEN_THOAI.matcher(digits).matches() ? Optional.of(new SoDienThoai(digits)) : Optional.empty();
	}

}
