package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/** Nhật ký của đăng nhập thành công và thất bại (spec 0007, AC-1, AC-2, AC-3). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class NhatKyDangNhapIntegrationTest {

	static final String IP = "203.0.113.7";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	NhatKyDb nhatKy;

	TaiKhoanThu taiKhoanThu;

	String requestId;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.taiKhoanThu = new TaiKhoanThu(this.jdbc);
		this.requestId = "nk-" + UUID.randomUUID();
	}

	// AC-1: đúng một dòng thành công với đủ cột, request_id bằng header của response
	@Test
	void dangNhapThanhCongGhiMotDong() throws Exception {
		TaiKhoanThu.TaiKhoan tk = taoTaiKhoan("Hoạt động");
		MvcResult result = this.mockMvc.perform(login(tk.soDienThoai(), AuthApi.MAT_KHAU_SEED))
			.andExpect(status().isOk())
			.andReturn();
		String refreshToken = AuthApi.cookieValue(result, "refresh_token").orElseThrow();
		UUID hoPhien = new PhienDb(this.jdbc).hoPhien(refreshToken);

		List<Map<String, Object>> dong = this.nhatKy.theoRequest(this.requestId);
		assertThat(dong).hasSize(1);
		Map<String, Object> d = dong.getFirst();
		assertThat(d.get("ma_nguoi_dung")).isEqualTo(tk.maNguoiDung());
		assertThat(d.get("loai_thao_tac")).isEqualTo("AUTH_DANG_NHAP_THANH_CONG");
		assertThat(d.get("doi_tuong_tac_dong")).isEqualTo("NguoiDungHeThong:" + tk.maNguoiDung());
		assertThat(NhatKyDb.chiTiet(d)).isEqualTo(NhatKyDb.json("{\"maHoPhien\":\"" + hoPhien + "\"}"));
		assertThat(d.get("kieu_chi_tiet")).isEqualTo("object");
		assertThat(d.get("dia_chi_ip")).isEqualTo(IP);
		assertThat(d.get("request_id")).isEqualTo(result.getResponse().getHeader("X-Request-Id"));
		assertThat(PhienDb.instant(d.get("thoi_gian_ghi"))).isEqualTo(MutableClock.MAC_DINH);
		assertThat((String) d.get("ma_log")).matches("^LOG[0-9]{8}$");
	}

	// AC-2: sai mật khẩu với tài khoản có thật
	@Test
	void saiMatKhauGhiSaiThongTin() throws Exception {
		TaiKhoanThu.TaiKhoan tk = taoTaiKhoan("Hoạt động");
		this.mockMvc.perform(login(tk.soDienThoai(), "SaiMatKhau@1"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
		assertMotDongThatBai(tk.maNguoiDung(), "SAI_THONG_TIN");
	}

	// AC-2: đúng mật khẩu nhưng tài khoản không hoạt động
	@Test
	void taiKhoanKhoaGhiTaiKhoanKhoa() throws Exception {
		TaiKhoanThu.TaiKhoan tk = taoTaiKhoan("Ngừng hoạt động");
		this.mockMvc.perform(login(tk.email(), AuthApi.MAT_KHAU_SEED))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"));
		assertMotDongThatBai(tk.maNguoiDung(), "TAI_KHOAN_KHOA");
	}

	// AC-3: tài khoản không tồn tại hoặc sai định dạng: không có người, không lưu chuỗi
	// đã
	// gửi
	@ParameterizedTest
	@ValueSource(strings = { "0999999999", "khong-phai-tai-khoan", "ai@khong.co" })
	void taiKhoanKhongTonTaiGhiKhongCoNguoi(String taiKhoan) throws Exception {
		String matKhau = "MatKhauGoNham@9";
		String truoc = this.nhatKy.maLogCuoi();
		this.mockMvc.perform(login(taiKhoan, matKhau))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
		Map<String, Object> d = assertMotDongThatBai(null, "SAI_THONG_TIN");
		assertThat(d.get("doi_tuong_tac_dong")).isEqualTo("NguoiDungHeThong");
		assertThat(d.get("dia_chi_ip")).isEqualTo(IP);
		assertThat(this.nhatKy.toanBoSau(truoc)).doesNotContain(taiKhoan).doesNotContain(matKhau);
	}

	// AC-3: request bị bean validation chặn trước service thì không có dòng nào
	@Test
	void requestThieuTruongKhongGhi() throws Exception {
		this.mockMvc.perform(login("", "")).andExpect(status().isBadRequest());
		this.mockMvc.perform(login("0999999999", " ")).andExpect(status().isBadRequest());
		assertThat(this.nhatKy.theoRequest(this.requestId)).isEmpty();
	}

	private TaiKhoanThu.TaiKhoan taoTaiKhoan(String trangThai) {
		return this.taiKhoanThu.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED), trangThai);
	}

	private Map<String, Object> assertMotDongThatBai(String maNguoiDung, String lyDo) {
		List<Map<String, Object>> dong = this.nhatKy.theoRequest(this.requestId);
		assertThat(dong).hasSize(1);
		Map<String, Object> d = dong.getFirst();
		assertThat(d.get("loai_thao_tac")).isEqualTo("AUTH_DANG_NHAP_THAT_BAI");
		assertThat(d.get("ma_nguoi_dung")).isEqualTo(maNguoiDung);
		if (maNguoiDung != null) {
			assertThat(d.get("doi_tuong_tac_dong")).isEqualTo("NguoiDungHeThong:" + maNguoiDung);
		}
		assertThat(NhatKyDb.chiTiet(d)).isEqualTo(NhatKyDb.json("{\"lyDo\":\"" + lyDo + "\"}"));
		assertThat(d.get("request_id")).isEqualTo(this.requestId);
		return d;
	}

	private MockHttpServletRequestBuilder login(String taiKhoan, String matKhau) {
		return AuthApi.loginRequest(taiKhoan, matKhau).header("X-Request-Id", this.requestId).with(request -> {
			request.setRemoteAddr(IP);
			return request;
		});
	}

}
