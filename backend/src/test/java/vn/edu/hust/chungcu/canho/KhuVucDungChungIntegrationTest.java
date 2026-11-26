package vn.edu.hust.chungcu.canho;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;

/**
 * {@code GET /api/v1/common-areas} trên dữ liệu seed (spec 0011, AC-11, AC-12, AC-13).
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class KhuVucDungChungIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	AuthApi auth;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
	}

	// AC-11, AC-12
	@Test
	void danhMucMacDinhTheoTen() throws Exception {
		this.mockMvc.perform(ToaNhaApi.khuVuc(this.auth.accessToken(ToaNhaApi.BQL), ""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Lấy danh mục khu vực dùng chung thành công"))
			.andExpect(jsonPath("$.data.totalItems").value(3))
			.andExpect(jsonPath("$.data.items[*].tenKhuVuc")
				.value(contains("Hành lang tầng 2", "Phòng kỹ thuật điện nước", "Sảnh chính")))
			.andExpect(jsonPath("$.data.items[0].viTri").value("Tầng 2"))
			.andExpect(jsonPath("$.data.items[0].maToaNha").value("A"))
			.andExpect(jsonPath("$.data.items[0].tenToaNha").value("Tòa A"))
			.andExpect(
					jsonPath("$.data.items[0].maKhuVuc").value(org.hamcrest.Matchers.matchesPattern("^KV[0-9]{8}$")));
	}

	// AC-11: tìm chứa, không phân biệt hoa thường, không bỏ dấu, thoát ký tự đại diện
	@Test
	void timTheoTen() throws Exception {
		String token = this.auth.accessToken(ToaNhaApi.KY_THUAT);
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "").param("q", "  SẢNH  "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].tenKhuVuc").value(contains("Sảnh chính")));
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "").param("q", "tầng"))
			.andExpect(jsonPath("$.data.items[*].tenKhuVuc").value(contains("Hành lang tầng 2")));
		for (String khongKhop : List.of("sanh", "%", "_", "\\")) {
			this.mockMvc.perform(ToaNhaApi.khuVuc(token, "").param("q", khongKhop))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalItems").value(0));
		}
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "").param("q", "   "))
			.andExpect(jsonPath("$.data.totalItems").value(3));
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "?size=0").param("q", "x".repeat(101)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("size", "q")))
			.andExpect(jsonPath("$.errors[1].message").value("Từ khóa tối đa 100 ký tự"));
	}

	// AC-11: lọc theo tòa, sắp xếp
	@Test
	void locTheoToaVaSapXep() throws Exception {
		String token = this.auth.accessToken(ToaNhaApi.BQL);
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "?maToaNha=a&sort=tenKhuVuc,desc&size=1&page=1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalItems").value(3))
			.andExpect(jsonPath("$.data.items[*].tenKhuVuc").value(contains("Sảnh chính")));
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "?maToaNha="))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalItems").value(3));
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "?maToaNha=KHONGCO"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		this.mockMvc.perform(ToaNhaApi.khuVuc(token, "?maToaNha=KHONGCO&sort=viTri,asc"))
			.andExpect(status().isBadRequest());
	}

	// AC-13
	@Test
	void chiBqlVaKyThuat() throws Exception {
		for (String so : List.of(ToaNhaApi.CU_DAN, ToaNhaApi.KE_TOAN, ToaNhaApi.BQT)) {
			this.mockMvc.perform(ToaNhaApi.khuVuc(this.auth.accessToken(so), ""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		}
	}

}
