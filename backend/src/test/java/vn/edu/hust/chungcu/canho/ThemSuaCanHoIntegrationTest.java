package vn.edu.hust.chungcu.canho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Tạo và sửa căn hộ trên tòa test (spec 0012, AC-7 đến AC-14). Tòa test được
 * {@link ToaNhaApi#donDep} xóa cùng căn sau mỗi test.
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class ThemSuaCanHoIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	AuthApi auth;

	NhatKyDb nhatKy;

	String bql;

	String toa;

	String tang1;

	@BeforeEach
	void setUp() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.bql = this.auth.accessToken(ToaNhaApi.BQL);
		this.toa = ToaNhaApi.maMoi();
		this.mockMvc.perform(ToaNhaApi.themToa(this.bql, ToaNhaApi.toaMoi(this.toa, 3)))
			.andExpect(status().isCreated());
		this.tang1 = this.toa + "-01";
	}

	@AfterEach
	void tearDown() {
		ToaNhaApi.donDep(this.jdbc);
	}

	// AC-5, AC-7, AC-11, AC-14: đường chính tạo, sửa, xem lại, hai dòng nhật ký
	@Test
	void taoSuaRoiXemLai() throws Exception {
		String ma = this.tang1 + "05";
		this.mockMvc.perform(CanHoApi.them(this.bql, CanHoApi.body(this.tang1.toLowerCase(), 5, 70.5)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("Tạo mới căn hộ thành công"))
			.andExpect(jsonPath("$.data.maCanHo").value(ma))
			.andExpect(jsonPath("$.data.maTang").value(this.tang1))
			.andExpect(jsonPath("$.data.soTang").value(1))
			.andExpect(jsonPath("$.data.maToaNha").value(this.toa))
			.andExpect(jsonPath("$.data.dienTich").value(70.5))
			.andExpect(jsonPath("$.data.trangThaiSuDung").value("Trống"))
			.andExpect(jsonPath("$.data.biTamDungTienIch").value(false));
		this.mockMvc.perform(CanHoApi.sua(this.bql, ma.toLowerCase(), 72, "Đang sử dụng"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Cập nhật thông tin căn hộ thành công"))
			.andExpect(jsonPath("$.data.dienTich").value(72.0))
			.andExpect(jsonPath("$.data.trangThaiSuDung").value("Đang sử dụng"));
		this.mockMvc.perform(CanHoApi.chiTiet(this.bql, ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.dienTich").value(72.0))
			.andExpect(jsonPath("$.data.maTang").value(this.tang1));
		this.mockMvc.perform(ToaNhaApi.chiTiet(this.bql, this.toa)).andExpect(jsonPath("$.data.soCanHo").value(1));

		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong("CanHo:" + ma);
		assertThat(dong).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("CANHO_THEM_CAN_HO", "CANHO_SUA_CAN_HO");
		assertThat(dong.get(0).get("chi_tiet").toString()).contains("\"dienTich\": 70.50")
			.contains("\"maTang\": \"" + this.tang1 + "\"")
			.contains("\"trangThaiSuDung\": \"Trống\"");
		assertThat(dong.get(1).get("chi_tiet").toString()).contains("\"cu\": 70.50").contains("\"moi\": 72.00");
		JsonNode sua = NhatKyDb.chiTiet(dong.get(1));
		assertThat(sua.get("trangThaiSuDung").get("cu").asString()).isEqualTo("Trống");
		assertThat(sua.get("trangThaiSuDung").get("moi").asString()).isEqualTo("Đang sử dụng");
	}

	// AC-9: trùng mã thì 409 ở ô số căn; lỗi 400 thắng 409
	@Test
	void trungMaThi409() throws Exception {
		this.mockMvc.perform(CanHoApi.them(this.bql, CanHoApi.body(this.tang1, 5, 50))).andExpect(status().isCreated());
		this.mockMvc.perform(CanHoApi.them(this.bql, CanHoApi.body(this.tang1, 5, 60)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CANHO_CAN_HO_DA_TON_TAI"))
			.andExpect(jsonPath("$.message").value("Căn hộ đã tồn tại"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("soCan")))
			.andExpect(jsonPath("$.errors[0].message").value("Tầng này đã có căn số 5 (" + this.tang1 + "05)"));
		this.mockMvc.perform(CanHoApi.them(this.bql, CanHoApi.body(this.tang1, 5, 0)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("dienTich")));
		assertThat(this.nhatKy.theoDoiTuong("CanHo:" + this.tang1 + "05")).hasSize(1);
	}

	// AC-8: một mục cho mỗi trường sai, theo thứ tự cố định
	@Test
	void duLieuTaoSaiThi400() throws Exception {
		this.mockMvc.perform(CanHoApi.themRaw(this.bql, """
				{"maTang": null, "soCan": 0, "dienTich": 0, "trangThaiSuDung": ""}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("maTang", "soCan", "dienTich", "trangThaiSuDung")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Vui lòng chọn tầng", "Số căn phải là số nguyên từ 1 đến 99",
						"Diện tích phải lớn hơn 0, tối đa 1000 m² và tối đa 2 chữ số thập phân",
						"Trạng thái sử dụng không hợp lệ")));
		for (String dienTich : List.of("1000.01", "75.505", "\"75\"", "null", "true", "-5")) {
			this.mockMvc.perform(CanHoApi.themRaw(this.bql, """
					{"maTang": "%s", "soCan": 1, "dienTich": %s}""".formatted(this.tang1, dienTich)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("dienTich")));
		}
		for (String soCan : List.of("100", "1.0", "\"5\"", "null")) {
			this.mockMvc.perform(CanHoApi.themRaw(this.bql, """
					{"maTang": "%s", "soCan": %s, "dienTich": 50}""".formatted(this.tang1, soCan)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("soCan")));
		}
		for (String maTang : List.of("5", "\"ZZ-01\"", "\"" + this.toa + "-04\"", "[\"A-01\"]")) {
			this.mockMvc.perform(CanHoApi.themRaw(this.bql, """
					{"maTang": %s, "soCan": 1, "dienTich": 50}""".formatted(maTang)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].message").value(contains("Tầng không tồn tại")));
		}
		this.mockMvc.perform(CanHoApi.themRaw(this.bql, """
				{"maTang": "%s", "soCan": 1, "dienTich": 50, "trangThaiSuDung": 5}""".formatted(this.tang1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("trangThaiSuDung")));
		this.mockMvc.perform(CanHoApi.themRaw(this.bql, "5"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
		this.mockMvc
			.perform(CanHoApi.themRaw(this.bql,
					"""
							{"maTang": "%s", "soCan": 99, "dienTich": 1000, "trangThaiSuDung": "Đã bàn giao"}"""
						.formatted(this.tang1)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.maCanHo").value(this.tang1 + "99"))
			.andExpect(jsonPath("$.data.trangThaiSuDung").value("Đã bàn giao"));
	}

	// AC-9: hai request tạo cùng căn chạy song song, đúng một 201 và một 409
	@Test
	void haiRequestTaoSongSong() throws Exception {
		List<Integer> trangThai = songSong(() -> CanHoApi.them(this.bql, CanHoApi.body(this.tang1, 7, 50)),
				() -> CanHoApi.them(this.bql, CanHoApi.body(this.tang1, 7, 55)));
		assertThat(trangThai).containsExactlyInAnyOrder(201, 409);
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM can_ho WHERE ma_can_ho = ?", Long.class,
				this.tang1 + "07"))
			.isEqualTo(1);
	}

	// AC-10: CHECK của V16 chặn cả khi chèn tay
	@Test
	void checkChanDuLieuSaiKhiChenTay() {
		for (String[] dong : List.of(new String[] { "A-0100", "A-01", "50" }, new String[] { "A-02-01", "A-01", "50" },
				new String[] { "B-0101", "A-01", "50" }, new String[] { "A-0105", "A-02", "50" },
				new String[] { "A-0199", "A-01", "1500" })) {
			assertThatThrownBy(() -> this.jdbc.update(
					"INSERT INTO can_ho (ma_can_ho, ma_tang, dien_tich) VALUES (?, ?, CAST(? AS numeric))", dong[0],
					dong[1], dong[2]))
				.as(String.join(",", dong))
				.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	// AC-12: thứ tự mục lỗi; 400 trước 404
	@Test
	void duLieuSuaSaiThi400() throws Exception {
		String ma = taoCan(3, 50);
		this.mockMvc.perform(CanHoApi.suaRaw(this.bql, ma, "{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("dienTich", "trangThaiSuDung")))
			.andExpect(jsonPath("$.errors[1].message").value("Vui lòng chọn trạng thái sử dụng"));
		this.mockMvc.perform(CanHoApi.sua(this.bql, ma, 50, "Sửa chữa"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].message").value(contains("Trạng thái sử dụng không hợp lệ")));
		this.mockMvc.perform(CanHoApi.sua(this.bql, "KHONGCO", 0, "Trống")).andExpect(status().isBadRequest());
		for (String sai : List.of("KHONGCO", "ZZ-0101")) {
			this.mockMvc.perform(CanHoApi.sua(this.bql, sai, 50, "Trống"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		}
	}

	// AC-14: không đổi gì thì không ghi nhật ký; chỉ ghi trường đổi
	@Test
	void suaKhongDoiThiKhongGhiNhatKy() throws Exception {
		String ma = taoCan(4, 75.50);
		this.mockMvc.perform(CanHoApi.sua(this.bql, ma, 75.5, "Trống")).andExpect(status().isOk());
		assertThat(this.nhatKy.theoDoiTuong("CanHo:" + ma)).hasSize(1);
		this.mockMvc.perform(CanHoApi.sua(this.bql, ma, 75.500, "Đang sửa chữa")).andExpect(status().isOk());
		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong("CanHo:" + ma);
		assertThat(dong).hasSize(2);
		JsonNode chiTiet = NhatKyDb.chiTiet(dong.get(1));
		assertThat(chiTiet.propertyNames()).containsExactly("trangThaiSuDung");
	}

	// AC-13: hai lần sửa song song đều 200, nhật ký nối nhau
	@Test
	void haiLanSuaSongSong() throws Exception {
		String ma = taoCan(6, 60);
		List<Integer> trangThai = songSong(() -> CanHoApi.sua(this.bql, ma, 80, "Trống"),
				() -> CanHoApi.sua(this.bql, ma, 90, "Đang sửa chữa"));
		assertThat(trangThai).containsExactly(200, 200);
		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong("CanHo:" + ma);
		assertThat(dong).hasSize(3);
		JsonNode truoc = NhatKyDb.chiTiet(dong.get(1));
		JsonNode sau = NhatKyDb.chiTiet(dong.get(2));
		assertThat(truoc.get("dienTich").get("cu").decimalValue()).isEqualByComparingTo("60");
		assertThat(sau.get("dienTich").get("cu").decimalValue())
			.isEqualByComparingTo(truoc.get("dienTich").get("moi").decimalValue());
	}

	private String taoCan(int soCan, double dienTich) throws Exception {
		this.mockMvc.perform(CanHoApi.them(this.bql, CanHoApi.body(this.tang1, soCan, dienTich)))
			.andExpect(status().isCreated());
		return this.tang1 + String.format("%02d", soCan);
	}

	@SafeVarargs
	private List<Integer> songSong(Callable<MockHttpServletRequestBuilder>... request) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(request.length);
		CountDownLatch batDau = new CountDownLatch(1);
		try {
			List<Future<Integer>> ketQua = new ArrayList<>();
			for (Callable<MockHttpServletRequestBuilder> r : request) {
				MockHttpServletRequestBuilder builder = r.call();
				ketQua.add(pool.submit(() -> {
					batDau.await();
					return this.mockMvc.perform(builder).andReturn().getResponse().getStatus();
				}));
			}
			batDau.countDown();
			List<Integer> trangThai = new ArrayList<>();
			for (Future<Integer> f : ketQua) {
				trangThai.add(f.get(30, TimeUnit.SECONDS));
			}
			return trangThai;
		}
		finally {
			pool.shutdownNow();
		}
	}

}
