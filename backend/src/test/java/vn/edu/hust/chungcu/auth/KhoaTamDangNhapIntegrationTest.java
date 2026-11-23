package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
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
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.PhienDangNhapService;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/** Khóa tạm cho tài khoản có thật (spec 0009, AC-1 tới AC-4, AC-7, AC-18). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class KhoaTamDangNhapIntegrationTest {

	static final String QUA_NHIEU_LAN = "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút hoặc liên hệ ban quản lý.";

	private static final String SAI = "SaiMatKhau1";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PhienDangNhapService phienService;

	@MockitoSpyBean
	PasswordEncoder passwordEncoder;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		clearInvocations(this.passwordEncoder);
	}

	// AC-1, AC-7
	@Test
	void saiNamLanThiKhoaMuoiLamPhut() throws Exception {
		KhoaTamTestSupport.TrangThai truoc = trangThai();
		for (int i = 1; i <= 4; i++) {
			this.clock.advance(Duration.ofSeconds(10));
			login(SAI).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"))
				.andExpect(header().doesNotExist("Retry-After"));
			KhoaTamTestSupport.TrangThai sau = trangThai();
			assertThat(sau.soLanSai()).isEqualTo(i);
			assertThat(sau.lanSaiCuoiLuc()).isEqualTo(this.clock.instant());
			assertThat(sau.khoaTamDen()).isNull();
		}
		this.clock.advance(Duration.ofSeconds(10));
		expectKhoa(login(SAI), 900);
		KhoaTamTestSupport.TrangThai khoa = trangThai();
		assertThat(khoa.khoaTamDen()).isEqualTo(this.clock.instant().plus(Duration.ofMinutes(15)));
		assertThat(khoa.soLanSai()).isZero();
		assertThat(khoa.phienBan()).isEqualTo(truoc.phienBan());
		assertThat(khoa.capNhatLuc()).isEqualTo(truoc.capNhatLuc());
		assertThat(khoa.trangThaiTaiKhoan()).isEqualTo("Hoạt động");
		assertThat(new TaiKhoanThu(this.jdbc).soPhien(this.tk.maNguoiDung())).isZero();
	}

	// AC-2, AC-7: đúng mật khẩu vẫn bị chặn, không bcrypt, không ghi gì
	@Test
	void dangKhoaThiChanKeCaMatKhauDung() throws Exception {
		khoa();
		KhoaTamTestSupport.TrangThai truoc = trangThai();
		NhatKyDb nhatKy = new NhatKyDb(this.jdbc);
		String maLog = nhatKy.maLogCuoi();
		clearInvocations(this.passwordEncoder);

		expectKhoa(login(AuthApi.MAT_KHAU_SEED), 900).andExpect(r -> assertThat(AuthApi.setCookies(r)).isEmpty());
		this.clock.advance(Duration.ofMinutes(10));
		expectKhoa(login(SAI), 300);
		this.clock.advance(Duration.ofSeconds(299).plusMillis(500));
		expectKhoa(login(AuthApi.MAT_KHAU_SEED), 1);

		verify(this.passwordEncoder, never()).matches(any(), anyString());
		assertThat(trangThai()).isEqualTo(truoc);
		assertThat(nhatKy.toanBoSau(maLog)).isEmpty();
		assertThat(new TaiKhoanThu(this.jdbc).soPhien(this.tk.maNguoiDung())).isZero();
	}

	// AC-3: tới đúng khoa_tam_den thì mở; sai thì bộ đếm bắt đầu lại từ 1
	@Test
	void toiHanThiTuMo() throws Exception {
		Instant khoaDen = khoa();
		this.clock.set(khoaDen);
		login(AuthApi.MAT_KHAU_SEED).andExpect(status().isOk());

		khoaDen = khoa();
		this.clock.set(khoaDen);
		login(SAI).andExpect(status().isUnauthorized());
		assertThat(trangThai().soLanSai()).isEqualTo(1);
		assertThat(trangThai().khoaTamDen()).isNull();
	}

	// AC-18: cửa sổ đếm 15 phút
	@Test
	void muoiLamPhutKhongSaiThiBoDemVeKhong() throws Exception {
		saiLan(4);
		this.clock.advance(Duration.ofMinutes(15));
		login(SAI).andExpect(status().isUnauthorized());
		assertThat(trangThai().soLanSai()).isEqualTo(1);

		KhoaTamTestSupport.datLai(this.jdbc);
		saiLan(4);
		this.clock.advance(Duration.ofMinutes(14).plusSeconds(59));
		expectKhoa(login(SAI), 900);
	}

	// AC-4: mật khẩu đúng đặt lại cả ba cột
	@Test
	void dungMatKhauThiDatLai() throws Exception {
		saiLan(3);
		login(AuthApi.MAT_KHAU_SEED).andExpect(status().isOk());
		KhoaTamTestSupport.TrangThai sau = trangThai();
		assertThat(sau.soLanSai()).isZero();
		assertThat(sau.khoaTamDen()).isNull();
		assertThat(sau.lanSaiCuoiLuc()).isNull();
	}

	// AC-4: lượt thứ 5 là mật khẩu đúng thì đăng nhập được, khóa vừa đặt cũng bị xóa
	@Test
	void luotCuoiDungMatKhauThiVaoDuoc() throws Exception {
		saiLan(4);
		login(AuthApi.MAT_KHAU_SEED).andExpect(status().isOk());
		assertThat(trangThai().khoaTamDen()).isNull();
		login(SAI).andExpect(status().isUnauthorized());
	}

	// AC-4: nhánh 403 cũng đặt lại
	@Test
	void taiKhoanTamKhoaDungMatKhauThiDatLai() throws Exception {
		String ma = TaiKhoanApi.maTheoSo(this.jdbc, "0900000008");
		for (int i = 0; i < 3; i++) {
			this.mockMvc.perform(AuthApi.loginRequest("0900000008", SAI)).andExpect(status().isUnauthorized());
		}
		this.mockMvc.perform(AuthApi.loginRequest("0900000008", AuthApi.MAT_KHAU_SEED))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"));
		assertThat(KhoaTamTestSupport.cua(this.jdbc, ma).soLanSai()).isZero();
		assertThat(KhoaTamTestSupport.cua(this.jdbc, ma).lanSaiCuoiLuc()).isNull();
	}

	// AC-4: đặt lại nằm trong transaction của taoPhien, lỗi thì rollback cùng
	@Test
	void taoPhienLoiThiBoDemCungRollback() throws Exception {
		saiLan(3);
		Instant now = this.clock.instant();
		PhienDangNhapService.PhienMoi sai = new PhienDangNhapService.PhienMoi(this.tk.maNguoiDung(), UUID.randomUUID(),
				"khong-phai-hash", now, now.plus(Duration.ofDays(7)), null, null, "x", null);
		assertThatThrownBy(() -> this.phienService.taoPhien(sai)).isInstanceOf(RuntimeException.class);
		assertThat(trangThai().soLanSai()).isEqualTo(3);
	}

	private Instant khoa() throws Exception {
		saiLan(4);
		expectKhoa(login(SAI), 900);
		return trangThai().khoaTamDen();
	}

	private void saiLan(int soLan) throws Exception {
		for (int i = 0; i < soLan; i++) {
			login(SAI).andExpect(status().isUnauthorized());
		}
	}

	private ResultActions login(String matKhau) throws Exception {
		return this.mockMvc.perform(AuthApi.loginRequest(this.tk.soDienThoai(), matKhau));
	}

	private KhoaTamTestSupport.TrangThai trangThai() {
		return KhoaTamTestSupport.cua(this.jdbc, this.tk.maNguoiDung());
	}

	static ResultActions expectKhoa(ResultActions result, long retryAfter) throws Exception {
		return result.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.errorCode").value("AUTH_TOO_MANY_ATTEMPTS"))
			.andExpect(jsonPath("$.message").value(QUA_NHIEU_LAN))
			.andExpect(jsonPath("$.errors").isEmpty())
			.andExpect(jsonPath("$.requestId").isNotEmpty())
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(header().string("Retry-After", Long.toString(retryAfter)));
	}

}
