package vn.edu.hust.chungcu.nhatky.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Bảng ví dụ của quy tắc tên khóa nhạy cảm (spec 0007, AC-8 b). */
class TenKhoaNhayCamTest {

	@ParameterizedTest
	@ValueSource(strings = { "refreshToken", "matKhauMoi", "passwordHash", "password", "token", "tokens",
			"accessTokens", "hash", "clientSecret", "cookie", "otp", "maOtp", "matkhau", "mat_khau", "MAT_KHAU_CU",
			"refresh_token", "JWTToken", "xacNhanMatKhau" })
	void tenNhayCam(String ten) {
		assertThat(TenKhoaNhayCam.nhayCam(ten)).as(ten).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "maHoPhien", "footprint", "lyDo", "soPhienThuHoi", "matTien", "khauTru", "hashtag",
			"tokenizer", "truoc", "sau", "trangThaiTaiKhoan" })
	void tenKhongNhayCam(String ten) {
		assertThat(TenKhoaNhayCam.nhayCam(ten)).as(ten).isFalse();
	}

}
