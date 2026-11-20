package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;

/**
 * Đường làm mới của spec 0005: xoay vòng, trần 30 ngày, token không hợp lệ, đường công
 * khai (AC-1, AC-2, AC-3, AC-7, AC-9, AC-16). Mỗi test dùng tài khoản riêng.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class LamMoiPhienIntegrationTest {

	static final String HET_PHIEN = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	JwtDecoder jwtDecoder;

	AuthApi auth;

	PhienDb db;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.db = new PhienDb(this.jdbc);
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	// AC-1
	@Test
	void lamMoiXoayVongVaDatBaCookie() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		Map<String, Object> dongCu = this.db.dong(t1);
		this.clock.advance(Duration.ofMinutes(20));

		MvcResult result = this.mockMvc
			.perform(AuthApi.refreshRequest(t1).header("User-Agent", "TrinhDuyet/2").with(r -> {
				r.setRemoteAddr("10.0.0.9");
				return r;
			}))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Làm mới phiên thành công"))
			.andExpect(jsonPath("$.data.lamMoi").value(true))
			.andExpect(jsonPath("$.data.maNguoiDung").value(this.tk.maNguoiDung()))
			.andExpect(jsonPath("$.data.hoTen").value("Người Thử"))
			.andExpect(jsonPath("$.data.accessTokenHetHanLuc").value("2026-11-16T01:35:00Z"))
			.andReturn();

		assertThat(AuthApi.setCookies(result)).hasSize(3);
		String t2 = AuthApi.cookieValue(result, "refresh_token").orElseThrow();
		String access = AuthApi.cookieValue(result, "access_token").orElseThrow();
		String body = result.getResponse().getContentAsString();
		assertThat(body).doesNotContain(t2).doesNotContain(access).doesNotContain(t1);

		Map<String, Object> cu = this.db.dong(t1);
		assertThat(cu.get("ly_do_thu_hoi")).isEqualTo("Đã xoay vòng");
		assertThat(PhienDb.instant(cu.get("thu_hoi_luc"))).isEqualTo(this.clock.instant());
		Map<String, Object> moi = this.db.dong(t2);
		assertThat(moi.get("ma_ho_phien")).isEqualTo(dongCu.get("ma_ho_phien"));
		assertThat(moi.get("bat_dau_luc")).isEqualTo(dongCu.get("bat_dau_luc"));
		assertThat(PhienDb.instant(moi.get("tao_luc"))).isEqualTo(this.clock.instant());
		assertThat(moi.get("token_hash")).isEqualTo(RefreshTokenGenerator.sha256Hex(t2));
		assertThat(moi.get("dia_chi_ip")).isEqualTo("10.0.0.9");
		assertThat(moi.get("user_agent")).isEqualTo("TrinhDuyet/2");
		assertThat(this.db.soDong((UUID) dongCu.get("ma_ho_phien"))).isEqualTo(2);

		Jwt jwt = this.jwtDecoder.decode(access);
		assertThat(jwt.getClaimAsString("sid")).isEqualTo(dongCu.get("ma_ho_phien").toString());
		assertThat(jwt.getSubject()).isEqualTo(this.tk.maNguoiDung());
	}

	// AC-1: vai trò đọc lại từ DB ở mỗi lần làm mới
	@Test
	void lamMoiDocLaiVaiTro() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		this.jdbc.update("INSERT INTO quan_tri_vien (ma_nguoi_dung) VALUES (?)", this.tk.maNguoiDung());

		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(t1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.vaiTro[0]").value("QUAN_TRI_VIEN"))
			.andReturn();
		Jwt jwt = this.jwtDecoder.decode(AuthApi.cookieValue(result, "access_token").orElseThrow());
		assertThat(jwt.getClaimAsStringList("vaiTro")).containsExactly("QUAN_TRI_VIEN");
	}

	// AC-2: phiên trượt 7 ngày, trần 30 ngày kể từ lần đăng nhập đầu
	@Test
	void phienTruotBayNgayVaTranBaMuoiNgay() throws Exception {
		Instant t = MutableClock.MAC_DINH;
		String token = this.auth.refreshToken(this.tk.soDienThoai());

		token = lamMoiLuc(token, t.plus(Duration.ofDays(6)), t.plus(Duration.ofDays(13)), 7 * 86400);
		token = lamMoiLuc(token, t.plus(Duration.ofDays(12)), t.plus(Duration.ofDays(19)), 7 * 86400);
		token = lamMoiLuc(token, t.plus(Duration.ofDays(18)), t.plus(Duration.ofDays(25)), 7 * 86400);
		token = lamMoiLuc(token, t.plus(Duration.ofDays(24)), t.plus(Duration.ofDays(30)), 6 * 86400);
		token = lamMoiLuc(token, t.plus(Duration.ofDays(29)), t.plus(Duration.ofDays(30)), 86400);

		this.clock.set(t.plus(Duration.ofDays(30)).plusSeconds(1));
		this.mockMvc.perform(AuthApi.refreshRequest(token))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_INVALID"));
	}

	// AC-2: giờ có phần nano giây, còn nửa giây tới hạn: không lỗi CHECK, Max-Age là 1
	@Test
	void conNuaGiayThiMaxAgeLaMot() throws Exception {
		Instant t = MutableClock.MAC_DINH.plusNanos(123_456_789);
		this.clock.set(t);
		String token = this.auth.refreshToken(this.tk.soDienThoai());
		Instant tMicro = t.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
		for (int ngay = 6; ngay <= 24; ngay += 6) {
			this.clock.set(t.plus(Duration.ofDays(ngay)));
			token = AuthApi.cookieValue(this.auth.refresh(token), "refresh_token").orElseThrow();
		}
		this.clock.set(tMicro.plus(Duration.ofDays(30)).minusMillis(500));
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(token)).andExpect(status().isOk()).andReturn();
		assertThat(AuthApi.setCookie(result, "refresh_token").orElseThrow()).contains("Max-Age=1;");
		assertThat(AuthApi.setCookie(result, "phien").orElseThrow()).contains("Max-Age=1;");
	}

	// AC-3: mọi nguyên nhân cùng mã, cùng câu, xóa ba cookie, không thêm dòng. Cookie
	// 42 ký tự hay có ký tự ngoài base64url bị chặn trước khi đọc DB
	@Test
	void tokenKhongHopLeDeuTraCungMotLoi() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		String tHetHan = this.auth.refreshToken(this.tk.soDienThoai());
		String tDangXuat = this.auth.refreshToken(this.tk.soDienThoai());
		this.jdbc.update("UPDATE phien_dang_nhap SET thu_hoi_luc = ?, ly_do_thu_hoi = 'Đăng xuất' WHERE token_hash = ?",
				java.sql.Timestamp.from(this.clock.instant()), RefreshTokenGenerator.sha256Hex(tDangXuat));
		this.clock.advance(Duration.ofDays(8));
		this.auth.refreshToken(this.tk.soDienThoai());
		int truoc = this.db.soDongCua(this.tk.maNguoiDung());

		for (String token : new String[] { null, "", "rac-khong-co-trong-db", "a".repeat(42), "a".repeat(42) + "!",
				"A".repeat(43), tHetHan, tDangXuat, t1 }) {
			MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_INVALID"))
				.andExpect(jsonPath("$.message").value(HET_PHIEN))
				.andReturn();
			assertBaCookieBiXoa(result);
		}
		assertThat(this.db.soDongCua(this.tk.maNguoiDung())).isEqualTo(truoc);
	}

	// AC-7: access token rác hay hết hạn không chặn làm mới; vẫn cần header chống CSRF
	@Test
	void duongCongKhaiNhungVanCanHeaderChongCsrf() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		this.mockMvc.perform(AuthApi.refreshRequest(t1).cookie(new Cookie("access_token", "rac.rac.rac")))
			.andExpect(status().isOk());
		this.mockMvc
			.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/v1/auth/refresh-token")
				.cookie(new Cookie("refresh_token", t1)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("CSRF_REJECTED"));
	}

	// AC-9: đăng nhập ghi bat_dau_luc = tao_luc; DB chặn bat_dau_luc > tao_luc và hai
	// dòng
	// chưa thu hồi cùng họ
	@Test
	void rangBuocCuaBatDauLucVaIndexMotDongMoiHo() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		Map<String, Object> dong = this.db.dong(t1);
		assertThat(dong.get("bat_dau_luc")).isEqualTo(dong.get("tao_luc"));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> this.jdbc.update(
				"UPDATE phien_dang_nhap SET bat_dau_luc = tao_luc + interval '1 second' WHERE token_hash = ?",
				RefreshTokenGenerator.sha256Hex(t1)))
			.hasMessageContaining("chk_phien_dang_nhap_bat_dau");
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> this.jdbc.update("""
				INSERT INTO phien_dang_nhap (ma_nguoi_dung, ma_ho_phien, token_hash, bat_dau_luc, tao_luc, het_han_luc)
				SELECT ma_nguoi_dung, ma_ho_phien, ?, bat_dau_luc, tao_luc, het_han_luc
				FROM phien_dang_nhap WHERE token_hash = ?""", "f".repeat(64), RefreshTokenGenerator.sha256Hex(t1)))
			.hasMessageContaining("uq_phien_dang_nhap_ho_con_hieu_luc");
	}

	// AC-16
	@Test
	void logKhongChuaToken(CapturedOutput output) throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		MvcResult result = this.auth.refresh(t1);
		String t2 = AuthApi.cookieValue(result, "refresh_token").orElseThrow();
		String access = AuthApi.cookieValue(result, "access_token").orElseThrow();
		this.auth.refresh("rac-khong-co-trong-db");
		assertThat(output.getAll()).doesNotContain(t1)
			.doesNotContain(t2)
			.doesNotContain(access)
			.doesNotContain(RefreshTokenGenerator.sha256Hex(t1))
			.doesNotContain(RefreshTokenGenerator.sha256Hex(t2));
	}

	private String lamMoiLuc(String token, Instant luc, Instant hetHanMongDoi, long maxAge) throws Exception {
		this.clock.set(luc);
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(token)).andExpect(status().isOk()).andReturn();
		String moi = AuthApi.cookieValue(result, "refresh_token").orElseThrow();
		assertThat(PhienDb.instant(this.db.dong(moi).get("het_han_luc"))).isEqualTo(hetHanMongDoi);
		assertThat(AuthApi.setCookie(result, "refresh_token").orElseThrow()).contains("Max-Age=" + maxAge + ";");
		assertThat(AuthApi.setCookie(result, "phien").orElseThrow()).contains("Max-Age=" + maxAge + ";");
		return moi;
	}

	static void assertBaCookieBiXoa(MvcResult result) {
		List<String> cookies = AuthApi.setCookies(result);
		assertThat(cookies).hasSize(3).allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
		assertThat(AuthApi.setCookie(result, "refresh_token").orElseThrow()).contains("Path=/api/v1/auth");
		assertThat(AuthApi.setCookie(result, "access_token").orElseThrow()).contains("Path=/;");
		assertThat(AuthApi.setCookie(result, "phien").orElseThrow()).contains("Path=/;");
	}

}
