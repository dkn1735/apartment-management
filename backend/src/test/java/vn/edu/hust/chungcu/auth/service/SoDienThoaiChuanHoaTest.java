package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Chuẩn hóa số điện thoại dùng chung cho đăng nhập và quản lý tài khoản (spec 0008,
 * AC-1).
 */
class SoDienThoaiChuanHoaTest {

	@ParameterizedTest(name = "\"{0}\" thành {1}")
	@CsvSource(delimiter = '|',
			value = { "0987 654 321|0987654321", "0987.654.321|0987654321", "0987-654-321|0987654321",
					"+84 987 654 321|0987654321", "+84987654321|0987654321", "84987654321|0987654321",
					"'  0987654321\t'|0987654321",
					// Khoảng trắng không ngắt (U+00A0) và khoảng trắng hẹp (U+202F) dán
					// từ Word hay web
					"0987\u00A0654\u202F321|0987654321" })
	void chuanHoaVeDangKhongCongChinChuSo(String raw, String ketQua) {
		assertThat(SoDienThoaiChuanHoa.chuanHoa(raw)).contains(ketQua);
	}

	@ParameterizedTest(name = "\"{0}\" không hợp lệ")
	@ValueSource(strings = { "", "   ", "098765432", "09876543210", "1987654321", "+840987654321", "849876543210",
			"0084987654321", "(098) 7654321", "098765432a" })
	void saiDangThiRong(String raw) {
		assertThat(SoDienThoaiChuanHoa.chuanHoa(raw)).isEmpty();
	}

	@Test
	void nullThiRongVaBoPhanCachRaChuoiRong() {
		assertThat(SoDienThoaiChuanHoa.chuanHoa(null)).isEmpty();
		assertThat(SoDienThoaiChuanHoa.boPhanCach(null)).isEmpty();
	}

	@Test
	void boPhanCachChuaKiemDinhDang() {
		// Đầu số 84 chỉ đổi khi chuỗi dài 11 ký tự, để không cắt nhầm số khác bắt đầu
		// bằng 84
		assertThat(SoDienThoaiChuanHoa.boPhanCach("8412")).isEqualTo("8412");
		assertThat(SoDienThoaiChuanHoa.boPhanCach("+84 12")).isEqualTo("012");
		assertThat(SoDienThoaiChuanHoa.boPhanCach("abc 12")).isEqualTo("abc12");
	}

}
