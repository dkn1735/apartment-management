package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/** Đổi mật khẩu (API-AUTH-04, tính năng 10). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class DoiMatKhauIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String MAT_KHAU_MOI = "MatKhauMoi2026";

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

	NhatKyDb nhatKy;

	TaiKhoanThu taiKhoan;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.db = new PhienDb(this.jdbc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.taiKhoan = new TaiKhoanThu(this.jdbc);
		this.tk = this.taiKhoan.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	/** Một lần đăng nhập: hai cookie của cùng một máy. */
	record May(String accessToken, String refreshToken) {
	}

	May dangNhap() throws Exception {
		MvcResult result = this.auth.login(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED);
		return new May(AuthApi.cookieValue(result, "access_token").orElseThrow(),
				AuthApi.cookieValue(result, "refresh_token").orElseThrow());
	}

	static MockHttpServletRequestBuilder doi(String accessToken, String matKhauCu, String matKhauMoi) {
		MockHttpServletRequestBuilder request = put("/api/v1/auth/change-password")
			.header("X-Requested-With", "XMLHttpRequest")
			.contentType(MediaType.APPLICATION_JSON)
			.content(JSON.writeValueAsString(new Body(matKhauCu, matKhauMoi)));
		return (accessToken != null) ? request.cookie(new Cookie("access_token", accessToken)) : request;
	}

	record Body(String matKhauCu, String matKhauMoi) {
	}

	long phienBan() {
		Long value = this.jdbc.queryForObject("SELECT phien_ban FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				Long.class, this.tk.maNguoiDung());
		return (value != null) ? value : -1;
	}

	@Test
	void doiThanhCongGiuMayNayThuHoiMayKhac() throws Exception {
		May mayA = dangNhap();
		May mayB = dangNhap();
		May mayC = dangNhap();
		UUID hoA = this.db.hoPhien(mayA.refreshToken());
		UUID hoB = this.db.hoPhien(mayB.refreshToken());
		UUID hoC = this.db.hoPhien(mayC.refreshToken());
		String hashCu = this.taiKhoan.hash(this.tk.maNguoiDung());
		long phienBanCu = phienBan();
		String maLogTruoc = this.nhatKy.maLogCuoi();

		this.mockMvc.perform(doi(mayA.accessToken(), AuthApi.MAT_KHAU_SEED, MAT_KHAU_MOI))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"))
			.andExpect(jsonPath("$.data.capNhatLuc").value(MutableClock.MAC_DINH.toString()))
			// Chỉ cấp lại access_token (spec 0008, AC-13); refresh_token và phien giữ
			// nguyên
			.andExpect(result -> assertThat(AuthApi.setCookies(result)).singleElement()
				.satisfies((c) -> assertThat(c).startsWith("access_token=").contains("Path=/;").contains("HttpOnly")));

		// Hash mới là bcrypt của mật khẩu mới, phien_ban tăng
		String hashMoi = this.taiKhoan.hash(this.tk.maNguoiDung());
		assertThat(hashMoi).isNotEqualTo(hashCu).startsWith("{bcrypt}");
		assertThat(this.passwordEncoder.matches(MAT_KHAU_MOI, hashMoi)).isTrue();
		assertThat(phienBan()).isEqualTo(phienBanCu + 1);

		// Máy A còn phiên; B và C bị thu hồi với lý do "Đổi mật khẩu"
		assertThat(this.db.lyDo(hoA)).containsExactly("con");
		assertThat(this.db.lyDo(hoB)).containsExactly("Đổi mật khẩu");
		assertThat(this.db.lyDo(hoC)).containsExactly("Đổi mật khẩu");
		assertThat(this.auth.refresh(mayB.refreshToken()).getResponse().getStatus()).isEqualTo(401);
		assertThat(this.auth.refresh(mayA.refreshToken()).getResponse().getStatus()).isEqualTo(200);

		// Mật khẩu cũ không còn đăng nhập được, mật khẩu mới thì được
		assertThat(this.auth.login(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED).getResponse().getStatus())
			.isEqualTo(401);
		assertThat(this.auth.login(this.tk.soDienThoai(), MAT_KHAU_MOI).getResponse().getStatus()).isEqualTo(200);

		// Đúng một dòng nhật ký đổi mật khẩu, không chứa mật khẩu hay hash
		List<Map<String, Object>> dong = this.nhatKy.cua(this.tk.maNguoiDung())
			.stream()
			.filter(d -> "AUTH_DOI_MAT_KHAU".equals(d.get("loai_thao_tac")))
			.toList();
		assertThat(dong).hasSize(1);
		assertThat(dong.getFirst().get("doi_tuong_tac_dong")).isEqualTo("NguoiDungHeThong:" + this.tk.maNguoiDung());
		assertThat(NhatKyDb.chiTiet(dong.getFirst())).isEqualTo(NhatKyDb.json("{\"soPhienThuHoi\":2}"));
		assertThat(this.nhatKy.toanBoSau(maLogTruoc)).doesNotContain(AuthApi.MAT_KHAU_SEED, MAT_KHAU_MOI, hashCu,
				hashMoi);
	}

	// Hai lần đổi cùng lúc: chỉ một lần thắng, lần kia không ghi đè (409 hoặc 422)
	@Test
	void haiLanDoiCungLucChiMotLanThang() throws Exception {
		May mayA = dangNhap();
		List<MvcResult> ketQua = AuthApi.songSong(this.mockMvc,
				doi(mayA.accessToken(), AuthApi.MAT_KHAU_SEED, "MatKhauThuNhat1"),
				doi(mayA.accessToken(), AuthApi.MAT_KHAU_SEED, "MatKhauThuHai22"));
		List<Integer> status = ketQua.stream().map(r -> r.getResponse().getStatus()).toList();
		assertThat(status).containsOnlyOnce(200);
		assertThat(status).anyMatch(code -> code == 409 || code == 422);
		String thang = (status.get(0) == 200) ? "MatKhauThuNhat1" : "MatKhauThuHai22";
		assertThat(this.passwordEncoder.matches(thang, this.taiKhoan.hash(this.tk.maNguoiDung()))).isTrue();
	}

	@Test
	void khongCoPhienKhacThiSoPhienThuHoiLaKhong() throws Exception {
		May mayA = dangNhap();
		this.mockMvc.perform(doi(mayA.accessToken(), AuthApi.MAT_KHAU_SEED, MAT_KHAU_MOI)).andExpect(status().isOk());
		Map<String, Object> dong = this.nhatKy.cua(this.tk.maNguoiDung()).getLast();
		assertThat(dong.get("loai_thao_tac")).isEqualTo("AUTH_DOI_MAT_KHAU");
		assertThat(NhatKyDb.chiTiet(dong)).isEqualTo(NhatKyDb.json("{\"soPhienThuHoi\":0}"));
	}

	@Test
	void saiMatKhauCuTra422VaKhongDoiGi() throws Exception {
		May mayA = dangNhap();
		May mayB = dangNhap();
		String hashCu = this.taiKhoan.hash(this.tk.maNguoiDung());
		long phienBanCu = phienBan();

		this.mockMvc.perform(doi(mayA.accessToken(), "SaiMatKhau123", MAT_KHAU_MOI))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("AUTH_OLD_PASSWORD_INCORRECT"))
			.andExpect(jsonPath("$.errors[0].field").value("matKhauCu"))
			.andExpect(jsonPath("$.errors[0].message").value("Mật khẩu hiện tại không đúng."));

		assertThat(this.taiKhoan.hash(this.tk.maNguoiDung())).isEqualTo(hashCu);
		assertThat(phienBan()).isEqualTo(phienBanCu);
		assertThat(this.db.lyDo(this.db.hoPhien(mayB.refreshToken()))).containsExactly("con");
		assertThat(this.nhatKy.loaiCua(this.tk.maNguoiDung())).doesNotContain("AUTH_DOI_MAT_KHAU");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "Ab1", "chikhongcoso", "12345678",
			"Mật khẩu có dấu nhưng thật dài 1 Mật khẩu có dấu nhưng thật dài 2 Mật khẩu có dấu 3" })
	void matKhauMoiKhongDatQuyTacTra400(String matKhauMoi) throws Exception {
		May mayA = dangNhap();
		String hashCu = this.taiKhoan.hash(this.tk.maNguoiDung());

		this.mockMvc.perform(doi(mayA.accessToken(), AuthApi.MAT_KHAU_SEED, matKhauMoi))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("matKhauMoi"));
		assertThat(this.taiKhoan.hash(this.tk.maNguoiDung())).isEqualTo(hashCu);
	}

	@Test
	void matKhauMoiTrungMatKhauCuTra400() throws Exception {
		May mayA = dangNhap();
		this.mockMvc.perform(doi(mayA.accessToken(), "MatKhau2026", "MatKhau2026"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("matKhauMoi"))
			.andExpect(jsonPath("$.errors[0].message").value("Mật khẩu mới phải khác mật khẩu hiện tại"));
	}

	@Test
	void thieuMatKhauCuTra400() throws Exception {
		May mayA = dangNhap();
		this.mockMvc.perform(doi(mayA.accessToken(), "", MAT_KHAU_MOI))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("matKhauCu"));
	}

	@Test
	void chuaDangNhapTra401() throws Exception {
		this.mockMvc.perform(doi(null, AuthApi.MAT_KHAU_SEED, MAT_KHAU_MOI))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
	}

	@Test
	void thieuHeaderChongCsrfTra403() throws Exception {
		May mayA = dangNhap();
		this.mockMvc
			.perform(put("/api/v1/auth/change-password").cookie(new Cookie("access_token", mayA.accessToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.writeValueAsString(new Body(AuthApi.MAT_KHAU_SEED, MAT_KHAU_MOI))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("CSRF_REJECTED"));
	}

	@Test
	void taiKhoanBiKhoaTra403() throws Exception {
		May mayA = dangNhap();
		this.taiKhoan.doiTrangThai(this.tk.maNguoiDung(), "Tạm khóa");
		this.mockMvc.perform(doi(mayA.accessToken(), AuthApi.MAT_KHAU_SEED, MAT_KHAU_MOI))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"));
	}

}
