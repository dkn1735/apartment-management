package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Token dùng một lần, có hạn, mọi token hỏng cùng một lỗi (spec 0010, AC-6, AC-8, AC-9).
 */
class QuenMatKhauTokenIntegrationTest extends QuenMatKhauTestBase {

	@MockitoSpyBean
	PasswordEncoder encoderSpy;

	/** Body lỗi của cả check và confirm với một token không dùng được. */
	List<String> haiLoi(String token) throws Exception {
		List<String> body = new ArrayList<>();
		for (MockHttpServletRequestBuilder r : List.of(QuenMatKhauThu.kiemTra(token),
				QuenMatKhauThu.xacNhan(token, MAT_KHAU_MOI))) {
			MvcResult result = this.mockMvc.perform(r)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("AUTH_RESET_TOKEN_INVALID"))
				.andExpect(jsonPath("$.message").value(QuenMatKhauThu.TOKEN_HONG))
				.andReturn();
			body.add(result.getResponse().getContentAsString().replaceAll("\"(requestId|timestamp)\":\"[^\"]*\"", ""));
		}
		return body;
	}

	@ParameterizedTest
	@ValueSource(strings = { "ngan", "co dau cach va ky tu la !!!!!!!!!!!!!!!!!!!!!!",
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"qua-dai-hon-64-ky-tu-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" })
	void tokenSaiDinhDangHoacKhongCoTraCungLoiVaKhongChayBcrypt(String token) throws Exception {
		clearInvocations(this.encoderSpy);
		haiLoi(token);
		verify(this.encoderSpy, never()).encode(any());
	}

	@Test
	void hetHanDaDungDaHuyVaTaiKhoanBiKhoaDeuCungMotBody() throws Exception {
		String hetHan = yeuCauVaLayToken(1);
		this.clock.advance(Duration.ofMinutes(30));
		List<String> mau = haiLoi(hetHan);

		this.clock.advance(Duration.ofSeconds(61));
		String daDung = yeuCauVaLayToken(2);
		this.mockMvc.perform(QuenMatKhauThu.xacNhan(daDung, MAT_KHAU_MOI)).andExpect(status().isOk());
		assertThat(haiLoi(daDung)).isEqualTo(mau);

		this.clock.advance(Duration.ofSeconds(61));
		String daHuy = yeuCauVaLayToken(3);
		this.clock.advance(Duration.ofSeconds(61));
		yeuCauVaLayToken(4);
		assertThat(haiLoi(daHuy)).isEqualTo(mau);

		this.clock.advance(Duration.ofSeconds(61));
		String biKhoa = yeuCauVaLayToken(5);
		this.taiKhoan.doiTrangThai(this.tk.maNguoiDung(), "Tạm khóa");
		clearInvocations(this.encoderSpy);
		assertThat(haiLoi(biKhoa)).isEqualTo(mau);
		verify(this.encoderSpy, never()).encode(any());
	}

	@Test
	void hetHanTheoClock() throws Exception {
		String token = yeuCauVaLayToken(1);
		this.clock.advance(Duration.ofMinutes(30).minusSeconds(1));
		this.mockMvc.perform(QuenMatKhauThu.kiemTra(token)).andExpect(status().isOk());
		this.clock.advance(Duration.ofSeconds(1));
		haiLoi(token);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Ngan123", "chichucaithoi", "12345678901", "" })
	void matKhauSaiQuyTacTra400VaKhongTieuToken(String matKhau) throws Exception {
		String token = yeuCauVaLayToken(1);
		this.mockMvc.perform(QuenMatKhauThu.xacNhan(token, matKhau))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("matKhauMoi"));
		this.mockMvc.perform(QuenMatKhauThu.xacNhan(token, MAT_KHAU_MOI)).andExpect(status().isOk());
	}

	@Test
	void matKhauQua72ByteTra400VaKhongTieuToken() throws Exception {
		String token = yeuCauVaLayToken(1);
		// 24 chữ "ệ" là 72 byte, thêm "1" là 73
		this.mockMvc.perform(QuenMatKhauThu.xacNhan(token, "ệ".repeat(24) + "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("matKhauMoi"));
		this.mockMvc.perform(QuenMatKhauThu.kiemTra(token)).andExpect(status().isOk());
	}

	@Test
	void matKhauSaiQuyTacCongTokenHongVanLaValidationFailed() throws Exception {
		this.mockMvc.perform(QuenMatKhauThu.xacNhan("hong", "ngan"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
	}

	@Test
	void trungMatKhauHienTaiVanDuoc() throws Exception {
		String token = yeuCauVaLayToken(1);
		this.mockMvc.perform(QuenMatKhauThu.xacNhan(token, AuthApi.MAT_KHAU_SEED)).andExpect(status().isOk());
		assertThat(this.auth.login(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED).getResponse().getStatus())
			.isEqualTo(200);
	}

	@Test
	void haiConfirmSongSongChoDungMotThanhCong() throws Exception {
		String token = yeuCauVaLayToken(1);
		List<MvcResult> ketQua = AuthApi.songSong(this.mockMvc, QuenMatKhauThu.xacNhan(token, "MatKhauA2026"),
				QuenMatKhauThu.xacNhan(token, "MatKhauB2026"));
		List<Integer> status = ketQua.stream().map(r -> r.getResponse().getStatus()).toList();
		assertThat(status).containsExactlyInAnyOrder(200, 400);
		String thang = (status.get(0) == 200) ? "MatKhauA2026" : "MatKhauB2026";
		String thua = (status.get(0) == 200) ? "MatKhauB2026" : "MatKhauA2026";
		assertThat(ketQua.get(status.indexOf(400)).getResponse().getContentAsString())
			.contains("AUTH_RESET_TOKEN_INVALID");
		assertThat(this.auth.login(this.tk.soDienThoai(), thang).getResponse().getStatus()).isEqualTo(200);
		assertThat(this.auth.login(this.tk.soDienThoai(), thua).getResponse().getStatus()).isEqualTo(401);
	}

}
