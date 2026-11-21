package vn.edu.hust.chungcu.auth.service;

import java.util.Locale;
import java.util.Optional;

/**
 * Giá trị {@code taiKhoan} người dùng gõ, đã chuẩn hóa thành email hoặc số điện thoại
 * (spec 0004, AC-2 và bảng ví dụ ở mục Luồng đăng nhập).
 */
public sealed interface TaiKhoanDangNhap {

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
		return SoDienThoaiChuanHoa.chuanHoa(value).map(SoDienThoai::new);
	}

}
