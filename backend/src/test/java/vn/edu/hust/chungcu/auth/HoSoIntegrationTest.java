package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.service.AccessTokenService;

/**
 * {@code GET /api/v1/auth/profile} với phạm vi, chi tiết vai trò, khóa giữa chừng (AC-8,
 * AC-9).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class HoSoIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AccessTokenService accessTokens;

	@Autowired
	JwtDecoder jwtDecoder;

	AuthApi auth;

	TaiKhoanThu taiKhoanThu;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.taiKhoanThu = new TaiKhoanThu(this.jdbc);
	}

	// AC-8
	@Test
	void banQuanLyCoMoiToaKhongCoCanHo() throws Exception {
		this.mockMvc.perform(this.auth.profile(this.auth.accessToken("0900000002")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.vaiTro").value(contains("BAN_QUAN_LY")))
			.andExpect(jsonPath("$.data.phamVi.maToaNha").value(contains("A")))
			.andExpect(jsonPath("$.data.phamVi.maCanHo").isEmpty())
			.andExpect(jsonPath("$.data.chiTietVaiTro.banQuanLy.chucVu").value("Trưởng ban"))
			.andExpect(jsonPath("$.data.chiTietVaiTro.banQuanLy.boPhan").value("Vận hành"))
			.andExpect(jsonPath("$.data.chiTietVaiTro.cuDan").doesNotExist());
	}

	// AC-8
	@Test
	void cuDanCoCanHoKhongCoToa() throws Exception {
		this.mockMvc.perform(this.auth.profile(this.auth.accessToken("0900000007")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phamVi.maToaNha").isEmpty())
			.andExpect(jsonPath("$.data.phamVi.maCanHo").value(contains("A-0201")))
			.andExpect(jsonPath("$.data.chiTietVaiTro.cuDan.maCuDan").isString());
	}

	// AC-8: tài khoản hai vai trò, thứ tự mã cố định; JSON không có hash hay CCCD
	@Test
	void banQuanTriKiemCuDanCoCaHaiPhamVi() throws Exception {
		MvcResult result = this.mockMvc.perform(this.auth.profile(this.auth.accessToken("0900000006")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.vaiTro").value(contains("BAN_QUAN_TRI", "CU_DAN")))
			.andExpect(jsonPath("$.data.phamVi.maToaNha").value(contains("A")))
			.andExpect(jsonPath("$.data.phamVi.maCanHo").value(contains("A-0101")))
			.andExpect(jsonPath("$.data.chiTietVaiTro.banQuanTri.nhiemKy").value("2026-2028"))
			.andExpect(jsonPath("$.data.chiTietVaiTro.banQuanTri.vaiTro").value("Trưởng ban"))
			.andReturn();
		String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(body).doesNotContain("matKhauHash", "soCccd", "001026000006", "$2a$");
	}

	// AC-8: hồ sơ hết hiệu lực, kết thúc hôm qua, hay chưa bắt đầu đều không tính;
	// "hôm nay" theo giờ Việt Nam
	// Tự rollback để hồ sơ cư trú thêm vào không lọt sang test đếm dữ liệu seed. MockMvc
	// chạy cùng luồng nên request thấy dữ liệu chưa commit.
	@Test
	@Transactional
	void chiTinhHoSoCuTruConHieuLucHomNayTheoGioVietNam() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String maCuDan = taoCuDan(tk.maNguoiDung());
		hoSoCuTru(maCuDan, "A-0101", "2026-01-01", null, "Hết hiệu lực");
		hoSoCuTru(maCuDan, "A-0201", "2026-01-01", "2026-11-15", "Hợp lệ");
		hoSoCuTru(maCuDan, "A-0301", "2026-11-16", "2026-11-16", "Hợp lệ");
		hoSoCuTru(maCuDan, "A-0101", "2026-11-17", null, "Hợp lệ");

		// 00:30 ngày 16/11 giờ Việt Nam; theo UTC vẫn là 15/11 nên A-0201 sẽ bị tính nhầm
		this.clock.set(Instant.parse("2026-11-15T17:30:00Z"));
		this.mockMvc.perform(this.auth.profile(this.auth.accessToken(tk.soDienThoai())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phamVi.maCanHo").value(contains("A-0301")));
	}

	// AC-8: nhân viên thấy mọi tòa trong bảng toa_nha, cư dân thuần không thấy tòa nào.
	// Tự rollback để tòa B không lọt sang test khác.
	@Test
	@Transactional
	void nhanVienThayMoiToaMoiThemCuDanThuanVanKhongCoToa() throws Exception {
		this.jdbc.update("""
				INSERT INTO toa_nha (ma_toa_nha, ten_toa_nha, so_tang, dia_chi_khu_vuc)
				VALUES ('B', 'Tòa B thử', 2, 'Khu thử')""");

		this.mockMvc.perform(this.auth.profile(this.auth.accessToken("0900000002")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phamVi.maToaNha").value(contains("A", "B")));
		this.mockMvc.perform(this.auth.profile(this.auth.accessToken("0900000007")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phamVi.maToaNha").isEmpty());
	}

	// AC-8: vai trò đọc từ DB mỗi lần gọi, không từ claim vaiTro của token cũ
	@Test
	@Transactional
	void vaiTroDocTuDbNenBoVaiTroNgayKhiTokenCuVanCon() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		this.jdbc.update("INSERT INTO thanh_vien_ban_quan_tri (ma_nguoi_dung, nhiem_ky, vai_tro) VALUES (?, ?, ?)",
				tk.maNguoiDung(), "2026-2028", "Thành viên");
		String token = this.auth.accessToken(tk.soDienThoai());
		this.mockMvc.perform(this.auth.profile(token))
			.andExpect(jsonPath("$.data.vaiTro").value(contains("BAN_QUAN_TRI")));

		this.jdbc.update("DELETE FROM thanh_vien_ban_quan_tri WHERE ma_nguoi_dung = ?", tk.maNguoiDung());

		this.mockMvc.perform(this.auth.profile(token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.vaiTro").isEmpty())
			.andExpect(jsonPath("$.data.phamVi.maToaNha").isEmpty())
			.andExpect(jsonPath("$.data.chiTietVaiTro.banQuanTri").doesNotExist());
		assertThat(this.jwtDecoder.decode(token).getClaimAsStringList("vaiTro")).containsExactly("BAN_QUAN_TRI");
	}

	// AC-9
	@Test
	void taiKhoanBiKhoaSauKhiDangNhapThi403VaXoaCookie() throws Exception {
		TaiKhoanThu.TaiKhoan tk = this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String token = this.auth.accessToken(tk.soDienThoai());
		this.taiKhoanThu.doiTrangThai(tk.maNguoiDung(), "Tạm khóa");

		MvcResult result = this.mockMvc.perform(this.auth.profile(token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"))
			.andReturn();
		expectXoaBaCookie(result);
	}

	// AC-9
	@Test
	void nguoiDungKhongConTonTaiThi401VaXoaCookie() throws Exception {
		String token = this.accessTokens
			.cap("ND99999999", UUID.randomUUID(), List.of(VaiTro.CU_DAN), false, MutableClock.MAC_DINH)
			.value();
		MvcResult result = this.mockMvc.perform(this.auth.profile(token))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
			.andReturn();
		expectXoaBaCookie(result);
	}

	private static void expectXoaBaCookie(MvcResult result) {
		assertThat(AuthApi.setCookies(result)).hasSize(3).allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
		assertThat(AuthApi.setCookie(result, "access_token"))
			.hasValueSatisfying(c -> assertThat(c).contains("Path=/;"));
		assertThat(AuthApi.setCookie(result, "refresh_token"))
			.hasValueSatisfying(c -> assertThat(c).contains("Path=/api/v1/auth;"));
		assertThat(AuthApi.setCookie(result, "phien")).hasValueSatisfying(c -> assertThat(c).contains("Path=/;"));
	}

	private String taoCuDan(String maNguoiDung) {
		String cccd = String.format("%012d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
		return this.jdbc.queryForObject("INSERT INTO cu_dan (ma_nguoi_dung, so_cccd) VALUES (?, ?) RETURNING ma_cu_dan",
				String.class, maNguoiDung, cccd);
	}

	private void hoSoCuTru(String maCuDan, String maCanHo, String batDau, String ketThuc, String trangThai) {
		this.jdbc.update("""
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
				    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so)
				SELECT ?, ?, ma_nv, 'Khách thuê', 'Tạm trú', CAST(? AS date), CAST(? AS date), ?
				FROM nhan_vien_bql LIMIT 1""", maCanHo, maCuDan, batDau, ketThuc, trangThai);
	}

}
