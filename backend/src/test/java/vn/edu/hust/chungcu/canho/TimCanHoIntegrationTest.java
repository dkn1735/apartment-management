package vn.edu.hust.chungcu.canho;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
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
 * Tìm, lọc và xem chi tiết căn hộ trên 12 căn seed của tòa A (spec 0012, AC-1 đến AC-6,
 * AC-15, AC-16). Chỉ đọc.
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class TimCanHoIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	AuthApi auth;

	String bql;

	@BeforeEach
	void setUp() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.bql = this.auth.accessToken(ToaNhaApi.BQL);
	}

	// AC-1, AC-4, AC-16: mặc định maCanHo tăng dần, phân trang
	@Test
	void danhSachMacDinhCoPhanTrang() throws Exception {
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&size=5&page=2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Tìm kiếm và lọc danh sách căn hộ thành công"))
			.andExpect(jsonPath("$.data.totalItems").value(12))
			.andExpect(jsonPath("$.data.totalPages").value(3))
			.andExpect(jsonPath("$.data.page").value(2))
			.andExpect(jsonPath("$.data.items[*].maCanHo")
				.value(contains("A-0202", "A-0203", "A-0204", "A-0301", "A-0302")))
			.andExpect(jsonPath("$.data.items[0].maTang").value("A-02"))
			.andExpect(jsonPath("$.data.items[0].soTang").value(2))
			.andExpect(jsonPath("$.data.items[0].maToaNha").value("A"))
			.andExpect(jsonPath("$.data.items[0].tenToaNha").value("Tòa A"))
			.andExpect(jsonPath("$.data.items[0].dienTich").value(52.5))
			.andExpect(jsonPath("$.data.items[0].trangThaiSuDung").value("Đang sửa chữa"))
			.andExpect(jsonPath("$.data.items[0].biTamDungTienIch").value(false));
	}

	// AC-4: trạng thái theo thứ tự nghiệp vụ, khóa phụ mã căn tăng dần
	@Test
	void sapXepTheoThuTuTrangThai() throws Exception {
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&sort=trangThaiSuDung,asc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].maCanHo").value(contains("A-0102", "A-0203", "A-0304", "A-0101",
					"A-0103", "A-0201", "A-0204", "A-0301", "A-0202", "A-0303", "A-0104", "A-0302")));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&sort=dienTich,desc&size=3"))
			.andExpect(jsonPath("$.data.items[*].maCanHo").value(contains("A-0104", "A-0303", "A-0204")));
	}

	// AC-2, AC-16
	@Test
	void locTheoTangDienTichTrangThaiVaMa() throws Exception {
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=a&soTang=2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].maCanHo").value(contains("A-0201", "A-0202", "A-0203", "A-0204")));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&soTang=9"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalItems").value(0));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&dienTichTu=45&dienTichDen=60.25"))
			.andExpect(jsonPath("$.data.items[*].maCanHo").value(contains("A-0102", "A-0103", "A-0202", "A-0304")));
		this.mockMvc
			.perform(CanHoApi.tim(this.bql, "?maToaNha=A").param("trangThaiSuDung", "Trống", "Đã bàn giao", " Trống "))
			.andExpect(jsonPath("$.data.items[*].maCanHo")
				.value(contains("A-0102", "A-0104", "A-0203", "A-0302", "A-0304")));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&q=03"))
			.andExpect(jsonPath("$.data.items[*].maCanHo")
				.value(contains("A-0103", "A-0203", "A-0301", "A-0302", "A-0303", "A-0304")));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&q=a-01"))
			.andExpect(jsonPath("$.data.totalItems").value(4));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "").param("maToaNha", "A").param("q", "%"))
			.andExpect(jsonPath("$.data.items").value(empty()));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&soTang=1&trangThaiSuDung=").param("dienTichTu", " "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalItems").value(4));
	}

	// AC-3: một mục cho mỗi tham số sai, theo thứ tự cố định; 400 trước 404
	@Test
	void thamSoSaiThi400() throws Exception {
		this.mockMvc
			.perform(CanHoApi.tim(this.bql, "")
				.param("maToaNha", "ZZ")
				.param("soTang", "0")
				.param("dienTichTu", "1e2")
				.param("dienTichDen", "1000.01")
				.param("trangThaiSuDung", "Sửa chữa")
				.param("q", "A".repeat(21)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field")
				.value(contains("soTang", "dienTichTu", "dienTichDen", "trangThaiSuDung", "q")))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Số tầng phải là số nguyên từ 1 đến 99",
					"Diện tích lọc phải từ 0 đến 1000, tối đa 2 chữ số thập phân",
					"Diện tích lọc phải từ 0 đến 1000, tối đa 2 chữ số thập phân", "Trạng thái sử dụng không hợp lệ",
					"Từ khóa tìm kiếm tối đa 20 ký tự")));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?soTang=2&dienTichTu=80&dienTichDen=50"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("soTang", "dienTichDen")))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Chọn tòa nhà trước khi lọc theo tầng",
					"Diện tích đến phải lớn hơn hoặc bằng diện tích từ")));
		for (String sai : List.of("50,5", ".5", "+5", "-1", "75.505")) {
			this.mockMvc.perform(CanHoApi.tim(this.bql, "").param("dienTichTu", sai))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("dienTichTu")));
		}
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?sort=maTang,asc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("sort")));
	}

	// AC-2: tòa ngoài phạm vi, không tồn tại hay sai định dạng là 404
	@Test
	void toaNgoaiPhamViThi404() throws Exception {
		for (String ma : List.of("ZZ", "a-b")) {
			this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=" + ma))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
		}
	}

	// AC-5, AC-6: nhân viên theo phạm vi tòa, cư dân chỉ căn đang cư trú
	@Test
	void chiTietTheoHaiLuatQuyen() throws Exception {
		for (String so : List.of(ToaNhaApi.BQL, ToaNhaApi.BQT, ToaNhaApi.KE_TOAN)) {
			this.mockMvc.perform(CanHoApi.chiTiet(this.auth.accessToken(so), "a-0101"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Lấy chi tiết căn hộ thành công"))
				.andExpect(jsonPath("$.data.maCanHo").value("A-0101"))
				.andExpect(jsonPath("$.data.soTang").value(1));
		}
		// 0900000007 chỉ có CU_DAN, cư trú ở A-0201
		String cuDan = this.auth.accessToken(ToaNhaApi.CU_DAN);
		this.mockMvc.perform(CanHoApi.chiTiet(cuDan, "A-0201"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.maCanHo").value("A-0201"));
		for (String ma : List.of("A-0101", "A-0202", "A-9999", "a-b")) {
			this.mockMvc.perform(CanHoApi.chiTiet(cuDan, ma))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
		}
		this.mockMvc.perform(CanHoApi.chiTiet(this.bql, "A-9999")).andExpect(status().isNotFound());
	}

	// AC-15: sai vai trò 403, chưa đăng nhập 401
	@Test
	void phanQuyen() throws Exception {
		String cuDan = this.auth.accessToken(ToaNhaApi.CU_DAN);
		this.mockMvc.perform(CanHoApi.tim(cuDan, "")).andExpect(status().isForbidden());
		String keToan = this.auth.accessToken(ToaNhaApi.KE_TOAN);
		this.mockMvc.perform(CanHoApi.tim(keToan, "?maToaNha=A")).andExpect(status().isOk());
		this.mockMvc.perform(CanHoApi.them(keToan, CanHoApi.body("A-01", 9, 50)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		this.mockMvc.perform(CanHoApi.sua(keToan, "A-0101", 80, "Trống")).andExpect(status().isForbidden());
		String kyThuat = this.auth.accessToken(ToaNhaApi.KY_THUAT);
		this.mockMvc.perform(CanHoApi.tim(kyThuat, "")).andExpect(status().isForbidden());
		this.mockMvc.perform(CanHoApi.chiTiet(kyThuat, "A-0101")).andExpect(status().isForbidden());
		this.mockMvc.perform(CanHoApi.them(kyThuat, CanHoApi.body("A-01", 9, 50))).andExpect(status().isForbidden());
		this.mockMvc.perform(CanHoApi.sua(kyThuat, "A-0101", 80, "Trống")).andExpect(status().isForbidden());
		this.mockMvc
			.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/apartments"))
			.andExpect(status().isUnauthorized());
	}

}
