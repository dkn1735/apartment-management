package vn.edu.hust.chungcu.canho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.canho.repository.ToaNhaQuery;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * {@code POST /api/v1/buildings} và ràng buộc của V15 (spec 0011, AC-1, AC-2, AC-3, AC-6,
 * AC-7, AC-13, AC-14).
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class ThemToaNhaIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	ToaNhaQuery toaNhaQuery;

	AuthApi auth;

	NhatKyDb nhatKy;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
	}

	@AfterEach
	void tearDown() {
		ToaNhaApi.donDep(this.jdbc);
	}

	// AC-1, AC-6, AC-7, AC-14
	@Test
	void bqlThemToaTaoDuTangVaGhiNhatKy() throws Exception {
		String ma = ToaNhaApi.maMoi();
		MvcResult result = this.mockMvc
			.perform(ToaNhaApi.themToa(this.auth.accessToken(ToaNhaApi.BQL),
					ToaNhaApi.body("  " + ma.toLowerCase() + " ", "  Tòa   thử\t" + ma + " ", 5, " Khu  mới ")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("Thêm mới tòa nhà thành công"))
			.andExpect(jsonPath("$.data.maToaNha").value(ma))
			.andExpect(jsonPath("$.data.tenToaNha").value("Tòa thử " + ma))
			.andExpect(jsonPath("$.data.soTang").value(5))
			.andExpect(jsonPath("$.data.diaChiKhuVuc").value("Khu mới"))
			.andExpect(jsonPath("$.data.soCanHo").value(0))
			.andReturn();
		assertThat(result.getResponse().getHeader("Location")).isNull();
		assertThat(this.jdbc.queryForList("SELECT ma_tang FROM tang WHERE ma_toa_nha = ? ORDER BY so_tang",
				String.class, ma))
			.containsExactly(ma + "-01", ma + "-02", ma + "-03", ma + "-04", ma + "-05");
		List<Map<String, Object>> dong = this.nhatKy.theoRequest(result.getResponse().getHeader("X-Request-Id"));
		assertThat(dong).hasSize(1);
		assertThat(dong.get(0).get("loai_thao_tac")).isEqualTo("CANHO_THEM_TOA_NHA");
		assertThat(dong.get(0).get("doi_tuong_tac_dong")).isEqualTo("ToaNha:" + ma);
		assertThat(dong.get(0).get("chi_tiet")).isEqualTo("{\"soTang\": 5}");
		assertThat(dong.get(0).get("ma_nguoi_dung")).isEqualTo(this.jdbc.queryForObject(
				"SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?", String.class, ToaNhaApi.BQL));
	}

	// AC-1: lưu NFC
	@Test
	void tenDangNfdDuocLuuNfc() throws Exception {
		String ma = ToaNhaApi.maMoi();
		String nfd = Normalizer.normalize("Tòa Hướng Dương " + ma, Normalizer.Form.NFD);
		this.mockMvc.perform(ToaNhaApi.themToa(this.auth.accessToken(ToaNhaApi.BQL), ToaNhaApi.body(ma, nfd, 1, "Khu")))
			.andExpect(status().isCreated());
		assertThat(this.jdbc.queryForObject("SELECT ten_toa_nha FROM toa_nha WHERE ma_toa_nha = ?", String.class, ma))
			.isEqualTo(Normalizer.normalize(nfd, Normalizer.Form.NFC));
	}

	// AC-2
	@Test
	void duLieuRongBaoBonLoiTheoThuTu() throws Exception {
		this.mockMvc.perform(ToaNhaApi.themToaRaw(this.auth.accessToken(ToaNhaApi.BQL), "{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("maToaNha", "tenToaNha", "soTang", "diaChiKhuVuc")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Vui lòng nhập mã tòa nhà", "Vui lòng nhập tên tòa nhà",
						"Số tầng phải là số nguyên từ 1 đến 99", "Vui lòng nhập địa chỉ khu vực")));
	}

	// AC-2
	@Test
	void soTangSaiKieuLaLoiTruong() throws Exception {
		String token = this.auth.accessToken(ToaNhaApi.BQL);
		for (String soTang : List.of("0", "100", "\"abc\"", "\"3\"", "3.0", "3.5", "null", "true", "[3]", "{}",
				"99999999999")) {
			this.mockMvc
				.perform(ToaNhaApi.themToaRaw(token,
						"{\"maToaNha\":\"" + ToaNhaApi.maMoi()
								+ "\",\"tenToaNha\":\"Tòa x\",\"diaChiKhuVuc\":\"Khu\",\"soTang\":" + soTang + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("soTang")))
				.andExpect(jsonPath("$.errors[0].message").value("Số tầng phải là số nguyên từ 1 đến 99"));
		}
	}

	// AC-2
	@Test
	void dinhDangVaDoDai() throws Exception {
		String token = this.auth.accessToken(ToaNhaApi.BQL);
		this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body("a-1", "x".repeat(101), 3, "y".repeat(256))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Mã tòa nhà gồm 1 đến 10 chữ cái không dấu hoặc chữ số", "Tên tòa nhà tối đa 100 ký tự",
						"Địa chỉ khu vực tối đa 255 ký tự")));
		for (String ma : List.of("ABCDEFGHIJK", "TÒA", "A B")) {
			this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body(ma, "Tòa y", 3, "Khu")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("maToaNha")));
		}
		// Đủ 100 ký tự (đếm code point) và mã 10 ký tự vẫn hợp lệ
		String ma = "T" + "1".repeat(9);
		this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body(ma, "ồ".repeat(100), 1, "Khu")))
			.andExpect(status().isCreated());
	}

	// AC-2: body không phải JSON đối tượng
	@Test
	void bodyHongLaMalformed() throws Exception {
		String token = this.auth.accessToken(ToaNhaApi.BQL);
		for (String body : List.of("[]", "abc", "")) {
			this.mockMvc.perform(ToaNhaApi.themToaRaw(token, body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
		}
	}

	// AC-3: trùng tên không phân biệt hoa thường, trùng cả hai thì hai mục theo thứ tự
	@Test
	void trungMaHoacTenThi409() throws Exception {
		String token = this.auth.accessToken(ToaNhaApi.BQL);
		this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body(ToaNhaApi.maMoi(), "tòa a", 3, "Khu")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CANHO_TOA_NHA_DA_TON_TAI"))
			.andExpect(jsonPath("$.message").value("Tòa nhà đã tồn tại"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("tenToaNha")))
			.andExpect(jsonPath("$.errors[0].message").value("Tên tòa nhà đã được dùng"));
		this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body("a", "Tòa mới hẳn", 3, "Khu")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors[*].field").value(contains("maToaNha")))
			.andExpect(jsonPath("$.errors[0].message").value("Mã tòa nhà đã được dùng"));
		this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body("A", "TÒA A", 3, "Khu")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors[*].field").value(contains("maToaNha", "tenToaNha")));
		assertThat(this.jdbc.queryForObject("SELECT so_tang FROM toa_nha WHERE ma_toa_nha = 'A'", Integer.class))
			.isEqualTo(3);
	}

	// AC-3: hai request vượt qua bước kiểm cùng lúc, ràng buộc DB chặn và đổi thành 409
	@Test
	void rangBuocDbDoiThanh409KhongSotTang() throws Exception {
		String token = this.auth.accessToken(ToaNhaApi.BQL);
		doReturn(new ToaNhaQuery.Trung(false, false)).when(this.toaNhaQuery).trung(anyString(), anyString());
		this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body("A", "Tòa khác hẳn", 4, "Khu")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CANHO_TOA_NHA_DA_TON_TAI"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("maToaNha")));
		String ma = ToaNhaApi.maMoi();
		MvcResult result = this.mockMvc.perform(ToaNhaApi.themToa(token, ToaNhaApi.body(ma, "TÒA A", 4, "Khu")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors[*].field").value(contains("tenToaNha")))
			.andReturn();
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM tang WHERE ma_toa_nha = ?", Long.class, ma)).isZero();
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM tang WHERE ma_toa_nha = 'A'", Long.class))
			.isEqualTo(3);
		// AC-14: lỗi không ghi nhật ký
		assertThat(this.nhatKy.theoRequest(result.getResponse().getHeader("X-Request-Id"))).isEmpty();
	}

	// AC-13
	@Test
	void chiBqlDuocThem() throws Exception {
		for (String so : List.of(ToaNhaApi.KE_TOAN, ToaNhaApi.KY_THUAT, ToaNhaApi.BQT, ToaNhaApi.CU_DAN)) {
			this.mockMvc.perform(ToaNhaApi.themToa(this.auth.accessToken(so), ToaNhaApi.body("", "", null, "")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		}
	}

	// AC-7: DB chặn dữ liệu chèn tay sai
	@Test
	void checkCuaV15ChanDuLieuSai() {
		for (String sql : List.of("INSERT INTO tang (ma_tang, ma_toa_nha, so_tang) VALUES ('A-7', 'A', 7)",
				"INSERT INTO tang (ma_tang, ma_toa_nha, so_tang) VALUES ('A-08', 'A', 9)",
				"INSERT INTO tang (ma_tang, ma_toa_nha, so_tang) VALUES ('A-100', 'A', 100)",
				"INSERT INTO toa_nha (ma_toa_nha, ten_toa_nha, so_tang, dia_chi_khu_vuc) VALUES ('a-1', 'X1', 1, 'K')",
				"INSERT INTO toa_nha (ma_toa_nha, ten_toa_nha, so_tang, dia_chi_khu_vuc) VALUES ('TX', 'X2', 100, 'K')",
				"INSERT INTO toa_nha (ma_toa_nha, ten_toa_nha, so_tang, dia_chi_khu_vuc) VALUES ('TY', 'TÒA A', 1, 'K')")) {
			assertThatThrownBy(() -> this.jdbc.update(sql)).as(sql).isInstanceOf(DataIntegrityViolationException.class);
		}
		assertThat(Arrays.asList(this.jdbc.queryForObject(
				"SELECT count(*) FROM pg_constraint WHERE conname = 'toa_nha_ten_toa_nha_key'", Long.class)))
			.containsExactly(0L);
	}

}
