package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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
 * Dùng lại token vừa xoay trong khoảng khoan dung 30 giây: trả lại đúng token đang sống
 * kèm ba cookie, không ghi DB (spec 0005, AC-5); response làm mới bị mất không đăng xuất
 * người dùng (AC-17). Log không có token (AC-16).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class LamMoiPhienKhoanDungIntegrationTest {

	private static final long BAY_NGAY = 7 * 86400;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	RefreshTokenGenerator generator;

	AuthApi auth;

	PhienDb db;

	TaiKhoanThu taiKhoanThu;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.db = new PhienDb(this.jdbc);
		this.taiKhoanThu = new TaiKhoanThu(this.jdbc);
		this.tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	// AC-5: sau 5 giây và đúng 30 giây, T1 nhận lại T2 với ba cookie, DB không đổi
	@Test
	void trongKhoangKhoanDungTraLaiTokenDangSong(CapturedOutput output) throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		String t2 = lamMoi(t1);
		assertThat(t2).isEqualTo(this.generator.keTiep(t1));

		long daQua = 0;
		for (long giay : new long[] { 5, 25 }) {
			this.clock.advance(Duration.ofSeconds(giay));
			daQua += giay;
			MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Làm mới phiên thành công"))
				.andExpect(jsonPath("$.data.lamMoi").value(false))
				.andExpect(jsonPath("$.data.hoTen").value("Người Thử"))
				.andExpect(jsonPath("$.data.maNguoiDung").value(this.tk.maNguoiDung()))
				.andExpect(jsonPath("$.data.accessTokenHetHanLuc").isNotEmpty())
				.andReturn();
			assertThat(AuthApi.cookieValue(result, "refresh_token")).contains(t2);
			assertThat(AuthApi.cookieValue(result, "access_token")).isPresent();
			assertThat(AuthApi.setCookie(result, "refresh_token").orElseThrow())
				.contains("Max-Age=" + (BAY_NGAY - daQua) + ";");
			assertThat(AuthApi.setCookie(result, "phien").orElseThrow())
				.contains("Max-Age=" + (BAY_NGAY - daQua) + ";");
			assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "con");
		}
		assertThat(output.getAll()).doesNotContain("Phát hiện dùng lại").doesNotContain(t1).doesNotContain(t2);
	}

	// AC-5: T1 → T2 → T3 trong 30 giây, làm mới bằng T1 thì đi chuỗi 2 bước tới T3
	@Test
	void diChuoiHaiBuoc() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		String t3 = lamMoi(lamMoi(t1));
		this.clock.advance(Duration.ofSeconds(3));
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.lamMoi").value(false))
			.andReturn();
		assertThat(AuthApi.cookieValue(result, "refresh_token")).contains(t3);
	}

	// AC-5: dòng còn hiệu lực không suy ra được từ T1 thì 401, không WARN, có
	// KHONG_KHOP_CHUOI
	@Test
	void khongKhopChuoiThiTuChoi(CapturedOutput output) throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		String t2 = lamMoi(t1);
		this.jdbc.update("UPDATE phien_dang_nhap SET token_hash = ? WHERE token_hash = ?",
				RefreshTokenGenerator.sha256Hex(this.generator.moi()), RefreshTokenGenerator.sha256Hex(t2));

		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t1))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_INVALID"))
			.andReturn();
		LamMoiPhienIntegrationTest.assertBaCookieBiXoa(result);
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "con");
		assertThat(output.getAll()).contains("lyDo=KHONG_KHOP_CHUOI, maHoPhien=" + ho)
			.doesNotContain("Phát hiện dùng lại");
	}

	// AC-5: trong 30 giây mà họ vừa đăng xuất thì như AC-3
	@Test
	void khoanDungSauDangXuatThiTuChoi() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		String t2 = lamMoi(t1);
		this.mockMvc.perform(AuthApi.logoutRequest(t2)).andExpect(status().isOk());
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t1))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_INVALID"))
			.andReturn();
		LamMoiPhienIntegrationTest.assertBaCookieBiXoa(result);
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "Đăng xuất");
	}

	// AC-5 + AC-6: khớp chuỗi nhưng tài khoản bị khóa thì 403, cả họ bị thu hồi (đã
	// commit)
	@Test
	void khoanDungKhiTaiKhoanKhoaThuHoiHo() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		lamMoi(t1);
		this.taiKhoanThu.doiTrangThai(this.tk.maNguoiDung(), "Tạm khóa");
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t1))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"))
			.andReturn();
		LamMoiPhienIntegrationTest.assertBaCookieBiXoa(result);
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "Tài khoản bị khóa");
	}

	// Cờ khacThietBi chỉ để lại dấu vết, không chặn
	@Test
	void logCoKhacThietBi(CapturedOutput output) throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		lamMoi(t1);
		int truoc = output.getAll().length();
		this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isOk());
		assertThat(output.getAll().substring(truoc)).contains("khacThietBi=false");

		truoc = output.getAll().length();
		this.mockMvc.perform(AuthApi.refreshRequest(t1).with(r -> {
			r.setRemoteAddr("10.0.0.9");
			return r;
		})).andExpect(status().isOk());
		assertThat(output.getAll().substring(truoc)).contains("khacThietBi=true").doesNotContain("10.0.0.9");
	}

	// AC-17: trình duyệt không nhận Set-Cookie của lần xoay T1 → T2, vẫn giữ T1. Lần làm
	// mới sau bằng T1 nhận T2, rồi T2 xoay bình thường ra keTiep(T2)
	@Test
	void matResponseKhongDangXuatNguoiDung(CapturedOutput output) throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isOk());
		this.clock.advance(Duration.ofSeconds(3));

		MvcResult lai = this.mockMvc.perform(AuthApi.refreshRequest(t1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.lamMoi").value(false))
			.andReturn();
		String t2 = AuthApi.cookieValue(lai, "refresh_token").orElseThrow();

		MvcResult tiep = this.mockMvc.perform(AuthApi.refreshRequest(t2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.lamMoi").value(true))
			.andReturn();
		assertThat(AuthApi.cookieValue(tiep, "refresh_token")).contains(this.generator.keTiep(t2));
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "Đã xoay vòng", "con");
		assertThat(output.getAll()).doesNotContain("Phát hiện dùng lại");
	}

	private String lamMoi(String token) throws Exception {
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(token)).andExpect(status().isOk()).andReturn();
		return AuthApi.cookieValue(result, "refresh_token").orElseThrow();
	}

}
