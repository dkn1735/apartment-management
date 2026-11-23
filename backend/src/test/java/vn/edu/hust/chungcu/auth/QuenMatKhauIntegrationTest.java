package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import vn.edu.hust.chungcu.GreenMailTestConfiguration;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Quên mật khẩu qua email, đường chính (spec 0010, AC-1, AC-6, AC-7, AC-11): yêu cầu,
 * email tới GreenMail, kiểm link, đặt mật khẩu mới.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class, GreenMailTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class QuenMatKhauIntegrationTest {

	static final String MAT_KHAU_MOI = "MatKhauMoi2026";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	GreenMail greenMail;

	AuthApi auth;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		QuenMatKhauThu.datLai(this.jdbc, this.greenMail);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	@Test
	void yeuCauGuiDungMotEmailCoLinkVaGhiHash() throws Exception {
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isEmpty())
			.andExpect(jsonPath("$.message").value(QuenMatKhauThu.PHAN_HOI_CHUNG));

		MimeMessage[] thu = QuenMatKhauThu.choEmail(this.greenMail, 1);
		assertThat(thu).hasSize(1);
		assertThat(thu[0].getAllRecipients()).extracting(a -> ((InternetAddress) a).getAddress())
			.containsExactly(this.tk.email());
		assertThat(thu[0].getSubject()).isEqualTo("Đặt lại mật khẩu tài khoản chung cư");
		String token = QuenMatKhauThu.token(thu[0]);
		QuenMatKhauThu.NoiDung noiDung = QuenMatKhauThu.noiDung(thu[0]);
		// 01:00Z + 30 phút là 08:30 giờ Việt Nam
		assertThat(noiDung.chuThuong()).contains("Xin chào Người Thử,")
			.contains("Link có hiệu lực tới 08:30 ngày 16/11/2026 và chỉ dùng được một lần.");

		List<Map<String, Object>> dong = QuenMatKhauThu.yeuCauCua(this.jdbc, this.tk.maNguoiDung());
		assertThat(dong).hasSize(1);
		assertThat(dong.get(0).get("token_hash")).isEqualTo(RefreshTokenGenerator.sha256Hex(token));
		Instant taoLuc = NhatKyDb.instant(dong.get(0).get("tao_luc"));
		assertThat(taoLuc).isEqualTo(MutableClock.MAC_DINH);
		assertThat(NhatKyDb.instant(dong.get(0).get("het_han_luc"))).isEqualTo(taoLuc.plus(Duration.ofMinutes(30)));
		assertThat(dong.get(0).get("da_dung_luc")).isNull();
		assertThat(dong.get(0).get("huy_luc")).isNull();
	}

	@Test
	void kiemTraRoiDatLaiThuHoiMoiPhienVaXoaCookie() throws Exception {
		this.jdbc.update("""
				UPDATE nguoi_dung_he_thong SET phai_doi_mat_khau = true, so_lan_dang_nhap_sai = 3,
				    khoa_tam_den = ?, lan_sai_cuoi_luc = ? WHERE ma_nguoi_dung = ?""",
				java.sql.Timestamp.from(MutableClock.MAC_DINH.minusSeconds(1)),
				java.sql.Timestamp.from(MutableClock.MAC_DINH.minusSeconds(60)), this.tk.maNguoiDung());
		String refreshA = this.auth.refreshToken(this.tk.soDienThoai());
		String refreshB = this.auth.refreshToken(this.tk.soDienThoai());
		long phienBan = phienBan();

		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email())).andExpect(status().isOk());
		String token = QuenMatKhauThu.token(QuenMatKhauThu.choEmail(this.greenMail, 1)[0]);

		this.mockMvc.perform(QuenMatKhauThu.kiemTra(token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.hetHanLuc").value("2026-11-16T01:30:00Z"))
			.andExpect(jsonPath("$.message").value("Link đặt lại mật khẩu hợp lệ"));
		assertThat(QuenMatKhauThu.yeuCauCua(this.jdbc, this.tk.maNguoiDung()).get(0).get("da_dung_luc")).isNull();

		this.clock.advance(Duration.ofMinutes(5));
		MvcResult ketQua = this.mockMvc.perform(QuenMatKhauThu.xacNhan(token, MAT_KHAU_MOI))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isEmpty())
			.andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"))
			.andReturn();
		assertThat(AuthApi.setCookies(ketQua)).hasSize(3)
			.anySatisfy(c -> assertThat(c).startsWith("access_token=;"))
			.anySatisfy(c -> assertThat(c).startsWith("refresh_token=;"))
			.anySatisfy(c -> assertThat(c).startsWith("phien=;"))
			.allSatisfy(c -> assertThat(c).contains("Max-Age=0"));

		Instant now = MutableClock.MAC_DINH.plus(Duration.ofMinutes(5));
		Map<String, Object> yeuCau = QuenMatKhauThu.yeuCauCua(this.jdbc, this.tk.maNguoiDung()).get(0);
		assertThat(NhatKyDb.instant(yeuCau.get("da_dung_luc"))).isEqualTo(now);
		Map<String, Object> nguoi = this.jdbc.queryForMap("SELECT * FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				this.tk.maNguoiDung());
		assertThat(nguoi.get("phai_doi_mat_khau")).isEqualTo(false);
		assertThat(NhatKyDb.instant(nguoi.get("doi_mat_khau_luc"))).isEqualTo(now);
		assertThat(((Number) nguoi.get("phien_ban")).longValue()).isEqualTo(phienBan + 1);
		assertThat(nguoi.get("so_lan_dang_nhap_sai")).isEqualTo(0);
		assertThat(nguoi.get("khoa_tam_den")).isNull();
		assertThat(nguoi.get("lan_sai_cuoi_luc")).isNull();
		assertThat(this.jdbc.queryForList(
				"SELECT ly_do_thu_hoi FROM phien_dang_nhap WHERE ma_nguoi_dung = ? AND thu_hoi_luc IS NOT NULL",
				String.class, this.tk.maNguoiDung()))
			.isNotEmpty()
			.containsOnly("Đặt lại mật khẩu");
		this.mockMvc.perform(AuthApi.refreshRequest(refreshA)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(AuthApi.refreshRequest(refreshB)).andExpect(status().isUnauthorized());

		assertThat(this.auth.login(this.tk.soDienThoai(), MAT_KHAU_MOI).getResponse().getStatus()).isEqualTo(200);
		assertThat(this.auth.login(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED).getResponse().getStatus())
			.isEqualTo(401);
	}

	@Test
	void nhatKyCoHaiLoaiMoiVoiNguoiThucHienDung() throws Exception {
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email())).andExpect(status().isOk());
		String token = QuenMatKhauThu.token(QuenMatKhauThu.choEmail(this.greenMail, 1)[0]);
		this.mockMvc.perform(QuenMatKhauThu.xacNhan(token, MAT_KHAU_MOI)).andExpect(status().isOk());

		List<Map<String, Object>> dong = new NhatKyDb(this.jdbc)
			.theoDoiTuong("NguoiDungHeThong:" + this.tk.maNguoiDung());
		assertThat(dong).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("AUTH_YEU_CAU_DAT_LAI_MAT_KHAU", "AUTH_DAT_LAI_MAT_KHAU_QUA_EMAIL");
		assertThat(dong.get(0).get("ma_nguoi_dung")).isNull();
		assertThat(dong.get(1).get("ma_nguoi_dung")).isEqualTo(this.tk.maNguoiDung());
		assertThat(NhatKyDb.chiTiet(dong.get(1)).get("soPhienThuHoi").asInt()).isZero();
		assertThat(dong).allSatisfy(d -> assertThat(d.get("dia_chi_ip")).isNotNull());
		String toanBo = dong.toString();
		assertThat(toanBo).doesNotContain(token)
			.doesNotContain(RefreshTokenGenerator.sha256Hex(token))
			.doesNotContain(this.tk.email());
	}

	long phienBan() {
		Long value = this.jdbc.queryForObject("SELECT phien_ban FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				Long.class, this.tk.maNguoiDung());
		return (value != null) ? value : -1;
	}

}
