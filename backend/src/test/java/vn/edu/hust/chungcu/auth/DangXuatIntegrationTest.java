package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;

/** Đăng xuất của spec 0005 (AC-7, AC-8, AC-9). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class DangXuatIntegrationTest {

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

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.db = new PhienDb(this.jdbc);
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	// AC-8: chỉ thu hồi họ của máy này; đăng xuất lại hay không cookie vẫn 200, DB không
	// đổi; làm mới sau đó bị từ chối
	@Test
	void dangXuatChiThuHoiHoHienTai() throws Exception {
		String mayA = this.auth.refreshToken(this.tk.soDienThoai());
		String mayB = this.auth.refreshToken(this.tk.soDienThoai());
		UUID hoA = this.db.hoPhien(mayA);
		UUID hoB = this.db.hoPhien(mayB);

		MvcResult result = this.mockMvc.perform(AuthApi.logoutRequest(mayA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Đăng xuất thành công"))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andReturn();
		LamMoiPhienIntegrationTest.assertBaCookieBiXoa(result);
		assertThat(this.db.lyDo(hoA)).containsExactly("Đăng xuất");
		assertThat(this.db.dong(mayA).get("thu_hoi_luc")).isNotNull();
		assertThat(PhienDb.instant(this.db.dong(mayA).get("thu_hoi_luc"))).isEqualTo(MutableClock.MAC_DINH);
		assertThat(this.db.lyDo(hoB)).containsExactly("con");

		Object thuHoiLuc = this.db.dong(mayA).get("thu_hoi_luc");
		this.clock.advance(java.time.Duration.ofMinutes(1));
		for (String token : new String[] { mayA, null, "rac-khong-co-trong-db" }) {
			MvcResult lai = this.mockMvc.perform(AuthApi.logoutRequest(token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Đăng xuất thành công"))
				.andReturn();
			LamMoiPhienIntegrationTest.assertBaCookieBiXoa(lai);
		}
		assertThat(this.db.dong(mayA).get("thu_hoi_luc")).isEqualTo(thuHoiLuc);

		this.mockMvc.perform(AuthApi.refreshRequest(mayA))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_INVALID"));
	}

	// AC-8: đăng xuất bằng token đã xoay vẫn thu hồi cả họ
	@Test
	void dangXuatBangTokenDaXoay() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.db.hoPhien(t1);
		this.auth.refresh(t1);
		this.mockMvc.perform(AuthApi.logoutRequest(t1)).andExpect(status().isOk());
		assertThat(this.db.lyDo(ho)).containsExactly("Đã xoay vòng", "Đăng xuất");
	}

	// AC-7: access token rác không chặn đăng xuất; thiếu header chống CSRF thì 403
	@Test
	void dangXuatLaDuongCongKhaiNhungCanHeader() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		this.mockMvc
			.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/logout")
				.cookie(new Cookie("refresh_token", t1)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("CSRF_REJECTED"));
		this.mockMvc.perform(AuthApi.logoutRequest(t1).cookie(new Cookie("access_token", "rac")))
			.andExpect(status().isOk());
	}

	// AC-8, AC-9: đăng xuất bằng T1 chạy song song làm mới bằng T2: sau cả hai, họ không
	// còn dòng nào chưa thu hồi
	@Test
	void dangXuatSongSongLamMoiKhongDeLaiDongSong() throws Exception {
		for (int lan = 0; lan < 10; lan++) {
			String t1 = this.auth.refreshToken(this.tk.soDienThoai());
			UUID ho = this.db.hoPhien(t1);
			String t2 = AuthApi.cookieValue(this.auth.refresh(t1), "refresh_token").orElseThrow();
			AuthApi.songSong(this.mockMvc, AuthApi.logoutRequest(t1), AuthApi.refreshRequest(t2));
			assertThat(this.db.soDongConHieuLuc(ho)).as("lần %d", lan).isZero();
		}
	}

}
