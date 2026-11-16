package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Mọi dòng của bảng ví dụ ở mục Luồng đăng nhập, spec 0004 (AC-2). */
class TaiKhoanDangNhapTest {

	@ParameterizedTest(name = "\"{0}\" là email cudan@chungcu.test")
	@ValueSource(strings = { "  CuDan@ChungCu.Test ", "cudan@chungcu.test", " CUDAN@CHUNGCU.TEST\t" })
	void emailDuocCatKhoangTrangVaDoiChuThuong(String input) {
		assertThat(TaiKhoanDangNhap.parse(input)).contains(new TaiKhoanDangNhap.Email("cudan@chungcu.test"));
	}

	@ParameterizedTest(name = "\"{0}\" là 0900000007")
	@ValueSource(strings = { "0900000007", "090 000 0007", "090.000.0007", "090-000-0007", "+84 900 000 007",
			"+84900000007", "84900000007", " 0900000007 ", "090 000 0007" })
	void soDienThoaiDuocChuanHoa(String input) {
		assertThat(TaiKhoanDangNhap.parse(input)).contains(new TaiKhoanDangNhap.SoDienThoai("0900000007"));
	}

	@ParameterizedTest(name = "\"{0}\" không hợp lệ")
	@ValueSource(strings = { "0084900000007", "(090) 0000007", "+840900000007", "090000000", "abc", "", "   ",
			"09000000071", "849000000071" })
	void giaTriKhongRaSoDienThoaiHopLe(String input) {
		assertThat(TaiKhoanDangNhap.parse(input)).isEmpty();
	}

	@ParameterizedTest
	@NullSource
	void nullKhongHopLe(String input) {
		assertThat(TaiKhoanDangNhap.parse(input)).isEmpty();
	}

}
