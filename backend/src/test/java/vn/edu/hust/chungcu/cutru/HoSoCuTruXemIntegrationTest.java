package vn.edu.hust.chungcu.cutru;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

/**
 * Xem nhân khẩu căn hộ, lọc dữ liệu cá nhân và tìm cư dân (spec 0014, AC-1 đến AC-3,
 * AC-13).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class HoSoCuTruXemIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String CHEN = """
			INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
			    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so)
			VALUES (?, ?, (SELECT min(ma_nv) FROM nhan_vien_bql), ?, ?, CAST(? AS date), CAST(? AS date), ?)
			RETURNING ma_ho_so""";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MutableClock clock;

	AuthApi auth;

	String bql;

	String ma;

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.bql = this.auth.accessToken(CuTruApi.BQL);
		this.ma = CuTruApi.taoCanThu(this.mockMvc, this.bql);
	}

	@AfterEach
	void tearDown() {
		CuTruApi.donDep(this.jdbc);
		this.clock.reset();
	}

	// AC-1, AC-2: BQL thấy đủ ba nhóm và mọi trường; cư dân thấy người cùng ở, chỉ lịch
	// sử
	// của mình, không có dữ liệu cá nhân
	@Test
	void bqlThayDuCuDanChiThayPhanCuaMinh() throws Exception {
		String toi = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		String thanhVien = CuTruApi.taoCuDanThu(this.jdbc, "Lý Thành Viên", "Hoạt động");
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Khách", "Hoạt động");
		String dangKhach = chen(khach, "Khách thuê", "Tạm trú", "2026-10-01", "2026-12-31", "Hợp lệ");
		String dangThanhVien = chen(thanhVien, "Thành viên gia đình", "Thường trú", "2026-05-01", null, "Hợp lệ");
		String dangToi = chen(toi, "Chủ căn hộ", "Thường trú", "2026-06-01", null, "Hợp lệ");
		String sapKhach = chen(khach, "Khách thuê", "Tạm trú", "2027-02-01", "2027-03-01", "Hợp lệ");
		String cuToi = chen(toi, "Khách thuê", "Tạm trú", "2025-01-01", "2025-06-30", "Hết hiệu lực");
		String cuKhach = chen(khach, "Khách thuê", "Tạm trú", "2025-07-01", "2025-12-31", "Hết hiệu lực");
		String huySomToi = chen(toi, "Khách thuê", "Tạm trú", "2027-06-01", "2027-06-01", "Hết hiệu lực");
		String quaHanKhach = chen(khach, "Khách thuê", "Tạm trú", "2026-01-01", "2026-09-30", "Hợp lệ");
		this.jdbc.update("""
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, vai_tro_cu_tru, tinh_trang_cu_tru, ngay_bat_dau,
				    ngay_ket_thuc, trang_thai_ho_so, ma_cu_dan_de_nghi)
				VALUES (?, ?, 'Khách thuê', 'Tạm trú', DATE '2028-01-01', DATE '2028-02-01', 'Chờ xác minh', ?)""",
				this.ma, khach, khach);

		JsonNode bql = data(this.bql);
		assertThat(ma(bql.get("dangCuTru"))).containsExactly(dangToi, dangThanhVien, dangKhach);
		assertThat(ma(bql.get("sapBatDau"))).containsExactly(sapKhach);
		assertThat(ma(bql.get("lichSuCuTru"))).containsExactly(quaHanKhach, cuKhach, cuToi, huySomToi);
		JsonNode dongToi = bql.get("dangCuTru").get(0);
		assertThat(dongToi.get("soCccd").asString()).isEqualTo("001026000007");
		assertThat(dongToi.get("soDienThoai").asString()).isEqualTo(CuTruApi.CU_DAN_A0201);
		assertThat(dongToi.get("email").asString()).isEqualTo("cudan@chungcu.test");
		assertThat(dongToi.get("maNguoiDung").asString()).startsWith("ND");
		assertThat(dongToi.get("hoTenNguoiXacMinh").isNull()).isFalse();

		this.mockMvc.perform(CuTruApi.xem(this.auth.accessToken(CuTruApi.CU_DAN_A0201), this.ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Tra cứu hồ sơ cư trú của căn hộ thành công"))
			.andExpect(jsonPath("$.data.dangCuTru[*].maHoSo").value(contains(dangToi, dangThanhVien, dangKhach)))
			.andExpect(jsonPath("$.data.dangCuTru[1].hoTen").value("Lý Thành Viên"))
			.andExpect(jsonPath("$.data.sapBatDau").isEmpty())
			.andExpect(jsonPath("$.data.lichSuCuTru[*].maHoSo").value(contains(cuToi, huySomToi)))
			.andExpect(jsonPath("$.data..soCccd").value(everyItem(nullValue())))
			.andExpect(jsonPath("$.data..soDienThoai").value(everyItem(nullValue())))
			.andExpect(jsonPath("$.data..email").value(everyItem(nullValue())))
			.andExpect(jsonPath("$.data..maNguoiDung").value(everyItem(nullValue())))
			.andExpect(jsonPath("$.data..maNvbqlXacMinh").value(everyItem(nullValue())))
			.andExpect(jsonPath("$.data..hoTenNguoiXacMinh").value(everyItem(nullValue())))
			.andExpect(jsonPath("$.data..maHoSo").value(hasSize(5)));
	}

	// AC-1, AC-14: căn seed A-0101, cư dân 0900000006 không thấy lịch sử của 0900000009
	@Test
	void cuDanSeedKhongThayLichSuNguoiKhac() throws Exception {
		String token = this.auth.accessToken(CuTruApi.BQT_CU_DAN_A0101);
		this.mockMvc.perform(CuTruApi.xem(token, "a-0101"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.dangCuTru[*].hoTen").value(contains("Đặng Ban Quản Trị")))
			.andExpect(jsonPath("$.data.lichSuCuTru").isEmpty());
		this.mockMvc.perform(CuTruApi.xem(this.bql, "A-0101"))
			.andExpect(jsonPath("$.data.lichSuCuTru[*].hoTen").value(contains("Bùi Chủ Hộ")));
		// Có CU_DAN nhưng không ở A-0201, và không phải BQL: như không tồn tại
		this.mockMvc.perform(CuTruApi.xem(token, "A-0201"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
	}

	// AC-2, AC-13: vai trò không được xem, căn không có, mã sai
	@Test
	void ngoaiPhamViThi404VaiTroKhacThi403() throws Exception {
		this.mockMvc.perform(CuTruApi.xem(this.auth.accessToken(CuTruApi.KE_TOAN), "A-0101"))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.xem(this.bql, "KHONG-CO")).andExpect(status().isNotFound());
		this.mockMvc.perform(CuTruApi.xem(this.auth.accessToken(CuTruApi.CU_DAN_A0201), this.ma))
			.andExpect(status().isNotFound());
		this.mockMvc.perform(CuTruApi.xem(this.bql, this.ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.dangCuTru").isEmpty())
			.andExpect(jsonPath("$.data.sapBatDau").isEmpty())
			.andExpect(jsonPath("$.data.lichSuCuTru").isEmpty());
	}

	// AC-3: tìm theo họ tên không dấu, số điện thoại, CCCD, email, mã; bỏ tài khoản ngừng
	@Test
	void timCuDanTheoNhieuCach() throws Exception {
		String ngung = CuTruApi.taoCuDanThu(this.jdbc, "Vũ Ngừng Hoạt", "Ngừng hoạt động");
		String maVu = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		this.mockMvc.perform(CuTruApi.timCuDan(this.bql, null))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Tra cứu danh sách cư dân thành công"))
			.andExpect(jsonPath("$.data.length()").value(lessThanOrEqualTo(20)))
			.andExpect(jsonPath("$.data[*].maCuDan").value(not(hasItem(ngung))))
			.andExpect(jsonPath("$.data[*].trangThaiTaiKhoan").value(hasItem("Tạm khóa")));
		for (String q : List.of(" vu cu dan ", "+84 900 000 007", "84900000007", "090.000.0007", "001026-000007",
				"CUDAN@CHUNGCU", maVu.toLowerCase())) {
			this.mockMvc.perform(CuTruApi.timCuDan(this.bql, q))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].maCuDan").value(hasItem(maVu)))
				.andExpect(jsonPath("$.data[*].maCuDan").value(not(hasItem(ngung))));
		}
		this.mockMvc.perform(CuTruApi.timCuDan(this.bql, "%")).andExpect(jsonPath("$.data").isEmpty());
		this.mockMvc.perform(CuTruApi.timCuDan(this.bql, "a".repeat(101)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("q"))
			.andExpect(jsonPath("$.errors[0].message").value("Từ khóa tối đa 100 ký tự"));
		this.mockMvc.perform(CuTruApi.timCuDan(this.bql, " " + "a".repeat(100) + " ")).andExpect(status().isOk());
		this.mockMvc.perform(CuTruApi.timCuDan(this.auth.accessToken(CuTruApi.CU_DAN_A0201), null))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.timCuDan(this.auth.accessToken(CuTruApi.KE_TOAN), null))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.timCuDan("khong-hop-le", null)).andExpect(status().isUnauthorized());
		this.mockMvc.perform(CuTruApi.xem("khong-hop-le", "A-0101")).andExpect(status().isUnauthorized());
	}

	// covers: AC-1, AC-2 (Value sourcing homNay lúc 00:30 giờ Việt Nam và lọc sapBatDau
	// của riêng cư dân)
	@Test
	void homNayTheoGioVietNamQuaNuaDemUtcVaLocSapBatDauCuaMinh() throws Exception {
		// 17:30 UTC ngày 2026-11-15 tức là 00:30 ngày 2026-11-16 giờ Asia/Ho_Chi_Minh
		this.clock.set(java.time.Instant.parse("2026-11-15T17:30:00Z"));
		String toi = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Qua Ngày", "Hoạt động");

		String batDauHomNay = chen(toi, "Chủ căn hộ", "Tạm trú", "2026-11-16", "2026-11-20", "Hợp lệ");
		String sapCuaToi = chen(toi, "Chủ căn hộ", "Tạm trú", "2026-11-25", "2026-12-01", "Hợp lệ");
		String sapNguoiKhac = chen(khach, "Khách thuê", "Tạm trú", "2026-11-25", "2026-12-01", "Hợp lệ");
		String hetHomQua = chen(khach, "Khách thuê", "Tạm trú", "2026-11-01", "2026-11-15", "Hợp lệ");

		JsonNode bqlData = data(this.bql);
		assertThat(ma(bqlData.get("dangCuTru"))).containsExactly(batDauHomNay);
		assertThat(ma(bqlData.get("sapBatDau"))).containsExactly(sapCuaToi, sapNguoiKhac);
		assertThat(ma(bqlData.get("lichSuCuTru"))).containsExactly(hetHomQua);

		String tokenCuDan = this.auth.accessToken(CuTruApi.CU_DAN_A0201);
		this.mockMvc.perform(CuTruApi.xem(tokenCuDan, this.ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.dangCuTru[*].maHoSo").value(contains(batDauHomNay)))
			.andExpect(jsonPath("$.data.sapBatDau[*].maHoSo").value(contains(sapCuaToi)))
			.andExpect(jsonPath("$.data.lichSuCuTru").isEmpty())
			.andExpect(jsonPath("$.data.sapBatDau[0].soCccd").value(nullValue()));
	}

	private String chen(String maCuDan, String vaiTro, String tinhTrang, String batDau, String ketThuc,
			String trangThai) {
		return this.jdbc.queryForObject(CHEN, String.class, this.ma, maCuDan, vaiTro, tinhTrang, batDau, ketThuc,
				trangThai);
	}

	private JsonNode data(String token) throws Exception {
		String body = this.mockMvc.perform(CuTruApi.xem(token, this.ma))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JSON.readTree(body).get("data");
	}

	private static List<String> ma(JsonNode mang) {
		return mang.valueStream().map(n -> n.get("maHoSo").asString()).toList();
	}

}
