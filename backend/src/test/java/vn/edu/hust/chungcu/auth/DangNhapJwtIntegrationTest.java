package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;

/**
 * Đường mỏng của spec 0004: đăng nhập bằng tài khoản seed, cookie, JWT, chống CSRF và
 * {@code /profile} được bảo vệ (AC-1, AC-7, AC-10).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class DangNhapJwtIntegrationTest {

	private static final String CU_DAN = "0900000007";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JwtDecoder jwtDecoder;

	@Autowired
	org.springframework.context.ApplicationContext context;

	AuthApi auth;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
	}

	// AC-1
	@Test
	void dangNhapBangSoDienThoaiDatBaCookieVaKhongTraToken() throws Exception {
		MvcResult result = this.mockMvc.perform(AuthApi.loginRequest(CU_DAN, AuthApi.MAT_KHAU_SEED))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
			.andExpect(jsonPath("$.data.hoTen").value("Vũ Cư Dân"))
			.andExpect(jsonPath("$.data.maNguoiDung").isString())
			.andExpect(jsonPath("$.data.vaiTro[0]").value("CU_DAN"))
			.andExpect(jsonPath("$.data.accessTokenHetHanLuc").value("2026-11-16T01:15:00Z"))
			.andReturn();

		String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		String access = AuthApi.cookieValue(result, "access_token").orElseThrow();
		String refresh = AuthApi.cookieValue(result, "refresh_token").orElseThrow();
		assertThat(body).doesNotContain(access).doesNotContain(refresh).doesNotContainIgnoringCase("token\"");
		assertThat(refresh).matches("^[A-Za-z0-9_-]{43}$");

		// Profile dev và test: cookie-secure=false
		assertThat(AuthApi.setCookie(result, "access_token"))
			.hasValueSatisfying(c -> assertThat(c).contains("Path=/;", "Max-Age=900", "HttpOnly", "SameSite=Strict")
				.doesNotContain("Secure"));
		assertThat(AuthApi.setCookie(result, "refresh_token")).hasValueSatisfying(
				c -> assertThat(c).contains("Path=/api/v1/auth;", "Max-Age=604800", "HttpOnly", "SameSite=Strict"));
		assertThat(AuthApi.setCookie(result, "phien")).hasValueSatisfying(
				c -> assertThat(c).startsWith("phien=1;").contains("Path=/;", "Max-Age=604800", "HttpOnly"));
	}

	// AC-7
	@Test
	void accessTokenCoDuClaim() throws Exception {
		Jwt jwt = this.jwtDecoder.decode(this.auth.accessToken("0900000006"));
		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("chungcu");
		assertThat(jwt.getSubject()).matches("^ND[0-9]{8}$");
		assertThat(UUID.fromString(jwt.getClaimAsString("sid"))).isNotNull();
		assertThat(jwt.getId()).isNotBlank();
		assertThat(jwt.getClaimAsStringList("vaiTro")).containsExactly("BAN_QUAN_TRI", "CU_DAN");
		assertThat(jwt.getIssuedAt()).isEqualTo(MutableClock.MAC_DINH);
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
	}

	// AC-1, AC-7
	@Test
	void profileDocTokenTuCookie() throws Exception {
		this.mockMvc.perform(this.auth.profile(this.auth.accessToken(CU_DAN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Lấy thông tin tài khoản thành công"))
			.andExpect(jsonPath("$.data.hoTen").value("Vũ Cư Dân"))
			.andExpect(jsonPath("$.data.soDienThoai").value(CU_DAN))
			.andExpect(jsonPath("$.data.email").value("cudan@chungcu.test"))
			.andExpect(jsonPath("$.data.trangThaiTaiKhoan").value("Hoạt động"))
			.andExpect(jsonPath("$.data.vaiTro[0]").value("CU_DAN"));
	}

	// AC-7
	@Test
	void profileKhongCoCookieTra401TheoVoBoc() throws Exception {
		expectUnauthenticated(get("/api/v1/auth/profile"));
	}

	// AC-7: quá 30 giây cho phép lệch giờ
	@Test
	void tokenHetHanTra401() throws Exception {
		String token = this.auth.accessToken(CU_DAN);
		this.clock.advance(Duration.ofMinutes(15).plusSeconds(20));
		this.mockMvc.perform(this.auth.profile(token)).andExpect(status().isOk());
		this.clock.advance(Duration.ofSeconds(20));
		expectUnauthenticated(this.auth.profile(token));
	}

	// AC-7
	@Test
	void tokenKyBangKhoaKhacTra401() throws Exception {
		byte[] khac = "mot-khoa-khac-dai-hon-ba-muoi-hai-byte-0000".getBytes(StandardCharsets.UTF_8);
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(khac, "HmacSHA256")));
		String token = encoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims("ND00000007")))
			.getTokenValue();
		expectUnauthenticated(this.auth.profile(token));
	}

	// AC-7
	@Test
	void tokenAlgNoneTra401() throws Exception {
		Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
		long now = MutableClock.MAC_DINH.getEpochSecond();
		String header = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
		String payload = b64.encodeToString(
				("{\"iss\":\"chungcu\",\"sub\":\"ND00000007\",\"iat\":" + now + ",\"exp\":" + (now + 900) + "}")
					.getBytes(StandardCharsets.UTF_8));
		expectUnauthenticated(this.auth.profile(header + "." + payload + "."));
	}

	// AC-7: chỉ đọc cookie, bỏ qua header Authorization
	@Test
	void tokenTrongHeaderAuthorizationBiBoQua() throws Exception {
		String token = this.auth.accessToken(CU_DAN);
		expectUnauthenticated(get("/api/v1/auth/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	// AC-7
	@Test
	void cookieRacKhongChanDuongCongKhai() throws Exception {
		Cookie rac = new Cookie("access_token", "rac.rac.rac");
		this.mockMvc.perform(AuthApi.loginRequest(CU_DAN, AuthApi.MAT_KHAU_SEED).cookie(rac))
			.andExpect(status().isOk());
		this.mockMvc.perform(get("/actuator/health").cookie(rac)).andExpect(status().isOk());
	}

	// AC-10
	@Test
	void dangNhapThieuHeaderChongCsrfTra403() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"taiKhoan\":\"0900000007\",\"matKhau\":\"ChungCu@2026\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("CSRF_REJECTED"))
			.andExpect(jsonPath("$.requestId").isString())
			.andExpect(result -> assertThat(AuthApi.setCookies(result)).isEmpty());
	}

	// AC-10: so khớp phân biệt hoa thường
	@Test
	void headerChongCsrfSaiHoaThuongBiChan() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/auth/login").header("X-Requested-With", "xmlhttprequest")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"taiKhoan\":\"0900000007\",\"matKhau\":\"ChungCu@2026\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("CSRF_REJECTED"));
	}

	// AC-10: GET và OPTIONS không bị kiểm
	@Test
	void getVaOptionsKhongCanHeaderChongCsrf() throws Exception {
		this.mockMvc.perform(this.auth.profile(this.auth.accessToken(CU_DAN))).andExpect(status().isOk());
		this.mockMvc.perform(options("/api/v1/auth/login"))
			.andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
	}

	// AC-10: filter không là bean, nên Spring Boot không đăng ký thêm nó thành servlet
	// filter chạy lần hai ngoài chuỗi bảo mật
	@Test
	void csrfHeaderFilterKhongLaBean() {
		assertThat(this.context.getBeansOfType(vn.edu.hust.chungcu.common.security.CsrfHeaderFilter.class)).isEmpty();
	}

	private void expectUnauthenticated(
			org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
		this.mockMvc.perform(request)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
			.andExpect(jsonPath("$.success").value(false));
	}

	private static JwtClaimsSet claims(String sub) {
		Instant now = MutableClock.MAC_DINH;
		return JwtClaimsSet.builder()
			.issuer("chungcu")
			.subject(sub)
			.issuedAt(now)
			.expiresAt(now.plusSeconds(900))
			.claim("vaiTro", List.of("CU_DAN"))
			.build();
	}

}
