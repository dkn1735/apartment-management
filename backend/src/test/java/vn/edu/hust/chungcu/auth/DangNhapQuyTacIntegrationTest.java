package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;

/** Quy tắc đăng nhập của spec 0004 (AC-2 tới AC-6, AC-16). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class DangNhapQuyTacIntegrationTest {

	private static final String SAI_THONG_TIN = "Số điện thoại, email hoặc mật khẩu không đúng.";

	private static final String BI_KHOA = "0900000008";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	PasswordEncoder passwordEncoder;

	TaiKhoanThu taiKhoanThu;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.taiKhoanThu = new TaiKhoanThu(this.jdbc);
		clearInvocations(this.passwordEncoder);
	}

	// AC-2
	@Test
	void dangNhapBangEmailChuHoaCoKhoangTrang() throws Exception {
		login("  CuDan@ChungCu.Test ", AuthApi.MAT_KHAU_SEED).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.hoTen").value("Vũ Cư Dân"));
	}

	// AC-2
	@Test
	void dangNhapBangSoCoTienToCongTamTu() throws Exception {
		login("+84 900 000 007", AuthApi.MAT_KHAU_SEED).andExpect(status().isOk());
	}

	// AC-3: cùng mã, cùng câu, không cookie, đúng một lần bcrypt ở mỗi nhánh
	@Test
	void saiMatKhauKhongTonTaiVaKhongHopLeGiongNhau() throws Exception {
		for (String[] input : List.of(new String[] { "0900000007", "SaiMatKhau1" },
				new String[] { "0999999999", AuthApi.MAT_KHAU_SEED }, new String[] { "abc", AuthApi.MAT_KHAU_SEED })) {
			clearInvocations(this.passwordEncoder);
			expectSaiThongTin(login(input[0], input[1]));
			verify(this.passwordEncoder, times(1)).matches(any(), anyString());
		}
	}

	// AC-3: hash không phải bcrypt bị coi là sai mật khẩu
	@Test
	void hashNoopBiTuChoi() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao("{noop}" + AuthApi.MAT_KHAU_SEED);
		expectSaiThongTin(login(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED));
	}

	// AC-4
	@Test
	void taiKhoanKhoaDungMatKhauTra403KhongTaoPhien() throws Exception {
		String ma = this.jdbc.queryForObject("SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?",
				String.class, BI_KHOA);
		int truoc = this.taiKhoanThu.soPhien(ma);
		login(BI_KHOA, AuthApi.MAT_KHAU_SEED).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"))
			.andExpect(result -> assertThat(AuthApi.setCookies(result)).isEmpty());
		assertThat(this.taiKhoanThu.soPhien(ma)).isEqualTo(truoc);
	}

	// AC-4
	@Test
	void taiKhoanKhoaSaiMatKhauVanLa401() throws Exception {
		expectSaiThongTin(login(BI_KHOA, "SaiMatKhau1"));
	}

	// AC-4
	@Test
	void taiKhoanNgungHoatDongTra403() throws Exception {
		String hash = this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED);
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(hash, "Ngừng hoạt động");
		login(tk.email(), AuthApi.MAT_KHAU_SEED).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"));
	}

	// AC-5
	@Test
	void thieuTruongTra400TheoTruong() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/auth/login").header("X-Requested-With", "XMLHttpRequest")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"taiKhoan\":\" \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[?(@.field == 'taiKhoan')]").exists())
			.andExpect(jsonPath("$.errors[?(@.field == 'matKhau')]").exists());
	}

	// AC-5
	@Test
	void quaDoDaiTra400() throws Exception {
		login("0900000007", "a".repeat(129)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("matKhau"));
		login("a".repeat(151), AuthApi.MAT_KHAU_SEED).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("taiKhoan"));
	}

	// AC-5: 40 ký tự "ệ" là 120 byte, quá giới hạn 72 byte của bcrypt
	@Test
	void matKhauQua72ByteLaSaiMatKhau() throws Exception {
		expectSaiThongTin(login("0900000007", "ệ".repeat(40)));
		verify(this.passwordEncoder, times(1)).matches(any(), anyString());
	}

	// AC-6
	@Test
	void dangNhapGhiMotPhienVaLucDangNhap() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		long phienBanTruoc = phienBan(tk.maNguoiDung());

		MvcResult result = this.mockMvc.perform(AuthApi.loginRequest(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED)
			.header("User-Agent", "U".repeat(600))
			.with(request -> {
				request.setRemoteAddr("203.0.113.7");
				return request;
			})).andExpect(status().isOk()).andReturn();

		String refresh = AuthApi.cookieValue(result, "refresh_token").orElseThrow();
		List<Map<String, Object>> phien = this.jdbc
			.queryForList("SELECT * FROM phien_dang_nhap WHERE ma_nguoi_dung = ?", tk.maNguoiDung());
		assertThat(phien).hasSize(1);
		Map<String, Object> row = phien.get(0);
		assertThat(row.get("token_hash")).isEqualTo(RefreshTokenGenerator.sha256Hex(refresh));
		assertThat(((Timestamp) row.get("tao_luc")).toInstant()).isEqualTo(MutableClock.MAC_DINH);
		assertThat(((Timestamp) row.get("het_han_luc")).toInstant())
			.isEqualTo(MutableClock.MAC_DINH.plus(Duration.ofDays(7)));
		assertThat(row.get("ma_ho_phien")).isNotNull();
		assertThat(row.get("dia_chi_ip")).isEqualTo("203.0.113.7");
		assertThat((String) row.get("user_agent")).hasSize(500);
		assertThat(row.get("thu_hoi_luc")).isNull();

		Map<String, Object> nd = this.jdbc.queryForMap(
				"SELECT dang_nhap_cuoi_luc, so_lan_dang_nhap_sai, khoa_tam_den, phien_ban FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				tk.maNguoiDung());
		assertThat(((Timestamp) nd.get("dang_nhap_cuoi_luc")).toInstant()).isEqualTo(MutableClock.MAC_DINH);
		assertThat(nd.get("so_lan_dang_nhap_sai")).isEqualTo(0);
		assertThat(nd.get("khoa_tam_den")).isNull();
		assertThat(nd.get("phien_ban")).isEqualTo(phienBanTruoc);
	}

	// AC-6: không có User-Agent thì ghi null
	@Test
	void khongCoUserAgentThiGhiNull() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		login(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED).andExpect(status().isOk());
		assertThat(this.jdbc.queryForObject("SELECT user_agent FROM phien_dang_nhap WHERE ma_nguoi_dung = ?",
				String.class, tk.maNguoiDung()))
			.isNull();
	}

	// AC-6: hai lần đăng nhập cùng lúc không gây lỗi khóa lạc quan
	@Test
	void haiLanDangNhapSongSongDeuThanhCong() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		Callable<Integer> task = () -> this.mockMvc
			.perform(AuthApi.loginRequest(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED))
			.andReturn()
			.getResponse()
			.getStatus();
		try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
			List<Future<Integer>> results = pool.invokeAll(List.of(task, task));
			for (Future<Integer> result : results) {
				assertThat(result.get()).isEqualTo(200);
			}
		}
		assertThat(this.taiKhoanThu.soPhien(tk.maNguoiDung())).isEqualTo(2);
	}

	// AC-16
	@Test
	void logKhongChuaBiMat(CapturedOutput output) throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String hash = this.taiKhoanThu.hash(tk.maNguoiDung());

		MvcResult ok = login(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED).andExpect(status().isOk()).andReturn();
		login(tk.email(), "SaiMatKhau1").andExpect(status().isUnauthorized());
		login("0999999999", "SaiMatKhau2").andExpect(status().isUnauthorized());
		login(BI_KHOA, AuthApi.MAT_KHAU_SEED).andExpect(status().isForbidden());
		login(tk.soDienThoai(), "x".repeat(129)).andExpect(status().isBadRequest());
		login("", "MatKhauRong").andExpect(status().isBadRequest());

		String access = AuthApi.cookieValue(ok, "access_token").orElseThrow();
		String refresh = AuthApi.cookieValue(ok, "refresh_token").orElseThrow();
		String log = output.getAll();
		assertThat(log).contains("Đăng nhập thành công: maNguoiDung=" + tk.maNguoiDung())
			.contains("lyDo=SAI_THONG_TIN, maNguoiDung=" + tk.maNguoiDung())
			.contains("lyDo=TAI_KHOAN_KHOA, maNguoiDung=")
			.containsPattern("lyDo=SAI_THONG_TIN\\R");
		assertThat(log).doesNotContain(AuthApi.MAT_KHAU_SEED, "SaiMatKhau1", "SaiMatKhau2", "x".repeat(129),
				"MatKhauRong", access, refresh, RefreshTokenGenerator.sha256Hex(refresh), hash, tk.soDienThoai(),
				tk.email(), "0999999999", BI_KHOA);
	}

	private ResultActions login(String taiKhoan, String matKhau) throws Exception {
		return this.mockMvc.perform(AuthApi.loginRequest(taiKhoan, matKhau));
	}

	private void expectSaiThongTin(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.message").value(SAI_THONG_TIN))
			.andExpect(r -> assertThat(AuthApi.setCookies(r)).isEmpty())
			.andExpect(r -> assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).isNotBlank());
	}

	private long phienBan(String ma) {
		Long value = this.jdbc.queryForObject("SELECT phien_ban FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				Long.class, ma);
		return (value != null) ? value : -1;
	}

}
