package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;

/**
 * Dùng lại token đã xoay quá khoảng khoan dung 30 giây, làm mới song song và khóa tài
 * khoản (spec 0005, AC-4, AC-5, AC-6, AC-16). Thu hồi kèm lỗi phải đã commit. Nhánh trong
 * khoảng khoan dung ở {@link LamMoiPhienKhoanDungIntegrationTest}.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class LamMoiPhienDungLaiIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	AuthApi auth;

	PhienDb db;

	TaiKhoanThu taiKhoanThu;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.db = new PhienDb(this.jdbc);
		this.taiKhoanThu = new TaiKhoanThu(this.jdbc);
		this.tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	// AC-4: dùng lại T1 quá 30 giây thì thu hồi cả họ, kể cả T2; log WARN không có token
	@Test
	void dungLaiQuaKhoangKhoanDungThuHoiCaHo(CapturedOutput output) throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		String t2 = lamMoi(t1);
		this.clock.advance(Duration.ofSeconds(31));

		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t1))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_INVALID"))
			.andExpect(jsonPath("$.message").value(LamMoiPhienIntegrationTest.HET_PHIEN))
			.andReturn();
		LamMoiPhienIntegrationTest.assertBaCookieBiXoa(result);
		// Đọc bằng kết nối khác: việc thu hồi đã commit dù request trả lỗi
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "Dùng lại token cũ");
		assertThat(this.db.soDongConHieuLuc(ho)).isZero();

		this.mockMvc.perform(AuthApi.refreshRequest(t2)).andExpect(status().isUnauthorized());
		assertThat(output.getAll()).contains("WARN")
			.contains("maHoPhien=" + ho)
			.contains("maNguoiDung=" + this.tk.maNguoiDung())
			.doesNotContain(t1)
			.doesNotContain(t2)
			.doesNotContain(RefreshTokenGenerator.sha256Hex(t1));
	}

	// AC-4: họ đã chết (đăng xuất) thì dùng lại chỉ bị từ chối, không log WARN, DB không
	// đổi
	@Test
	void dungLaiSauKhiHoDaChetThiChiTuChoi(CapturedOutput output) throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		String t2 = lamMoi(t1);
		this.mockMvc.perform(AuthApi.logoutRequest(t2)).andExpect(status().isOk());
		this.clock.advance(Duration.ofSeconds(31));
		int truoc = output.getAll().length();

		this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isUnauthorized());
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "Đăng xuất");
		assertThat(output.getAll().substring(truoc)).doesNotContain("Phát hiện dùng lại");
	}

	// AC-5: hai lần làm mới song song cùng token đều 200 với cùng cookie refresh_token,
	// đúng một dòng mới, họ không bị thu hồi
	@Test
	void haiLanLamMoiSongSongCungToken() throws Exception {
		for (int lan = 0; lan < 5; lan++) {
			String t1 = this.auth.refreshToken(this.tk.soDienThoai());
			UUID ho = this.db.hoPhien(t1);
			List<MvcResult> ketQua = AuthApi.songSong(this.mockMvc, AuthApi.refreshRequest(t1),
					AuthApi.refreshRequest(t1));
			assertThat(ketQua).allSatisfy(r -> assertThat(r.getResponse().getStatus()).isEqualTo(200));
			assertThat(ketQua.stream().map(r -> AuthApi.cookieValue(r, "refresh_token").orElseThrow()).distinct())
				.hasSize(1);
			assertThat(this.db.soDong(ho)).isEqualTo(2);
			assertThat(this.db.soDongConHieuLuc(ho)).isEqualTo(1);
		}
	}

	// AC-6: tài khoản bị khóa thì 403, xóa cookie, thu hồi cả họ (đã commit)
	@Test
	void taiKhoanBiKhoaThuHoiHo() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		String t2 = lamMoi(t1);
		this.taiKhoanThu.doiTrangThai(this.tk.maNguoiDung(), "Ngừng hoạt động");

		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t2))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"))
			.andReturn();
		LamMoiPhienIntegrationTest.assertBaCookieBiXoa(result);
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "Tài khoản bị khóa");
	}

	private String lamMoi(String token) throws Exception {
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(token)).andExpect(status().isOk()).andReturn();
		return AuthApi.cookieValue(result, "refresh_token").orElseThrow();
	}

}
