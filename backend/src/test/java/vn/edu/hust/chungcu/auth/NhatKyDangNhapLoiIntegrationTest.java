package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.auth.service.AccessTokenService;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Đăng nhập hỏng sau khi đã tạo phiên thì không có dòng thành công (spec 0007, AC-1). Lớp
 * riêng vì spy bean tạo một Spring context khác.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class NhatKyDangNhapLoiIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@MockitoSpyBean
	VaiTroQuery vaiTroQuery;

	@MockitoSpyBean
	AccessTokenService accessTokens;

	NhatKyDb nhatKy;

	TaiKhoanThu.TaiKhoan tk;

	String requestId;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		this.requestId = "nk-" + UUID.randomUUID();
	}

	// AC-1: tra vai trò lỗi (500) thì không có dòng đăng nhập thành công
	@Test
	void traVaiTroLoiThiKhongGhiThanhCong() throws Exception {
		doThrow(new IllegalStateException("tra vai trò hỏng")).when(this.vaiTroQuery).cua(this.tk.maNguoiDung());

		dangNhapTra500();

		assertThat(this.nhatKy.theoRequest(this.requestId)).isEmpty();
		assertThat(this.nhatKy.loaiCua(this.tk.maNguoiDung())).doesNotContain("AUTH_DANG_NHAP_THANH_CONG");
	}

	// AC-1: cấp access token lỗi (500) thì không có dòng đăng nhập thành công
	@Test
	void capTokenLoiThiKhongGhiThanhCong() throws Exception {
		doThrow(new IllegalStateException("cấp token hỏng")).when(this.accessTokens)
			.cap(eq(this.tk.maNguoiDung()), any(), any(), any());

		dangNhapTra500();

		assertThat(this.nhatKy.theoRequest(this.requestId)).isEmpty();
		assertThat(this.nhatKy.loaiCua(this.tk.maNguoiDung())).doesNotContain("AUTH_DANG_NHAP_THANH_CONG");
	}

	private void dangNhapTra500() throws Exception {
		this.mockMvc
			.perform(AuthApi.loginRequest(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED)
				.header("X-Request-Id", this.requestId))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
	}

}
