package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.TreeMap;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;

/**
 * Định danh không có tài khoản bị đếm và khóa y hệt tài khoản thật (spec 0009, AC-5,
 * AC-7, AC-19).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class KhoaTamDinhDanhIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String SAI = "SaiMatKhau1";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		clearInvocations(this.passwordEncoder);
	}

	// AC-5: mỗi lượt một lần soKhopGia; lần 5 là 429; đang khóa thì không bcrypt
	@Test
	void dinhDanhAoBiKhoaNhuTaiKhoanThat() throws Exception {
		String ao = TaiKhoanApi.soMoi();
		for (int i = 1; i <= 4; i++) {
			login(ao, SAI).andExpect(status().isUnauthorized());
		}
		KhoaTamDangNhapIntegrationTest.expectKhoa(login(ao, SAI), 900);
		verify(this.passwordEncoder, times(5)).matches(any(), anyString());

		clearInvocations(this.passwordEncoder);
		KhoaTamDangNhapIntegrationTest.expectKhoa(login(ao, AuthApi.MAT_KHAU_SEED), 900);
		verify(this.passwordEncoder, never()).matches(any(), anyString());

		// Một dòng, khóa là SHA-256 của định danh, không cột nào chứa chuỗi gốc
		assertThat(KhoaTamTestSupport.soDongDinhDanh(this.jdbc)).isEqualTo(1);
		Map<String, Object> dong = KhoaTamTestSupport.dinhDanh(this.jdbc, ao);
		assertThat(dong).isNotNull();
		assertThat(dong.get("dinh_danh_hash")).isEqualTo(RefreshTokenGenerator.sha256Hex(ao));
		assertThat(dong.values()).noneMatch((v) -> v != null && v.toString().contains(ao));
	}

	// AC-5: chuỗi sai định dạng không được đếm
	@Test
	void saiDinhDangKhongDem() throws Exception {
		for (int i = 0; i < 10; i++) {
			login("abc", SAI).andExpect(status().isUnauthorized());
		}
		assertThat(KhoaTamTestSupport.soDongDinhDanh(this.jdbc)).isZero();
	}

	// AC-5: số điện thoại và email của cùng tài khoản dùng chung một bộ đếm
	@Test
	void soDienThoaiVaEmailDungChungBoDem() throws Exception {
		TaiKhoanThu.TaiKhoan tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		for (int i = 0; i < 3; i++) {
			login(tk.soDienThoai(), SAI).andExpect(status().isUnauthorized());
		}
		login(tk.email().toUpperCase(), SAI).andExpect(status().isUnauthorized());
		KhoaTamDangNhapIntegrationTest.expectKhoa(login(tk.email(), SAI), 900);
		assertThat(KhoaTamTestSupport.soDongDinhDanh(this.jdbc)).isZero();
	}

	// AC-7: cùng thời điểm khóa thì status, body (trừ requestId) và header giống hệt
	@Test
	void phanHoiKhoaKhongLoTaiKhoanTonTai() throws Exception {
		TaiKhoanThu.TaiKhoan tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String ao = TaiKhoanApi.soMoi();
		for (int i = 0; i < 5; i++) {
			login(tk.soDienThoai(), SAI);
			login(ao, SAI);
		}
		MvcResult that = login(tk.soDienThoai(), SAI).andReturn();
		MvcResult gia = login(ao, SAI).andReturn();
		assertThat(that.getResponse().getStatus()).isEqualTo(429).isEqualTo(gia.getResponse().getStatus());
		assertThat(boRequestId(that)).isEqualTo(boRequestId(gia));
		assertThat(headers(that)).isEqualTo(headers(gia));
	}

	// AC-19: tạo tài khoản với số đang bị khóa ảo thì dòng biến mất, sai lần nữa là 401
	@Test
	void taoTaiKhoanXoaDongDinhDanhAo() throws Exception {
		String admin = new AuthApi(this.mockMvc).accessToken(TaiKhoanApi.ADMIN);
		Map<String, Object> body = TaiKhoanApi.cuDanMoi();
		String so = (String) body.get("soDienThoai");
		String email = (String) body.get("email");
		khoaAo(so);
		khoaAo(email);
		this.mockMvc.perform(TaiKhoanApi.tao(admin, body)).andExpect(status().isCreated());
		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, so)).isNull();
		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, email)).isNull();
		login(so, SAI).andExpect(status().isUnauthorized());
	}

	// AC-19: sửa email sang một email đang bị khóa ảo
	@Test
	void suaEmailXoaDongDinhDanhAo() throws Exception {
		String admin = new AuthApi(this.mockMvc).accessToken(TaiKhoanApi.ADMIN);
		TaiKhoanThu.TaiKhoan tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String emailMoi = "moi" + TaiKhoanApi.soMoi() + "@chungcu.test";
		khoaAo(emailMoi);
		Long phienBan = this.jdbc.queryForObject("SELECT phien_ban FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				Long.class, tk.maNguoiDung());
		Map<String, Object> sua = Map.of("hoTen", "Người Thử", "soDienThoai", tk.soDienThoai(), "email", emailMoi,
				"phienBan", phienBan);
		this.mockMvc.perform(TaiKhoanApi.sua(admin, tk.maNguoiDung(), sua)).andExpect(status().isOk());
		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, emailMoi)).isNull();
		login(emailMoi, SAI).andExpect(status().isUnauthorized());
	}

	// AC-5: định danh ảo đếm theo dạng đã chuẩn hóa, +84 có dấu cách và chữ hoa dùng
	// chung dòng
	@Test
	void dinhDanhAoChuanHoaDungChungMotDong() throws Exception {
		String so = TaiKhoanApi.soMoi();
		String soQuocTe = "+84 " + so.substring(1, 3) + " " + so.substring(3, 6) + " " + so.substring(6);
		login(soQuocTe, SAI).andExpect(status().isUnauthorized());
		login(so, SAI).andExpect(status().isUnauthorized());
		login("+84" + so.substring(1), SAI).andExpect(status().isUnauthorized());
		login(soQuocTe, SAI).andExpect(status().isUnauthorized());
		KhoaTamDangNhapIntegrationTest.expectKhoa(login(so, SAI), 900);

		String email = "ao" + so + "@chungcu.test";
		for (int i = 0; i < 4; i++) {
			login(email.toUpperCase(), SAI).andExpect(status().isUnauthorized());
		}
		KhoaTamDangNhapIntegrationTest.expectKhoa(login(email, SAI), 900);

		assertThat(KhoaTamTestSupport.soDongDinhDanh(this.jdbc)).isEqualTo(2);
		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, so)).isNotNull();
		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, email)).isNotNull();
	}

	// AC-19: tạo tài khoản nhập +84 và email chữ hoa vẫn xóa đúng dòng của dạng chuẩn
	// hóa
	@Test
	void taoTaiKhoanNhapChuaChuanHoaVanXoaDung() throws Exception {
		String admin = new AuthApi(this.mockMvc).accessToken(TaiKhoanApi.ADMIN);
		Map<String, Object> body = TaiKhoanApi.cuDanMoi();
		String so = (String) body.get("soDienThoai");
		String email = (String) body.get("email");
		khoaAo(so);
		khoaAo(email);
		body.put("soDienThoai", "+84 " + so.substring(1));
		body.put("email", email.toUpperCase());

		this.mockMvc.perform(TaiKhoanApi.tao(admin, body)).andExpect(status().isCreated());

		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, so)).isNull();
		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, email)).isNull();
		login(email, SAI).andExpect(status().isUnauthorized());
	}

	// AC-19: sửa số điện thoại sang số đang bị khóa ảo, nhập dạng +84
	@Test
	void suaSoDienThoaiXoaDongDinhDanhAo() throws Exception {
		String admin = new AuthApi(this.mockMvc).accessToken(TaiKhoanApi.ADMIN);
		TaiKhoanThu.TaiKhoan tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String soMoi = TaiKhoanApi.soMoi();
		khoaAo(soMoi);
		Long phienBan = this.jdbc.queryForObject("SELECT phien_ban FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				Long.class, tk.maNguoiDung());
		Map<String, Object> sua = Map.of("hoTen", "Người Thử", "soDienThoai", "+84" + soMoi.substring(1), "email",
				tk.email(), "phienBan", phienBan);

		this.mockMvc.perform(TaiKhoanApi.sua(admin, tk.maNguoiDung(), sua)).andExpect(status().isOk());

		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, soMoi)).isNull();
		login(soMoi, SAI).andExpect(status().isUnauthorized());
	}

	private void khoaAo(String dinhDanh) throws Exception {
		for (int i = 0; i < 5; i++) {
			login(dinhDanh, SAI);
		}
		login(dinhDanh, SAI).andExpect(status().isTooManyRequests());
	}

	private ResultActions login(String taiKhoan, String matKhau) throws Exception {
		return this.mockMvc.perform(AuthApi.loginRequest(taiKhoan, matKhau));
	}

	private static ObjectNode boRequestId(MvcResult result) throws Exception {
		ObjectNode body = (ObjectNode) JSON.readTree(result.getResponse().getContentAsString());
		body.remove("requestId");
		return body;
	}

	/** Mọi header trừ giá trị của {@code X-Request-Id} (vẫn phải có ở cả hai). */
	private static Map<String, String> headers(MvcResult result) {
		Map<String, String> h = new TreeMap<>();
		for (String ten : result.getResponse().getHeaderNames()) {
			h.put(ten, ten.equalsIgnoreCase("X-Request-Id") ? "*"
					: String.join(",", result.getResponse().getHeaders(ten)));
		}
		return h;
	}

}
