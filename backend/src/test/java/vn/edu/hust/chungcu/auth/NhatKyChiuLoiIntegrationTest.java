package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;
import vn.edu.hust.chungcu.nhatky.repository.NhatKyGhi;

/**
 * Ghi nhật ký lỗi thì thao tác chính vẫn trả đúng response như khi ghi thành công (spec
 * 0007, AC-7).
 */
@Import({ SeedTestcontainersConfiguration.class, NhatKyChiuLoiIntegrationTest.Cau.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class NhatKyChiuLoiIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	NhatKyGhiHong nhatKyGhi;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		this.nhatKyGhi.datHong(true);
	}

	@AfterEach
	void tearDown() {
		this.nhatKyGhi.datHong(false);
	}

	@Test
	void dangNhapVanThanhCongKhiGhiNhatKyLoi(CapturedOutput output) throws Exception {
		String requestId = "nk-" + UUID.randomUUID();
		MvcResult result = this.mockMvc
			.perform(AuthApi.loginRequest(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED)
				.header("X-Request-Id", requestId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.maNguoiDung").value(this.tk.maNguoiDung()))
			.andReturn();
		assertThat(AuthApi.cookieValue(result, "access_token")).isPresent();
		assertThat(AuthApi.cookieValue(result, "refresh_token")).isPresent();
		assertThat(new NhatKyDb(this.jdbc).theoRequest(requestId)).isEmpty();
		assertThat(output.getAll()).contains("ERROR")
			.contains("Ghi nhật ký thất bại: loaiThaoTac=AUTH_DANG_NHAP_THANH_CONG")
			.contains("requestId=" + requestId)
			.contains(DataAccessResourceFailureException.class.getName());
	}

	@Test
	void dangXuatVanThanhCongKhiGhiSauCommitLoi(CapturedOutput output) throws Exception {
		this.nhatKyGhi.datHong(false);
		String token = new AuthApi(this.mockMvc).refreshToken(this.tk.soDienThoai());
		this.nhatKyGhi.datHong(true);
		this.mockMvc.perform(AuthApi.logoutRequest(token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Đăng xuất thành công"));
		assertThat(new PhienDb(this.jdbc).lyDo(new PhienDb(this.jdbc).hoPhien(token))).containsExactly("Đăng xuất");
		assertThat(output.getAll()).contains("Ghi nhật ký thất bại: loaiThaoTac=AUTH_DANG_XUAT");
	}

	@Test
	void dangNhapSaiVanTraDungLoiKhiGhiNhatKyLoi() throws Exception {
		this.mockMvc.perform(AuthApi.loginRequest(this.tk.soDienThoai(), "SaiMatKhau@1"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
	}

	/** Lớp ghi bật tắt được lỗi, thay bean thật trong context của test này. */
	static class NhatKyGhiHong extends NhatKyGhi {

		// Bean bị bọc proxy (@Repository của lớp cha), nên chỉ đổi qua hàm, không qua
		// field
		private volatile boolean hong;

		NhatKyGhiHong(JdbcClient jdbc) {
			super(jdbc);
		}

		public void datHong(boolean value) {
			this.hong = value;
		}

		@Override
		public void them(DongNhatKy dong) {
			if (this.hong) {
				throw new DataAccessResourceFailureException("Mất kết nối giả");
			}
			super.them(dong);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class Cau {

		@Bean
		@Primary
		NhatKyGhiHong nhatKyGhiHong(JdbcClient jdbc) {
			return new NhatKyGhiHong(jdbc);
		}

	}

}
