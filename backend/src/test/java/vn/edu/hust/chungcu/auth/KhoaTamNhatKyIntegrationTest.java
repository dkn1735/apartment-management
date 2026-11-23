package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.regex.Pattern;
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
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/** Nhật ký kiểm toán và log ứng dụng của khóa tạm (spec 0009, AC-11, AC-17). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class KhoaTamNhatKyIntegrationTest {

	private static final String SAI = "SaiMatKhau1";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
	}

	// AC-11, AC-17: 5 lần sai rồi 3 lần bị chặn
	@Test
	void namLanSaiBaLanBiChan(CapturedOutput output) throws Exception {
		TaiKhoanThu.TaiKhoan tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		NhatKyDb nhatKy = new NhatKyDb(this.jdbc);
		String maLog = nhatKy.maLogCuoi();
		int dauLog = output.getAll().length();

		for (int i = 0; i < 5; i++) {
			this.mockMvc.perform(AuthApi.loginRequest(tk.soDienThoai(), SAI));
		}
		for (int i = 0; i < 3; i++) {
			this.mockMvc.perform(AuthApi.loginRequest(tk.email(), (i == 0) ? AuthApi.MAT_KHAU_SEED : SAI))
				.andExpect(status().isTooManyRequests());
		}

		List<String> loai = nhatKy.loaiCua(tk.maNguoiDung());
		assertThat(loai).containsExactly("AUTH_DANG_NHAP_THAT_BAI", "AUTH_DANG_NHAP_THAT_BAI",
				"AUTH_DANG_NHAP_THAT_BAI", "AUTH_DANG_NHAP_THAT_BAI", "AUTH_DANG_NHAP_THAT_BAI",
				"AUTH_KHOA_TAM_DANG_NHAP");
		String chiTiet = (String) nhatKy.cua(tk.maNguoiDung()).get(5).get("chi_tiet");
		assertThat(chiTiet).contains("\"khoaTamDen\"");
		assertThat(nhatKy.theoDoiTuong("NguoiDungHeThong:" + tk.maNguoiDung()).get(5).get("loai_thao_tac"))
			.isEqualTo("AUTH_KHOA_TAM_DANG_NHAP");

		String log = output.getAll().substring(dauLog);
		assertThat(Pattern.compile("lyDo=KHOA_TAM, maNguoiDung=" + tk.maNguoiDung()).matcher(log).results().count())
			.isEqualTo(3);
		assertThat(log).contains("Khóa tạm đăng nhập: maNguoiDung=" + tk.maNguoiDung());
		kiemKhongLo(nhatKy.toanBoSau(maLog) + log, tk.soDienThoai(), tk.email());
	}

	// AC-11, AC-17: định danh ảo không có dòng AUTH_KHOA_TAM_DANG_NHAP, log không có
	// định danh
	@Test
	void dinhDanhAoKhongCoNhatKyKhoa(CapturedOutput output) throws Exception {
		String ao = TaiKhoanApi.soMoi();
		NhatKyDb nhatKy = new NhatKyDb(this.jdbc);
		String maLog = nhatKy.maLogCuoi();
		int dauLog = output.getAll().length();
		for (int i = 0; i < 6; i++) {
			this.mockMvc.perform(AuthApi.loginRequest(ao, SAI));
		}
		String moi = nhatKy.toanBoSau(maLog);
		assertThat(moi).doesNotContain("AUTH_KHOA_TAM_DANG_NHAP");
		assertThat(moi.lines().filter((d) -> d.contains("AUTH_DANG_NHAP_THAT_BAI")).count()).isEqualTo(5);
		String log = output.getAll().substring(dauLog);
		assertThat(log).containsPattern("lyDo=KHOA_TAM\\R");
		kiemKhongLo(moi + log, ao);
	}

	private static void kiemKhongLo(String vanBan, String... dinhDanh) {
		assertThat(vanBan).doesNotContain(SAI, AuthApi.MAT_KHAU_SEED);
		for (String d : dinhDanh) {
			assertThat(vanBan).doesNotContain(d, RefreshTokenGenerator.sha256Hex(d));
		}
	}

}
