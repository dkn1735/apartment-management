package vn.edu.hust.chungcu.canho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
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
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Chi tiết tòa, danh sách tầng và thêm tầng (spec 0011, AC-4, AC-5, AC-7, AC-8, AC-9,
 * AC-13, AC-14).
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class TangIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	AuthApi auth;

	NhatKyDb nhatKy;

	String bql;

	@BeforeEach
	void setUp() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.bql = this.auth.accessToken(ToaNhaApi.BQL);
	}

	@AfterEach
	void tearDown() {
		ToaNhaApi.donDep(this.jdbc);
	}

	private String taoToa(int soTang) throws Exception {
		String ma = ToaNhaApi.maMoi();
		this.mockMvc.perform(ToaNhaApi.themToa(this.bql, ToaNhaApi.toaMoi(ma, soTang))).andExpect(status().isCreated());
		return ma;
	}

	// Happy path: AC-1, AC-4, AC-6, AC-7, AC-8, AC-9, AC-14
	@Test
	void themTangNoiTiepRoiXemLai() throws Exception {
		String ma = taoToa(5);
		MvcResult result = this.mockMvc.perform(ToaNhaApi.themTang(this.bql, ma.toLowerCase(), 6))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("Thêm tầng mới vào tòa nhà thành công"))
			.andExpect(jsonPath("$.data.maTang").value(ma + "-06"))
			.andExpect(jsonPath("$.data.soTang").value(6))
			.andExpect(jsonPath("$.data.soCanHo").value(0))
			.andReturn();
		var dong = this.nhatKy.theoRequest(result.getResponse().getHeader("X-Request-Id"));
		assertThat(dong).hasSize(1);
		assertThat(dong.get(0).get("loai_thao_tac")).isEqualTo("CANHO_THEM_TANG");
		assertThat(dong.get(0).get("doi_tuong_tac_dong")).isEqualTo("Tang:" + ma + "-06");
		assertThat(dong.get(0).get("chi_tiet")).isEqualTo("{\"soTang\": 6, \"maToaNha\": \"" + ma + "\"}");

		this.mockMvc.perform(ToaNhaApi.dsTang(this.auth.accessToken(ToaNhaApi.KY_THUAT), ma, ""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Lấy danh sách tầng theo tòa nhà thành công"))
			.andExpect(jsonPath("$.data.totalItems").value(6))
			.andExpect(jsonPath("$.data.items[*].maTang")
				.value(contains(ma + "-01", ma + "-02", ma + "-03", ma + "-04", ma + "-05", ma + "-06")));
		this.mockMvc.perform(ToaNhaApi.chiTiet(this.bql, ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Lấy thông tin tòa nhà thành công"))
			.andExpect(jsonPath("$.data.soTang").value(6))
			.andExpect(jsonPath("$.data.soCanHo").value(0));
	}

	// AC-5
	@Test
	void tangTrungHoacNhayCocThi422() throws Exception {
		String ma = taoToa(3);
		for (int soTang : List.of(3, 1, 5)) {
			MvcResult result = this.mockMvc.perform(ToaNhaApi.themTang(this.bql, ma, soTang))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errorCode").value("CANHO_TANG_SAI_THU_TU"))
				.andExpect(jsonPath("$.message").value("Tầng mới phải nối tiếp tầng cao nhất của tòa"))
				.andExpect(jsonPath("$.errors[*].field").value(contains("soTang")))
				.andExpect(jsonPath("$.errors[0].message").value("Tầng tiếp theo của tòa này là tầng 4"))
				.andReturn();
			assertThat(this.nhatKy.theoRequest(result.getResponse().getHeader("X-Request-Id"))).isEmpty();
		}
		assertThat(this.jdbc.queryForObject("SELECT so_tang FROM toa_nha WHERE ma_toa_nha = ?", Integer.class, ma))
			.isEqualTo(3);
	}

	// AC-5: trần 99 thắng khi cả hai cùng sai; 100 là lỗi 400
	@Test
	void toaDu99TangThi422TranTang() throws Exception {
		String ma = taoToa(99);
		for (int soTang : List.of(99, 50)) {
			this.mockMvc.perform(ToaNhaApi.themTang(this.bql, ma, soTang))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errors[0].message").value("Tòa đã đạt tối đa 99 tầng"));
		}
		this.mockMvc.perform(ToaNhaApi.themTang(this.bql, ma, 100))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Số tầng phải là số nguyên từ 1 đến 99"));
	}

	// AC-5: 400 trước 404; mã sai định dạng hay không có là 404
	@Test
	void thuTuKiemVaPhamVi() throws Exception {
		this.mockMvc.perform(ToaNhaApi.themTang(this.bql, "KHONGCO", "4"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("soTang")));
		for (String ma : List.of("KHONGCO", "a-b")) {
			this.mockMvc.perform(ToaNhaApi.themTang(this.bql, ma, 4))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
			this.mockMvc.perform(ToaNhaApi.chiTiet(this.bql, ma)).andExpect(status().isNotFound());
			this.mockMvc.perform(ToaNhaApi.dsTang(this.bql, ma, "")).andExpect(status().isNotFound());
		}
		this.mockMvc.perform(ToaNhaApi.dsTang(this.bql, "KHONGCO", "?sort=dienTich,asc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("sort")));
	}

	// AC-5, AC-7: hai BQL cùng thêm tầng 4, đúng một người thành công
	@Test
	void haiRequestSongSongChiMotThanhCong() throws Exception {
		String ma = taoToa(3);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch batDau = new CountDownLatch(1);
		try {
			List<Future<Integer>> ketQua = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				ketQua.add(pool.submit(() -> {
					batDau.await();
					return this.mockMvc.perform(ToaNhaApi.themTang(this.bql, ma, 4))
						.andReturn()
						.getResponse()
						.getStatus();
				}));
			}
			batDau.countDown();
			List<Integer> trangThai = new ArrayList<>();
			for (Future<Integer> f : ketQua) {
				trangThai.add(f.get(30, TimeUnit.SECONDS));
			}
			assertThat(trangThai).containsExactlyInAnyOrder(201, 422);
		}
		finally {
			pool.shutdownNow();
		}
		assertThat(this.jdbc.queryForObject("SELECT so_tang FROM toa_nha WHERE ma_toa_nha = ?", Integer.class, ma))
			.isEqualTo(4);
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM tang WHERE ma_toa_nha = ?", Long.class, ma))
			.isEqualTo(4);
	}

	// AC-8: số căn hộ mỗi tầng, sắp xếp, phân trang
	@Test
	void danhSachTangCuaToaSeed() throws Exception {
		this.mockMvc.perform(ToaNhaApi.dsTang(this.bql, "a", "?sort=maTang,desc&size=2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalItems").value(3))
			.andExpect(jsonPath("$.data.totalPages").value(2))
			.andExpect(jsonPath("$.data.items[*].maTang").value(contains("A-03", "A-02")))
			.andExpect(jsonPath("$.data.items[*].soCanHo").value(contains(4, 4)));
	}

	// AC-9, AC-13
	@Test
	void phanQuyenXemVaThem() throws Exception {
		for (String so : List.of(ToaNhaApi.BQL, ToaNhaApi.BQT, ToaNhaApi.KE_TOAN, ToaNhaApi.KY_THUAT)) {
			this.mockMvc.perform(ToaNhaApi.chiTiet(this.auth.accessToken(so), "A"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tenToaNha").value("Tòa A"))
				.andExpect(jsonPath("$.data.soCanHo").value(12));
		}
		String cuDan = this.auth.accessToken(ToaNhaApi.CU_DAN);
		this.mockMvc.perform(ToaNhaApi.chiTiet(cuDan, "A")).andExpect(status().isForbidden());
		for (String so : List.of(ToaNhaApi.KE_TOAN, ToaNhaApi.BQT, ToaNhaApi.CU_DAN)) {
			this.mockMvc.perform(ToaNhaApi.dsTang(this.auth.accessToken(so), "A", ""))
				.andExpect(status().isForbidden());
		}
		for (String so : List.of(ToaNhaApi.KY_THUAT, ToaNhaApi.KE_TOAN)) {
			this.mockMvc.perform(ToaNhaApi.themTang(this.auth.accessToken(so), "A", 4))
				.andExpect(status().isForbidden());
		}
		assertThat(this.jdbc.queryForObject("SELECT so_tang FROM toa_nha WHERE ma_toa_nha = 'A'", Integer.class))
			.isEqualTo(3);
	}

}
