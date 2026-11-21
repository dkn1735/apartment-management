package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;

/** Bắt đổi mật khẩu tạm ở lần đăng nhập đầu (spec 0008, AC-13). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class BatDoiMatKhauIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String MAT_KHAU_MOI = "MatKhauMoi2026";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	AuthApi auth;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
	}

	/** Admin tạo một tài khoản BQL, trả số điện thoại và mật khẩu tạm. */
	String[] taoTaiKhoanBql() throws Exception {
		String so = TaiKhoanApi.soMoi();
		Map<String, Object> body = TaiKhoanApi.body("Người Mới", so, "moi" + so + "@chungcu.test",
				List.of(TaiKhoanApi.vaiTro("BAN_QUAN_LY", "chucVu", "Nhân viên", "boPhan", "Vận hành")));
		MvcResult r = this.mockMvc.perform(TaiKhoanApi.tao(this.auth.accessToken(TaiKhoanApi.ADMIN), body))
			.andExpect(status().isCreated())
			.andReturn();
		String matKhauTam = JSON.readTree(r.getResponse().getContentAsString())
			.get("data")
			.get("matKhauTam")
			.asString();
		return new String[] { so, matKhauTam };
	}

	@Test
	void matKhauTamChiGoiDuocHoSoVaDoiMatKhauDoiXongChayNgay() throws Exception {
		String[] tk = taoTaiKhoanBql();
		MvcResult login = this.auth.login(tk[0], tk[1]);
		assertThat(login.getResponse().getStatus()).isEqualTo(200);
		assertThat(
				JSON.readTree(login.getResponse().getContentAsString()).get("data").get("phaiDoiMatKhau").asBoolean())
			.isTrue();
		String access = AuthApi.cookieValue(login, "access_token").orElseThrow();
		String refresh = AuthApi.cookieValue(login, "refresh_token").orElseThrow();

		// API thường bị chặn, kể cả API mà vai trò được phép
		this.mockMvc.perform(get("/api/v1/buildings").cookie(new Cookie("access_token", access)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_PASSWORD_CHANGE_REQUIRED"))
			.andExpect(jsonPath("$.message").value("Bạn cần đổi mật khẩu tạm trước khi tiếp tục."));
		// Hồ sơ vẫn gọi được và báo cờ
		this.mockMvc.perform(this.auth.profile(access))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phaiDoiMatKhau").value(true));
		// Làm mới vẫn giữ cờ vì đọc lại từ DB
		MvcResult lamMoi = this.auth.refresh(refresh);
		assertThat(
				JSON.readTree(lamMoi.getResponse().getContentAsString()).get("data").get("phaiDoiMatKhau").asBoolean())
			.isTrue();
		access = AuthApi.cookieValue(lamMoi, "access_token").orElseThrow();
		this.mockMvc.perform(get("/api/v1/buildings").cookie(new Cookie("access_token", access)))
			.andExpect(status().isForbidden());

		// Đổi mật khẩu bằng mật khẩu tạm làm mật khẩu cũ: nhận access token mới, gọi được
		// ngay
		MvcResult doi = this.mockMvc.perform(DoiMatKhauIntegrationTest.doi(access, tk[1], MAT_KHAU_MOI))
			.andExpect(status().isOk())
			.andReturn();
		String accessMoi = AuthApi.cookieValue(doi, "access_token").orElseThrow();
		this.mockMvc.perform(get("/api/v1/buildings").cookie(new Cookie("access_token", accessMoi)))
			.andExpect(status().isOk());
		this.mockMvc.perform(this.auth.profile(accessMoi)).andExpect(jsonPath("$.data.phaiDoiMatKhau").value(false));
		assertThat(this.jdbc.queryForObject("SELECT phai_doi_mat_khau FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?",
				Boolean.class, tk[0]))
			.isFalse();
	}

	@Test
	void taiKhoanThuongKhongBiChanVaChuaDangNhapVan401() throws Exception {
		String admin = this.auth.accessToken(TaiKhoanApi.ADMIN);
		this.mockMvc.perform(TaiKhoanApi.danhSach(admin)).andExpect(status().isOk());
		this.mockMvc.perform(get("/api/v1/accounts"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
	}

}
