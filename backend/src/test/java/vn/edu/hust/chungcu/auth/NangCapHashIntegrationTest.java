package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;

/** Nâng hash bcrypt cost 10 lên 12 sau lần đăng nhập đúng (spec 0004, AC-6). */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.bcrypt-cost=12")
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class NangCapHashIntegrationTest {

	private static final String HASH_COST_10 = "{bcrypt}" + new BCryptPasswordEncoder(10).encode(AuthApi.MAT_KHAU_SEED);

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	PasswordEncoder passwordEncoder;

	TaiKhoanThu taiKhoanThu;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.taiKhoanThu = new TaiKhoanThu(this.jdbc);
	}

	@AfterEach
	void tearDown() {
		reset(this.passwordEncoder);
	}

	@Test
	void hashCost10DuocNangLen12VaLanSauVanDangNhapDuoc() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(HASH_COST_10);

		this.mockMvc.perform(AuthApi.loginRequest(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED)).andExpect(status().isOk());
		String hashMoi = this.taiKhoanThu.hash(tk.maNguoiDung());
		assertThat(hashMoi).startsWith("{bcrypt}$2a$12$");

		this.mockMvc.perform(AuthApi.loginRequest(tk.email(), AuthApi.MAT_KHAU_SEED)).andExpect(status().isOk());
		assertThat(this.taiKhoanThu.hash(tk.maNguoiDung())).isEqualTo(hashMoi);
	}

	@Test
	void matKhauDoiGiuaChungThiKhongGhiDe() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(HASH_COST_10);
		String hashDoiGiuaChung = "{bcrypt}" + new BCryptPasswordEncoder(4).encode("MatKhauMoi@2026");
		// Giả lập tính năng đổi mật khẩu chạy xen giữa lúc so khớp và lúc ghi hash mới
		doAnswer(invocation -> {
			this.jdbc.update("UPDATE nguoi_dung_he_thong SET mat_khau_hash = ? WHERE ma_nguoi_dung = ?",
					hashDoiGiuaChung, tk.maNguoiDung());
			return invocation.callRealMethod();
		}).when(this.passwordEncoder).encode(any());

		this.mockMvc.perform(AuthApi.loginRequest(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED)).andExpect(status().isOk());
		assertThat(this.taiKhoanThu.hash(tk.maNguoiDung())).isEqualTo(hashDoiGiuaChung);
	}

}
