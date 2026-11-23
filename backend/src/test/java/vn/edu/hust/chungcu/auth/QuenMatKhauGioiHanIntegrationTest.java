package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import vn.edu.hust.chungcu.MutableClock;

/** Giới hạn gửi và một link mở cho mỗi tài khoản (spec 0010, AC-4, AC-5). */
@ExtendWith(OutputCaptureExtension.class)
class QuenMatKhauGioiHanIntegrationTest extends QuenMatKhauTestBase {

	@Test
	void haiYeuCauCach30GiayChiMotEmail() throws Exception {
		yeuCauVaLayToken(1);
		this.clock.advance(Duration.ofSeconds(30));
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email())).andExpect(status().isOk());
		Thread.sleep(500);
		assertThat(this.greenMail.getReceivedMessages()).hasSize(1);
		assertThat(soYeuCau()).isEqualTo(1);
	}

	@Test
	void sauYeuCauCach61GiayTrongMotGioChiNamDong() throws Exception {
		for (int i = 0; i < 6; i++) {
			this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email())).andExpect(status().isOk());
			this.clock.advance(Duration.ofSeconds(61));
		}
		assertThat(soYeuCau()).isEqualTo(5);
		QuenMatKhauThu.choEmail(this.greenMail, 5);
		// Qua một giờ kể từ dòng đầu thì được yêu cầu tiếp
		this.clock.set(MutableClock.MAC_DINH.plus(Duration.ofMinutes(61)));
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email())).andExpect(status().isOk());
		assertThat(soYeuCau()).isEqualTo(6);
	}

	@Test
	void yeuCauMoiHuyLinkCu(CapturedOutput output) throws Exception {
		String cu = yeuCauVaLayToken(1);
		this.clock.advance(Duration.ofSeconds(61));
		String moi = yeuCauVaLayToken(2);

		List<Map<String, Object>> dong = QuenMatKhauThu.yeuCauCua(this.jdbc, this.tk.maNguoiDung());
		assertThat(dong).hasSize(2);
		assertThat(dong.get(0).get("huy_luc")).isNotNull();
		assertThat(dong.get(1).get("huy_luc")).isNull();
		this.mockMvc.perform(QuenMatKhauThu.kiemTra(cu))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("AUTH_RESET_TOKEN_INVALID"));
		this.mockMvc.perform(QuenMatKhauThu.kiemTra(moi)).andExpect(status().isOk());
		assertThat(QuenMatKhauThu.logUngDung(output.getAll())).doesNotContain(cu)
			.doesNotContain(moi)
			.doesNotContain(this.tk.email());
	}

	@Test
	void dbTuChoiDongMoThuHai() throws Exception {
		yeuCauVaLayToken(1);
		assertThatThrownBy(() -> this.jdbc.update("""
				INSERT INTO yeu_cau_dat_lai_mat_khau (ma_nguoi_dung, token_hash, tao_luc, het_han_luc)
				VALUES (?, ?, now(), now() + interval '30 minutes')""", this.tk.maNguoiDung(), "a".repeat(64)))
			.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void muoiYeuCauSongSongChoDungMotDongVaMotEmail() throws Exception {
		RequestBuilder[] requests = new RequestBuilder[10];
		for (int i = 0; i < requests.length; i++) {
			requests[i] = QuenMatKhauThu.yeuCau(this.tk.email());
		}
		List<MvcResult> ketQua = AuthApi.songSong(this.mockMvc, requests);
		assertThat(ketQua).allSatisfy(r -> assertThat(r.getResponse().getStatus()).isEqualTo(200));
		assertThat(soYeuCau()).isEqualTo(1);
		QuenMatKhauThu.choEmail(this.greenMail, 1);
		Thread.sleep(500);
		assertThat(this.greenMail.getReceivedMessages()).hasSize(1);
	}

	@Test
	void tranChungMotTramMoiGio(CapturedOutput output) throws Exception {
		TaiKhoanThu.TaiKhoan khac = this.taiKhoan.tao(this.passwordEncoder.encode("x"));
		Timestamp trongGio = Timestamp.from(MutableClock.MAC_DINH.minus(Duration.ofMinutes(30)));
		for (int i = 0; i < 100; i++) {
			// Dòng đã hủy để không vướng unique index một phần
			this.jdbc.update("""
					INSERT INTO yeu_cau_dat_lai_mat_khau (ma_nguoi_dung, token_hash, tao_luc, het_han_luc, huy_luc)
					VALUES (?, ?, ?, ?, ?)""", khac.maNguoiDung(), String.format("%064x", i), trongGio,
					Timestamp.from(trongGio.toInstant().plus(Duration.ofMinutes(30))), trongGio);
		}
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(QuenMatKhauThu.PHAN_HOI_CHUNG));
		assertThat(soYeuCau()).isZero();
		assertThat(output.getAll()).contains("GIOI_HAN_CHUNG");
	}

}
