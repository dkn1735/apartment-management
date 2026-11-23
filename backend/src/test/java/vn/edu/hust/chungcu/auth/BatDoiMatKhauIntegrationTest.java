package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.common.security.JwtConfig;

/** Bắt đổi mật khẩu tạm ở lần đăng nhập đầu (spec 0008, AC-13). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class BatDoiMatKhauIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String MAT_KHAU_MOI = "MatKhauMoi2026";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	JwtDecoder jwtDecoder;

	@Autowired
	JwtEncoder jwtEncoder;

	@Autowired
	PasswordEncoder passwordEncoder;

	AuthApi auth;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
	}

	/** Admin tạo một tài khoản BQL, trả số điện thoại và mật khẩu tạm. */
	String[] taoTaiKhoanBql() throws Exception {
		String so = TaiKhoanApi.soMoi();
		Map<String, Object> body = TaiKhoanApi.body("Người Mới", so, "moi" + so + "@chungcu.test",
				List.of(TaiKhoanApi.vaiTro("BAN_QUAN_LY", "chucVu", "Nhân viên", "boPhan", "Vận hành")));
		MvcResult r = this.mockMvc.perform(TaiKhoanApi.tao(this.auth.accessToken(TaiKhoanApi.ADMIN), body))
			.andExpect(status().isCreated())
			.andReturn();
		String matKhauTam = JSON.readTree(r.getResponse().getContentAsString())
			.get("data")
			.get("matKhauTam")
			.asString();
		return new String[] { so, matKhauTam };
	}

	@Test
	void matKhauTamChiGoiDuocHoSoVaDoiMatKhauDoiXongChayNgay() throws Exception {
		String[] tk = taoTaiKhoanBql();
		MvcResult login = this.auth.login(tk[0], tk[1]);
		assertThat(login.getResponse().getStatus()).isEqualTo(200);
		assertThat(
				JSON.readTree(login.getResponse().getContentAsString()).get("data").get("phaiDoiMatKhau").asBoolean())
			.isTrue();
		String access = AuthApi.cookieValue(login, "access_token").orElseThrow();
		String refresh = AuthApi.cookieValue(login, "refresh_token").orElseThrow();

		// API thường bị chặn, kể cả API mà vai trò được phép
		this.mockMvc.perform(get("/api/v1/buildings").cookie(new Cookie("access_token", access)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_PASSWORD_CHANGE_REQUIRED"))
			.andExpect(jsonPath("$.message").value("Bạn cần đổi mật khẩu tạm trước khi tiếp tục."));
		// Hồ sơ vẫn gọi được và báo cờ
		this.mockMvc.perform(this.auth.profile(access))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phaiDoiMatKhau").value(true));
		// Làm mới vẫn giữ cờ vì đọc lại từ DB
		MvcResult lamMoi = this.auth.refresh(refresh);
		assertThat(
				JSON.readTree(lamMoi.getResponse().getContentAsString()).get("data").get("phaiDoiMatKhau").asBoolean())
			.isTrue();
		access = AuthApi.cookieValue(lamMoi, "access_token").orElseThrow();
		this.mockMvc.perform(get("/api/v1/buildings").cookie(new Cookie("access_token", access)))
			.andExpect(status().isForbidden());

		// Đổi mật khẩu bằng mật khẩu tạm làm mật khẩu cũ: nhận access token mới, gọi được
		// ngay
		MvcResult doi = this.mockMvc.perform(DoiMatKhauIntegrationTest.doi(access, tk[1], MAT_KHAU_MOI))
			.andExpect(status().isOk())
			.andReturn();
		String accessMoi = AuthApi.cookieValue(doi, "access_token").orElseThrow();
		this.mockMvc.perform(get("/api/v1/buildings").cookie(new Cookie("access_token", accessMoi)))
			.andExpect(status().isOk());
		this.mockMvc.perform(this.auth.profile(accessMoi)).andExpect(jsonPath("$.data.phaiDoiMatKhau").value(false));
		assertThat(this.jdbc.queryForObject("SELECT phai_doi_mat_khau FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?",
				Boolean.class, tk[0]))
			.isFalse();
	}

	@Test
	void taiKhoanThuongKhongBiChanVaChuaDangNhapVan401() throws Exception {
		String admin = this.auth.accessToken(TaiKhoanApi.ADMIN);
		this.mockMvc.perform(TaiKhoanApi.danhSach(admin)).andExpect(status().isOk());
		this.mockMvc.perform(get("/api/v1/accounts"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
	}

	/** Ký lại access token với claim đã sửa, giữ nguyên chữ ký hợp lệ của server. */
	String kyLai(String access, Consumer<Map<String, Object>> sua) {
		Jwt jwt = this.jwtDecoder.decode(access);
		JwtClaimsSet claims = JwtClaimsSet.builder().claims((c) -> {
			c.putAll(jwt.getClaims());
			sua.accept(c);
		}).build();
		return this.jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	int buildings(String access) throws Exception {
		return this.mockMvc.perform(get("/api/v1/buildings").cookie(new Cookie("access_token", access)))
			.andReturn()
			.getResponse()
			.getStatus();
	}

	// AC-13: token cấp trước tính năng này không có claim, hoặc claim không phải boolean,
	// thì coi như false
	@Test
	void claimThieuHoacKhongPhaiBooleanCoiNhuFalse() throws Exception {
		String[] tk = taoTaiKhoanBql();
		String access = AuthApi.cookieValue(this.auth.login(tk[0], tk[1]), "access_token").orElseThrow();
		assertThat(buildings(access)).isEqualTo(403);

		assertThat(buildings(kyLai(access, (c) -> c.remove(JwtConfig.CLAIM_PHAI_DOI_MAT_KHAU)))).isEqualTo(200);
		assertThat(buildings(kyLai(access, (c) -> c.put(JwtConfig.CLAIM_PHAI_DOI_MAT_KHAU, "true")))).isEqualTo(200);
		assertThat(buildings(kyLai(access, (c) -> c.put(JwtConfig.CLAIM_PHAI_DOI_MAT_KHAU, 1)))).isEqualTo(200);
	}

	// AC-13 (Value sourcing): claim đọc lại từ DB mỗi lần cấp, không chép từ token cũ
	@Test
	void lamMoiDocLaiCoTuDb() throws Exception {
		String[] tk = taoTaiKhoanBql();
		this.jdbc.update(
				"UPDATE nguoi_dung_he_thong SET mat_khau_hash = ?, phai_doi_mat_khau = false WHERE so_dien_thoai = ?",
				this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED), tk[0]);
		MvcResult login = this.auth.login(tk[0], AuthApi.MAT_KHAU_SEED);
		String access = AuthApi.cookieValue(login, "access_token").orElseThrow();
		String refresh = AuthApi.cookieValue(login, "refresh_token").orElseThrow();
		assertThat(buildings(access)).isEqualTo(200);

		this.jdbc.update("UPDATE nguoi_dung_he_thong SET phai_doi_mat_khau = true WHERE so_dien_thoai = ?", tk[0]);
		MvcResult lamMoi = this.auth.refresh(refresh);
		assertThat(
				JSON.readTree(lamMoi.getResponse().getContentAsString()).get("data").get("phaiDoiMatKhau").asBoolean())
			.isTrue();
		assertThat(buildings(AuthApi.cookieValue(lamMoi, "access_token").orElseThrow())).isEqualTo(403);
	}

}
