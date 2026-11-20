package vn.edu.hust.chungcu.canho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.TaiKhoanThu;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.phanquyen.ChiCuDanController;
import vn.edu.hust.chungcu.auth.service.AccessTokenService;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.canho.repository.ToaNhaQuery;

/**
 * {@code GET /api/v1/buildings} và lớp kiểm vai trò (spec 0006, AC-2, AC-3, AC-4, AC-7,
 * AC-8, AC-9).
 */
@Import({ SeedTestcontainersConfiguration.class, ChiCuDanController.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class ToaNhaIntegrationTest {

	private static final String BUILDINGS = "/api/v1/buildings";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AccessTokenService accessTokens;

	@MockitoSpyBean
	ToaNhaQuery toaNhaQuery;

	@MockitoSpyBean
	PhamViTruyCapService phamViTruyCap;

	AuthApi auth;

	@BeforeEach
	void setUp() {
		this.auth = new AuthApi(this.mockMvc);
	}

	static Stream<Arguments> taiKhoanSeed() {
		return Stream.of(Arguments.of("0900000001", 403), Arguments.of("0900000002", 200),
				Arguments.of("0900000003", 403), Arguments.of("0900000004", 200), Arguments.of("0900000005", 403),
				Arguments.of("0900000006", 200), Arguments.of("0900000007", 403));
	}

	// AC-2, AC-3, AC-8
	@ParameterizedTest
	@MethodSource("taiKhoanSeed")
	void maTranVaiTro(String soDienThoai, int expected) throws Exception {
		String token = this.auth.accessToken(soDienThoai);
		clearInvocations(this.toaNhaQuery, this.phamViTruyCap);
		var result = this.mockMvc.perform(buildings(token, "")).andExpect(status().is(expected));
		if (expected == 403) {
			expectForbidden(result.andReturn());
			verify(this.phamViTruyCap, never()).cuaNguoiDangGoi();
			verify(this.toaNhaQuery, never()).dem(anyList());
		}
	}

	// AC-3: không vai trò nào thì 403 ở @ChoPhep nhưng vẫn vào được @DaDangNhap
	@Test
	void khongVaiTroThi403NhungVanXemDuocHoSo() throws Exception {
		TaiKhoanThu.TaiKhoan tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		String token = this.auth.accessToken(tk.soDienThoai());
		expectForbidden(this.mockMvc.perform(buildings(token, "")).andReturn());
		expectForbidden(this.mockMvc.perform(get(ChiCuDanController.PATH).cookie(cookie(token))).andReturn());
		this.mockMvc.perform(this.auth.profile(token)).andExpect(status().isOk());
	}

	// AC-3: quyền là hợp của các vai trò
	@Test
	void haiVaiTroGoiDuocCaHaiPhia() throws Exception {
		String token = this.auth.accessToken("0900000006");
		this.mockMvc.perform(buildings(token, "")).andExpect(status().isOk());
		this.mockMvc.perform(get(ChiCuDanController.PATH).cookie(cookie(token))).andExpect(status().isOk());
		expectForbidden(
				this.mockMvc.perform(get(ChiCuDanController.PATH).cookie(cookie(this.auth.accessToken("0900000002"))))
					.andReturn());
	}

	// AC-2: vai trò chỉ lấy từ token, không đọc DB
	@Test
	void vaiTroLayTuTokenKhongDocDb() throws Exception {
		String maBql = this.jdbc.queryForObject(
				"SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000002'", String.class);
		String token = this.accessTokens.cap(maBql, UUID.randomUUID(), List.of(VaiTro.CU_DAN), Instant.now()).value();
		expectForbidden(this.mockMvc.perform(buildings(token, "")).andReturn());
	}

	// AC-2: chưa đăng nhập hay token hỏng là 401, không bao giờ 403
	@Test
	void chuaDangNhapThi401() throws Exception {
		this.mockMvc.perform(get(BUILDINGS))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
		this.mockMvc.perform(buildings("hong", ""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
	}

	// AC-2: 403 đứng trước mọi lỗi 400
	@Test
	void saiVaiTroThi403TruocKhi400() throws Exception {
		expectForbidden(
				this.mockMvc.perform(buildings(this.auth.accessToken("0900000007"), "?page=abc&size=0")).andReturn());
	}

	// AC-4
	@Test
	void tuChoiGhiLogKhongCoQueryHayToken(CapturedOutput output) throws Exception {
		String token = this.auth.accessToken("0900000007");
		MvcResult result = this.mockMvc.perform(buildings(token, "?page=2&bimat=x")).andReturn();
		String requestId = result.getResponse().getHeader("X-Request-Id");
		String dong = output.getOut()
			.lines()
			.filter(l -> l.contains("Từ chối quyền") && l.contains(requestId))
			.findFirst()
			.orElseThrow();
		assertThat(dong).contains("WARN")
			.contains("vaiTro=CU_DAN ")
			.contains("method=GET ")
			.contains("path=/api/v1/buildings ")
			.contains("canCo=BAN_QUAN_LY,BAN_QUAN_TRI,KE_TOAN")
			.doesNotContain("bimat")
			.doesNotContain("page=2")
			.doesNotContain(token);
		String maCuDan = this.jdbc.queryForObject(
				"SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000007'", String.class);
		assertThat(dong).contains("maNguoiDung=" + maCuDan + " ");
	}

	// AC-7
	@Test
	void danhSachMacDinhCoSoCanHo() throws Exception {
		this.mockMvc.perform(buildings(this.auth.accessToken("0900000002"), ""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Lấy danh mục cấu trúc tòa nhà thành công"))
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(20))
			.andExpect(jsonPath("$.data.totalItems").value(1))
			.andExpect(jsonPath("$.data.totalPages").value(1))
			.andExpect(jsonPath("$.data.items[0].maToaNha").value("A"))
			.andExpect(jsonPath("$.data.items[0].tenToaNha").value("Tòa A"))
			.andExpect(jsonPath("$.data.items[0].soTang").value(3))
			.andExpect(jsonPath("$.data.items[0].diaChiKhuVuc").value("Khu đô thị mẫu, Hà Nội"))
			.andExpect(jsonPath("$.data.items[0].soCanHo").value(3));
	}

	// AC-7: trang vượt quá trả items rỗng, tổng vẫn đúng
	@Test
	void trangVuotQuaThiRong() throws Exception {
		clearInvocations(this.toaNhaQuery);
		this.mockMvc.perform(buildings(this.auth.accessToken("0900000004"), "?page=5&size=1&sort=soTang,desc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items").isEmpty())
			.andExpect(jsonPath("$.data.page").value(5))
			.andExpect(jsonPath("$.data.totalItems").value(1))
			.andExpect(jsonPath("$.data.totalPages").value(1));
		verify(this.toaNhaQuery, never()).danhSach(anyList(), any());
	}

	// AC-9
	@Test
	void thamSoSaiThi400TheoTungTruong() throws Exception {
		String token = this.auth.accessToken("0900000002");
		for (String query : List.of("?sort=diaChiKhuVuc,asc", "?sort=tenToaNha", "?sort=tenToaNha,ASC",
				"?sort=a&sort=b", "?sort=%20tenToaNha,asc")) {
			this.mockMvc.perform(buildings(token, query))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[*].field").value(contains("sort")));
		}
		for (String query : List.of("?size=101", "?size=0", "?size=abc")) {
			this.mockMvc.perform(buildings(token, query))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("size")));
		}
		for (String query : List.of("?page=0", "?page=abc", "?page=-1", "?page=1.5")) {
			this.mockMvc.perform(buildings(token, query))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("page")));
		}
		this.mockMvc.perform(buildings(token, "?sort=x,y&size=0&page=0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("page", "size", "sort")));
	}

	// AC-9: rỗng thì dùng mặc định
	@Test
	void thamSoRongThiDungMacDinh() throws Exception {
		this.mockMvc.perform(buildings(this.auth.accessToken("0900000002"), "?page=&size=&sort="))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(20));
	}

	private static MockHttpServletRequestBuilder buildings(String token, String query) {
		return get(BUILDINGS + query).cookie(cookie(token));
	}

	private static Cookie cookie(String token) {
		return new Cookie("access_token", token);
	}

	private static void expectForbidden(MvcResult result) throws Exception {
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
		String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		assertThat(body).contains("\"errorCode\":\"FORBIDDEN\"")
			.contains("\"message\":\"Bạn không có quyền thực hiện thao tác này\"")
			.contains("\"requestId\":\"");
	}

}
