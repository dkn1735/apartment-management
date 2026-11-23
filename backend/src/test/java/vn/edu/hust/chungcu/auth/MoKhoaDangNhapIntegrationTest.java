package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Phía người quản lý: mở khóa sớm, {@code khoaTamDen} ở chi tiết, đặt lại mật khẩu và mở
 * lại xóa khóa, phân quyền (spec 0009, AC-8, AC-9, AC-10, AC-11, AC-16).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class MoKhoaDangNhapIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String SAI = "SaiMatKhau1";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	AuthApi auth;

	String admin;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUp() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.admin = this.auth.accessToken(TaiKhoanApi.ADMIN);
		this.tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	// AC-8, AC-9, AC-11: mở sớm có tác dụng thì ghi nhật ký; gọi lần nữa vẫn 200, không
	// ghi
	@Test
	void adminMoKhoaSom() throws Exception {
		khoa(this.tk.soDienThoai());
		KhoaTamTestSupport.TrangThai truoc = KhoaTamTestSupport.cua(this.jdbc, this.tk.maNguoiDung());
		this.mockMvc.perform(TaiKhoanApi.chiTiet(this.admin, this.tk.maNguoiDung()))
			.andExpect(jsonPath("$.data.khoaTamDen").value(truoc.khoaTamDen().toString()))
			.andExpect(jsonPath("$.data.soLanDangNhapSai").doesNotExist())
			.andExpect(jsonPath("$.data.lanSaiCuoiLuc").doesNotExist());

		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(this.admin, this.tk.maNguoiDung()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Mở khóa đăng nhập thành công"))
			.andExpect(jsonPath("$.data.maNguoiDung").value(this.tk.maNguoiDung()))
			.andExpect(jsonPath("$.data.khoaTamDen").doesNotExist());
		KhoaTamTestSupport.TrangThai sau = KhoaTamTestSupport.cua(this.jdbc, this.tk.maNguoiDung());
		assertThat(sau.khoaTamDen()).isNull();
		assertThat(sau.lanSaiCuoiLuc()).isNull();
		assertThat(sau.phienBan()).isEqualTo(truoc.phienBan());
		assertThat(sau.capNhatLuc()).isEqualTo(truoc.capNhatLuc());
		assertThat(this.auth.login(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED).getResponse().getStatus())
			.isEqualTo(200);

		NhatKyDb nhatKy = new NhatKyDb(this.jdbc);
		List<Map<String, Object>> dong = nhatKy.theoDoiTuong("NguoiDungHeThong:" + this.tk.maNguoiDung())
			.stream()
			.filter((d) -> "AUTH_MO_KHOA_TAM_DANG_NHAP".equals(d.get("loai_thao_tac")))
			.toList();
		assertThat(dong).hasSize(1);
		assertThat(dong.get(0).get("ma_nguoi_dung")).isEqualTo(TaiKhoanApi.maTheoSo(this.jdbc, TaiKhoanApi.ADMIN));
		assertThat(JSON.readTree((String) dong.get(0).get("chi_tiet")).get("khoaTamDen").asString())
			.isEqualTo(truoc.khoaTamDen().toString());

		String maLog = nhatKy.maLogCuoi();
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(this.admin, this.tk.maNguoiDung())).andExpect(status().isOk());
		assertThat(nhatKy.toanBoSau(maLog)).doesNotContain("AUTH_MO_KHOA_TAM_DANG_NHAP");
	}

	// AC-9: khóa đã tới hạn thì chi tiết trả null
	@Test
	void khoaHetHanThiChiTietLaNull() throws Exception {
		khoa(this.tk.soDienThoai());
		this.clock.advance(Duration.ofMinutes(15));
		this.mockMvc.perform(TaiKhoanApi.chiTiet(this.admin, this.tk.maNguoiDung()))
			.andExpect(jsonPath("$.data.khoaTamDen").doesNotExist());
		this.mockMvc.perform(TaiKhoanApi.danhSach(this.admin))
			.andExpect(jsonPath("$.data.items[0].khoaTamDen").doesNotExist());
	}

	// AC-8, AC-16: phạm vi, vai trò, chưa đăng nhập
	@Test
	void phanQuyenVaPhamVi() throws Exception {
		String bql = this.auth.accessToken(TaiKhoanApi.BQL);
		khoa("0900000007");
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(bql, TaiKhoanApi.maTheoSo(this.jdbc, "0900000007")))
			.andExpect(status().isOk());
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(bql, TaiKhoanApi.maTheoSo(this.jdbc, TaiKhoanApi.KY_THUAT)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản"));
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(this.admin, "ND99999999")).andExpect(status().isNotFound());
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(this.admin, "sai-dang")).andExpect(status().isNotFound());

		String cuDan = this.auth.accessToken("0900000007");
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(cuDan, this.tk.maNguoiDung()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap("khong-hop-le", this.tk.maNguoiDung()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
	}

	// AC-8: tự mở cho chính mình được
	@Test
	void tuMoChoChinhMinh() throws Exception {
		this.mockMvc.perform(TaiKhoanApi.moKhoaDangNhap(this.admin, TaiKhoanApi.maTheoSo(this.jdbc, TaiKhoanApi.ADMIN)))
			.andExpect(status().isOk());
	}

	// AC-10: đặt lại mật khẩu khi đang khóa thì đăng nhập được ngay bằng mật khẩu tạm
	@Test
	void datLaiMatKhauXoaKhoa() throws Exception {
		khoa(this.tk.soDienThoai());
		MvcResult r = this.mockMvc.perform(TaiKhoanApi.datLai(this.admin, this.tk.maNguoiDung()))
			.andExpect(status().isOk())
			.andReturn();
		String matKhauTam = JSON.readTree(r.getResponse().getContentAsString())
			.get("data")
			.get("matKhauTam")
			.asString();
		assertThat(this.auth.login(this.tk.soDienThoai(), matKhauTam).getResponse().getStatus()).isEqualTo(200);
	}

	// AC-10: mở lại từ Tạm khóa vẫn đặt lại bộ đếm và khóa
	@Test
	void moLaiTuTamKhoaXoaKhoa() throws Exception {
		khoa(this.tk.soDienThoai());
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, this.tk.maNguoiDung(), "Tạm khóa", "Thử khóa tạm"))
			.andExpect(status().isOk());
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, this.tk.maNguoiDung(), "Hoạt động", null))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.khoaTamDen").doesNotExist());
		assertThat(this.auth.login(this.tk.soDienThoai(), AuthApi.MAT_KHAU_SEED).getResponse().getStatus())
			.isEqualTo(200);
	}

	private void khoa(String taiKhoan) throws Exception {
		for (int i = 0; i < 5; i++) {
			this.auth.login(taiKhoan, SAI);
		}
		assertThat(this.auth.login(taiKhoan, SAI).getResponse().getStatus()).isEqualTo(429);
	}

}
