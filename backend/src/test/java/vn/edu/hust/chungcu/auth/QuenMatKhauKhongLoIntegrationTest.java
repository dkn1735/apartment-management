package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Trang quên mật khẩu không cho biết email nào có tài khoản (spec 0010, AC-2): mọi trường
 * hợp hợp lệ về định dạng cho cùng status, body và header.
 */
class QuenMatKhauKhongLoIntegrationTest extends QuenMatKhauTestBase {

	@Test
	void bonTruongHopChoCungPhanHoiChiTruongHopDauCoEmail() throws Exception {
		TaiKhoanThu.TaiKhoan khoa = this.taiKhoan.tao(this.passwordEncoder.encode("x"), "Tạm khóa");
		TaiKhoanThu.TaiKhoan ngung = this.taiKhoan.tao(this.passwordEncoder.encode("x"), "Ngừng hoạt động");

		Map<String, Object> coTaiKhoan = phanHoi(goi(this.tk.email()));
		QuenMatKhauThu.choEmail(this.greenMail, 1);
		Map<String, Object> khongCo = phanHoi(goi("khong-ai-dung@chungcu.test"));
		Map<String, Object> tamKhoa = phanHoi(goi(khoa.email()));
		Map<String, Object> ngungHoatDong = phanHoi(goi(ngung.email()));
		// Ngay sau yêu cầu đầu: bị giới hạn 60 giây
		Map<String, Object> gioiHan = phanHoi(goi(this.tk.email()));

		assertThat(coTaiKhoan.get("status")).isEqualTo(200);
		assertThat(khongCo).isEqualTo(coTaiKhoan);
		assertThat(tamKhoa).isEqualTo(coTaiKhoan);
		assertThat(ngungHoatDong).isEqualTo(coTaiKhoan);
		assertThat(gioiHan).isEqualTo(coTaiKhoan);

		// Chờ thêm để chắc listener không gửi email muộn
		Thread.sleep(500);
		assertThat(this.greenMail.getReceivedMessages()).hasSize(1);
		assertThat(soYeuCau()).isEqualTo(1);
		assertThat(QuenMatKhauThu.yeuCauCua(this.jdbc, khoa.maNguoiDung())).isEmpty();
		assertThat(QuenMatKhauThu.yeuCauCua(this.jdbc, ngung.maNguoiDung())).isEmpty();
	}

	@Test
	void emailVietHoaCoKhoangTrangVanKhop() throws Exception {
		String email = "  " + this.tk.email().toUpperCase(java.util.Locale.ROOT) + " ";
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(email)).andExpect(status().isOk());
		assertThat(QuenMatKhauThu.choEmail(this.greenMail, 1)).hasSize(1);
		assertThat(soYeuCau()).isEqualTo(1);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "khong-phai-email", "a@b", "co khoang@trang.vn" })
	void emailSaiDinhDangTra400OEmail(String email) throws Exception {
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(email))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("email"));
	}

	@Test
	void emailDaiHon150KyTuTra400NhuNhauDuCoTaiKhoanHayKhong() throws Exception {
		String dai = "a".repeat(140) + "@chungcu.test";
		MvcResult result = goi(dai);
		assertThat(result.getResponse().getStatus()).isEqualTo(400);
		assertThat(result.getResponse().getContentAsString()).contains("\"field\":\"email\"");
	}

	private MvcResult goi(String email) throws Exception {
		return this.mockMvc.perform(QuenMatKhauThu.yeuCau(email)).andReturn();
	}

}
