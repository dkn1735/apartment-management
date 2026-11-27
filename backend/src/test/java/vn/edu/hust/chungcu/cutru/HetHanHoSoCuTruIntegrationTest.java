package vn.edu.hust.chungcu.cutru;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.cutru.service.HetHanHoSoCuTruService;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/** Job chuyển hồ sơ quá ngày kết thúc sang Hết hiệu lực (spec 0014, AC-11, AC-12). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class HetHanHoSoCuTruIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	/** 08:00 ngày 2026-11-21 giờ Việt Nam. */
	private static final Instant SAU_HET_HAN = Instant.parse("2026-11-21T01:00:00Z");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MutableClock clock;

	@Autowired
	HetHanHoSoCuTruService hetHan;

	NhatKyDb nhatKy;

	String bql;

	String ma;

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.bql = new AuthApi(this.mockMvc).accessToken(CuTruApi.BQL);
		this.ma = CuTruApi.taoCanThu(this.mockMvc, this.bql);
	}

	@AfterEach
	void tearDown() {
		CuTruApi.donDep(this.jdbc);
		this.clock.reset();
	}

	@Test
	void hoSoQuaHanThanhHetHieuLucVaCanVeTrong() throws Exception {
		String a = CuTruApi.taoCuDanThu(this.jdbc, "Lý Hết Một", "Hoạt động");
		String b = CuTruApi.taoCuDanThu(this.jdbc, "Lý Hết Hai", "Hoạt động");
		String hsA = them(a, "Chủ căn hộ", "2026-11-01", "2026-11-20");
		String hsB = them(b, "Khách thuê", "2026-11-10", "2026-11-18");
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Đang sử dụng");
		String nvTruoc = nguoiXacMinh(hsA);

		this.clock.set(SAU_HET_HAN);
		assertThat(this.hetHan.chay()).isGreaterThanOrEqualTo(2);

		for (String hs : List.of(hsA, hsB)) {
			Map<String, Object> dong = this.jdbc.queryForMap(
					"SELECT trang_thai_ho_so, cap_nhat_luc, ma_nvbql_xac_minh FROM ho_so_cu_tru WHERE ma_ho_so = ?",
					hs);
			assertThat(dong.get("trang_thai_ho_so")).isEqualTo("Hết hiệu lực");
			assertThat(NhatKyDb.instant(dong.get("cap_nhat_luc"))).isEqualTo(SAU_HET_HAN);
			assertThat(dong.get("ma_nvbql_xac_minh")).isEqualTo(nvTruoc);
		}
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Trống");

		// Hai dòng cùng căn: trangThaiCanHo chỉ ở dòng của mã hồ sơ cuối cùng
		Map<String, Object> dongA = this.nhatKy.theoDoiTuong("HoSoCuTru:" + hsA).get(1);
		Map<String, Object> dongB = this.nhatKy.theoDoiTuong("HoSoCuTru:" + hsB).get(1);
		for (Map<String, Object> dong : List.of(dongA, dongB)) {
			assertThat(dong.get("loai_thao_tac")).isEqualTo("CUTRU_KET_THUC_HO_SO");
			assertThat(dong.get("ma_nguoi_dung")).isNull();
			assertThat(NhatKyDb.chiTiet(dong).get("tuDong").asBoolean()).isTrue();
		}
		JsonNode chiTietA = NhatKyDb.chiTiet(dongA);
		JsonNode chiTietB = NhatKyDb.chiTiet(dongB);
		assertThat(chiTietA.get("ngayKetThuc").asString()).isEqualTo("2026-11-20");
		assertThat(chiTietA.has("trangThaiCanHo")).isFalse();
		assertThat(chiTietB.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "ngayKetThuc", "tuDong",
				"trangThaiCanHo");
		assertThat(chiTietB.get("trangThaiCanHo").get("cu").asString()).isEqualTo("Đang sử dụng");

		// Chạy lại: không còn gì để đổi, không thêm nhật ký
		this.hetHan.chay();
		assertThat(this.nhatKy.theoDoiTuong("HoSoCuTru:" + hsA)).hasSize(2);
		assertThat(this.nhatKy.theoDoiTuong("HoSoCuTru:" + hsB)).hasSize(2);
	}

	// Hồ sơ hôm nay là ngày cuối vẫn đang cư trú, chưa hết hạn
	@Test
	void ngayCuoiChuaHetHan() throws Exception {
		String a = CuTruApi.taoCuDanThu(this.jdbc, "Lý Ngày Cuối", "Hoạt động");
		String hs = them(a, "Khách thuê", "2026-11-01", CuTruApi.HOM_NAY);
		this.hetHan.chay();
		assertThat(this.jdbc.queryForObject("SELECT trang_thai_ho_so FROM ho_so_cu_tru WHERE ma_ho_so = ?",
				String.class, hs))
			.isEqualTo("Hợp lệ");
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Đang sử dụng");
	}

	// covers: AC-10, AC-11, AC-12 (lúc 00:05 giờ Việt Nam căn còn người ở khác thì giữ
	// Đang sử dụng và nhật ký không có trangThaiCanHo)
	@Test
	void canConNguoiDangOThiGiuDangSuDungKhiMotHoSoHetHanDauNgayGioVietNam() throws Exception {
		String chu = CuTruApi.taoCuDanThu(this.jdbc, "Lý Chủ Ở Lại", "Hoạt động");
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Khách Đi", "Hoạt động");
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, chu, "Chủ căn hộ", "Thường trú", "2026-11-01", null)))
			.andExpect(status().isCreated());
		String hsKhach = them(khach, "Khách thuê", "2026-11-01", "2026-11-20");

		// 17:05 UTC ngày 2026-11-20 tức là 00:05 sáng 2026-11-21 giờ Việt Nam (giờ cron
		// mặc định)
		this.clock.set(Instant.parse("2026-11-20T17:05:00Z"));
		assertThat(this.hetHan.chay()).isGreaterThanOrEqualTo(1);

		assertThat(this.jdbc.queryForObject("SELECT trang_thai_ho_so FROM ho_so_cu_tru WHERE ma_ho_so = ?",
				String.class, hsKhach))
			.isEqualTo("Hết hiệu lực");
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Đang sử dụng");
		JsonNode chiTiet = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("HoSoCuTru:" + hsKhach).get(1));
		assertThat(chiTiet.get("tuDong").asBoolean()).isTrue();
		assertThat(chiTiet.has("trangThaiCanHo")).isFalse();
	}

	private String them(String maCuDan, String vaiTro, String batDau, String ketThuc) throws Exception {
		String body = this.mockMvc
			.perform(CuTruApi.them(this.bql, CuTruApi.hoSo(this.ma, maCuDan, vaiTro, "Tạm trú", batDau, ketThuc)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JSON.readTree(body).get("data").get("maHoSo").asString();
	}

	private String nguoiXacMinh(String maHoSo) {
		return this.jdbc.queryForObject("SELECT ma_nvbql_xac_minh FROM ho_so_cu_tru WHERE ma_ho_so = ?", String.class,
				maHoSo);
	}

}
