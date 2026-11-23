package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Sau mọi luồng xác thực, không cột nào của nhật ký chứa mật khẩu, token hay hash (spec
 * 0007, AC-8 d).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class NhatKyBiMatIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void khongCotNaoChuaBiMat() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		NhatKyDb nhatKy = new NhatKyDb(this.jdbc);
		TaiKhoanThu taiKhoanThu = new TaiKhoanThu(this.jdbc);
		TaiKhoanThu.TaiKhoan tk = taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String truoc = nhatKy.maLogCuoi();
		List<String> biMat = new ArrayList<>(
				List.of(AuthApi.MAT_KHAU_SEED, "SaiMatKhau@77", taiKhoanThu.hash(tk.maNguoiDung())));

		this.mockMvc.perform(AuthApi.loginRequest(tk.soDienThoai(), "SaiMatKhau@77"))
			.andExpect(status().isUnauthorized());
		MvcResult login = this.mockMvc.perform(AuthApi.loginRequest(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED))
			.andExpect(status().isOk())
			.andReturn();
		String t1 = AuthApi.cookieValue(login, "refresh_token").orElseThrow();
		biMat.add(t1);
		biMat.add(AuthApi.cookieValue(login, "access_token").orElseThrow());
		MvcResult lamMoi = this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isOk()).andReturn();
		biMat.add(AuthApi.cookieValue(lamMoi, "refresh_token").orElseThrow());
		biMat.add(AuthApi.cookieValue(lamMoi, "access_token").orElseThrow());
		this.clock.advance(Duration.ofSeconds(31));
		this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isUnauthorized());

		String t3 = new AuthApi(this.mockMvc).refreshToken(tk.email());
		biMat.add(t3);
		this.mockMvc.perform(AuthApi.logoutRequest(t3)).andExpect(status().isOk());

		assertThat(nhatKy.loaiCua(tk.maNguoiDung())).containsExactly("AUTH_DANG_NHAP_THAT_BAI",
				"AUTH_DANG_NHAP_THANH_CONG", "AUTH_PHAT_HIEN_DUNG_LAI_TOKEN", "AUTH_DANG_NHAP_THANH_CONG",
				"AUTH_DANG_XUAT");
		String toanBo = nhatKy.toanBoSau(truoc);
		for (String s : biMat) {
			assertThat(toanBo).doesNotContain(s);
		}
	}

}
