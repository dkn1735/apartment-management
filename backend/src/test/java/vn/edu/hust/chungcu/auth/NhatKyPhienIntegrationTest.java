package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/** Nhật ký của đăng xuất và phát hiện dùng lại token (spec 0007, AC-4, AC-5). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class NhatKyPhienIntegrationTest {

	static final String DANG_NHAP = "AUTH_DANG_NHAP_THANH_CONG";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	AuthApi auth;

	PhienDb phien;

	NhatKyDb nhatKy;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.phien = new PhienDb(this.jdbc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	// AC-4: đăng xuất thu hồi được phiên thì đúng một dòng; đăng xuất lại không ghi thêm
	@Test
	void dangXuatGhiMotDong() throws Exception {
		String token = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.phien.hoPhien(token);
		String requestId = "nk-" + UUID.randomUUID();
		this.mockMvc.perform(AuthApi.logoutRequest(token).header("X-Request-Id", requestId)).andExpect(status().isOk());

		List<Map<String, Object>> dong = this.nhatKy.theoRequest(requestId);
		assertThat(dong).hasSize(1);
		Map<String, Object> d = dong.getFirst();
		assertThat(d.get("loai_thao_tac")).isEqualTo("AUTH_DANG_XUAT");
		assertThat(d.get("ma_nguoi_dung")).isEqualTo(this.tk.maNguoiDung());
		assertThat(d.get("doi_tuong_tac_dong")).isEqualTo("NguoiDungHeThong:" + this.tk.maNguoiDung());
		assertThat(NhatKyDb.chiTiet(d)).isEqualTo(NhatKyDb.json("{\"maHoPhien\":\"" + ho + "\"}"));

		for (String lai : new String[] { token, null, "rac-khong-co-trong-db" }) {
			this.mockMvc.perform(AuthApi.logoutRequest(lai)).andExpect(status().isOk());
		}
		assertThat(this.nhatKy.loaiCua(this.tk.maNguoiDung())).containsExactly(DANG_NHAP, "AUTH_DANG_XUAT");
	}

	// AC-4: đăng xuất bằng token đã xoay vòng vẫn ghi đúng chủ phiên và họ phiên của
	// cookie
	@Test
	void dangXuatBangTokenDaXoayGhiDungChuPhien() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.phien.hoPhien(t1);
		lamMoi(t1);
		String requestId = "nk-" + UUID.randomUUID();
		this.mockMvc.perform(AuthApi.logoutRequest(t1).header("X-Request-Id", requestId)).andExpect(status().isOk());

		List<Map<String, Object>> dong = this.nhatKy.theoRequest(requestId);
		assertThat(dong).hasSize(1);
		Map<String, Object> d = dong.getFirst();
		assertThat(d.get("loai_thao_tac")).isEqualTo("AUTH_DANG_XUAT");
		assertThat(d.get("ma_nguoi_dung")).isEqualTo(this.tk.maNguoiDung());
		assertThat(d.get("doi_tuong_tac_dong")).isEqualTo("NguoiDungHeThong:" + this.tk.maNguoiDung());
		assertThat(NhatKyDb.chiTiet(d)).isEqualTo(NhatKyDb.json("{\"maHoPhien\":\"" + ho + "\"}"));
		assertThat(this.phien.soDongConHieuLuc(ho)).isZero();
	}

	// AC-4: đăng xuất trên một thiết bị chỉ ghi họ phiên của thiết bị đó; họ kia không bị
	// đụng và không có dòng nào
	@Test
	void dangXuatMotThietBiKhongGhiHoKhac() throws Exception {
		String tokenA = this.auth.refreshToken(this.tk.soDienThoai());
		String tokenB = this.auth.refreshToken(this.tk.soDienThoai());
		UUID hoA = this.phien.hoPhien(tokenA);
		UUID hoB = this.phien.hoPhien(tokenB);
		assertThat(hoA).isNotEqualTo(hoB);

		this.mockMvc.perform(AuthApi.logoutRequest(tokenA)).andExpect(status().isOk());

		assertThat(this.phien.soDongConHieuLuc(hoA)).isZero();
		assertThat(this.phien.soDongConHieuLuc(hoB)).isEqualTo(1);
		List<Map<String, Object>> dangXuat = this.nhatKy.cua(this.tk.maNguoiDung())
			.stream()
			.filter(d -> "AUTH_DANG_XUAT".equals(d.get("loai_thao_tac")))
			.toList();
		assertThat(dangXuat).hasSize(1);
		assertThat(NhatKyDb.chiTiet(dangXuat.getFirst())).isEqualTo(NhatKyDb.json("{\"maHoPhien\":\"" + hoA + "\"}"));
	}

	// AC-5: dùng lại token quá khoảng khoan dung ghi một dòng dù response là 401
	@Test
	void dungLaiQuaKhoangKhoanDungGhiMotDong() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		UUID ho = this.phien.hoPhien(t1);
		lamMoi(t1);
		this.clock.advance(Duration.ofSeconds(31));
		String requestId = "nk-" + UUID.randomUUID();
		this.mockMvc.perform(AuthApi.refreshRequest(t1).header("X-Request-Id", requestId))
			.andExpect(status().isUnauthorized());

		List<Map<String, Object>> dong = this.nhatKy.theoRequest(requestId);
		assertThat(dong).hasSize(1);
		Map<String, Object> d = dong.getFirst();
		assertThat(d.get("loai_thao_tac")).isEqualTo("AUTH_PHAT_HIEN_DUNG_LAI_TOKEN");
		assertThat(d.get("ma_nguoi_dung")).isEqualTo(this.tk.maNguoiDung());
		assertThat(d.get("doi_tuong_tac_dong")).isEqualTo("NguoiDungHeThong:" + this.tk.maNguoiDung());
		assertThat(NhatKyDb.chiTiet(d)).isEqualTo(NhatKyDb.json("{\"maHoPhien\":\"" + ho + "\"}"));
		assertThat(PhienDb.instant(d.get("thoi_gian_ghi"))).isEqualTo(MutableClock.MAC_DINH.plusSeconds(31));

		// Họ đã chết hẳn: dùng lại lần nữa chỉ bị từ chối, không ghi thêm
		this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isUnauthorized());
		assertThat(this.nhatKy.loaiCua(this.tk.maNguoiDung())).containsExactly(DANG_NHAP,
				"AUTH_PHAT_HIEN_DUNG_LAI_TOKEN");
	}

	// AC-5: làm mới thành công, trong khoảng khoan dung và các lần thất bại khác không
	// ghi
	@Test
	void lamMoiKhacKhongGhi() throws Exception {
		String t1 = this.auth.refreshToken(this.tk.soDienThoai());
		String t2 = lamMoi(t1);
		this.clock.advance(Duration.ofSeconds(5));
		this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isOk());
		this.mockMvc.perform(AuthApi.refreshRequest("rac")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(AuthApi.refreshRequest(null)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(AuthApi.logoutRequest(t2)).andExpect(status().isOk());
		this.clock.advance(Duration.ofSeconds(60));
		// Dùng lại token của họ đã đăng xuất: timConHieuLuc rỗng, chỉ từ chối
		this.mockMvc.perform(AuthApi.refreshRequest(t1)).andExpect(status().isUnauthorized());
		assertThat(this.nhatKy.loaiCua(this.tk.maNguoiDung())).containsExactly(DANG_NHAP, "AUTH_DANG_XUAT");
	}

	private String lamMoi(String token) throws Exception {
		MvcResult result = this.mockMvc.perform(AuthApi.refreshRequest(token)).andExpect(status().isOk()).andReturn();
		return AuthApi.cookieValue(result, "refresh_token").orElseThrow();
	}

}
