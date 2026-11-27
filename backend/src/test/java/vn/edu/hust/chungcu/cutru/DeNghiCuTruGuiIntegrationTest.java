package vn.edu.hust.chungcu.cutru;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Cư dân gửi đề nghị cư trú và đọc tab đề nghị trên căn hộ (spec 0015, AC-1 đến AC-3,
 * AC-8, AC-11 đến AC-13). Người gửi là cư dân seed {@code 0900000007} được cho ở căn thử;
 * cư dân seed {@code 0900000006} ở cùng căn để kiểm cách ly đề nghị.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class DeNghiCuTruGuiIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MutableClock clock;

	AuthApi auth;

	NhatKyDb nhatKy;

	String bql;

	String ma;

	String toi;

	String tokenToi;

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.bql = this.auth.accessToken(CuTruApi.BQL);
		this.ma = CuTruApi.taoCanThu(this.mockMvc, this.bql);
		CuTruApi.choO(this.jdbc, CuTruApi.CU_DAN_A0201, this.ma);
		CuTruApi.choO(this.jdbc, CuTruApi.BQT_CU_DAN_A0101, this.ma);
		this.toi = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		this.tokenToi = this.auth.accessToken(CuTruApi.CU_DAN_A0201);
	}

	@AfterEach
	void tearDown() {
		CuTruApi.donDep(this.jdbc);
		this.clock.reset();
	}

	// AC-1, AC-8, AC-13: đề nghị cho người khác, phản hồi ẩn PII, căn giữ trạng thái, chỉ
	// người liên quan thấy đề nghị
	@Test
	void guiChoNguoiKhacRoiDocTabDeNghi() throws Exception {
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Khách Đề Nghị", "Hoạt động");
		String cccd = CuTruApi.soCccdCua(this.jdbc, khach);
		String sdt = CuTruApi.soDienThoaiCua(this.jdbc, khach);
		String cccdGach = cccd.substring(0, 6) + "-" + cccd.substring(6);
		String sdtQuocTe = "+84 " + sdt.substring(1, 4) + "." + sdt.substring(4);
		MvcResult kq = this.mockMvc
			.perform(CuTruApi.gui(this.tokenToi,
					CuTruApi.deNghi(" " + this.ma.toLowerCase() + " ", cccdGach, sdtQuocTe, "Khách thuê", "Tạm trú",
							"2026-12-01", "2027-05-31", "  Con gái thuê trọ  ")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("Cư dân gửi yêu cầu cập nhật hồ sơ thành công"))
			.andExpect(jsonPath("$.data.maHoSo").value(matchesPattern("^HS[0-9]{8}$")))
			.andExpect(jsonPath("$.data.maCanHo").value(this.ma))
			.andExpect(jsonPath("$.data.maCuDan").value(khach))
			.andExpect(jsonPath("$.data.hoTen").value("Lý Khách Đề Nghị"))
			.andExpect(jsonPath("$.data.soCccd").value(nullValue()))
			.andExpect(jsonPath("$.data.soDienThoai").value(nullValue()))
			.andExpect(jsonPath("$.data.email").value(nullValue()))
			.andExpect(jsonPath("$.data.maNguoiDung").value(nullValue()))
			.andExpect(jsonPath("$.data.trangThaiHoSo").value("Chờ xác minh"))
			.andExpect(jsonPath("$.data.maNvbqlXacMinh").value(nullValue()))
			.andExpect(jsonPath("$.data.hoTenNguoiXacMinh").value(nullValue()))
			.andExpect(jsonPath("$.data.maCuDanDeNghi").value(this.toi))
			.andExpect(jsonPath("$.data.hoTenNguoiDeNghi").value(notNullValue()))
			.andExpect(jsonPath("$.data.ghiChuDeNghi").value("Con gái thuê trọ"))
			.andExpect(jsonPath("$.data.lyDoTuChoi").value(nullValue()))
			.andExpect(jsonPath("$.data.coTheHuy").value(true))
			.andExpect(jsonPath("$.data.taoLuc").value(MutableClock.MAC_DINH.toString()))
			.andReturn();
		assertThat(kq.getResponse().getContentAsString()).doesNotContain("@chungcu.test").doesNotContain(cccd);
		String maHoSo = JSON.readTree(kq.getResponse().getContentAsString()).get("data").get("maHoSo").asString();
		Map<String, Object> dong = this.jdbc.queryForMap(
				"SELECT ma_cu_dan_de_nghi, ma_nvbql_xac_minh, ghi_chu_de_nghi FROM ho_so_cu_tru WHERE ma_ho_so = ?",
				maHoSo);
		assertThat(dong.get("ma_cu_dan_de_nghi")).isEqualTo(this.toi);
		assertThat(dong.get("ma_nvbql_xac_minh")).isNull();
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Trống");

		List<Map<String, Object>> log = this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo);
		assertThat(log).extracting(d -> d.get("loai_thao_tac")).containsExactly("CUTRU_GUI_DE_NGHI");
		JsonNode chiTiet = NhatKyDb.chiTiet(log.get(0));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "maCuDanDeNghi",
				"vaiTroCuTru", "tinhTrangCuTru", "ngayBatDau", "ngayKetThuc", "coGhiChu");
		assertThat(chiTiet.get("coGhiChu").asBoolean()).isTrue();
		assertThat(chiTiet.get("maCuDanDeNghi").asString()).isEqualTo(this.toi);
		assertThat(log.get(0).get("chi_tiet").toString()).doesNotContain("Con gái")
			.doesNotContain(cccd)
			.doesNotContain("Lý Khách");

		this.mockMvc.perform(CuTruApi.xem(this.tokenToi, this.ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.deNghiCuTru[*].maHoSo").value(contains(maHoSo)))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].coTheHuy").value(true))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].soCccd").value(nullValue()))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].ghiChuDeNghi").value("Con gái thuê trọ"))
			.andExpect(jsonPath("$.data.dangCuTru[0].maCuDanDeNghi").value(nullValue()))
			.andExpect(jsonPath("$.data.dangCuTru[0].coTheHuy").value(false));
		this.mockMvc.perform(CuTruApi.xem(this.auth.accessToken(CuTruApi.BQT_CU_DAN_A0101), this.ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.deNghiCuTru").isEmpty())
			.andExpect(jsonPath("$.data.dangCuTru", hasSize(2)));
		this.mockMvc.perform(CuTruApi.xem(this.bql, this.ma))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].soCccd").value(cccd))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].coTheHuy").value(false))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].hoTenNguoiDeNghi").value(notNullValue()));
	}

	// AC-1, AC-3 bước 3 và 6: tự đề nghị cho mình, chồng ngày với hồ sơ hợp lệ thì 409
	@Test
	void deNghiChoChinhMinh() throws Exception {
		Map<String, Object> body = CuTruApi.deNghi(this.ma, null, null, "Thành viên gia đình", "Thường trú",
				CuTruApi.HOM_NAY, null, "   ");
		this.mockMvc.perform(CuTruApi.gui(this.tokenToi, body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN"));
		this.jdbc.update(
				"UPDATE ho_so_cu_tru SET ngay_ket_thuc = DATE '2026-12-31' WHERE ma_can_ho = ? " + "AND ma_cu_dan = ?",
				this.ma, this.toi);
		body.put("ngayBatDau", "2027-01-01");
		MvcResult kq = this.mockMvc.perform(CuTruApi.gui(this.tokenToi, body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.maCuDan").value(this.toi))
			.andExpect(jsonPath("$.data.maCuDanDeNghi").value(this.toi))
			.andExpect(jsonPath("$.data.ghiChuDeNghi").value(nullValue()))
			.andReturn();
		String maHoSo = JSON.readTree(kq.getResponse().getContentAsString()).get("data").get("maHoSo").asString();
		JsonNode chiTiet = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo).get(0));
		assertThat(chiTiet.get("coGhiChu").asBoolean()).isFalse();
		assertThat(chiTiet.get("ngayKetThuc").isNull()).isTrue();
	}

	// AC-3 bước 3: mọi lý do không khớp đều chung một mã 422
	@Test
	void khongKhopDinhDanhThiChungMotMa() throws Exception {
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Khớp", "Hoạt động");
		String ngung = CuTruApi.taoCuDanThu(this.jdbc, "Lý Ngừng", "Ngừng hoạt động");
		String cccd = CuTruApi.soCccdCua(this.jdbc, khach);
		List<String[]> cap = List.of(new String[] { cccd, "0911111111" },
				new String[] { "999999999999", CuTruApi.soDienThoaiCua(this.jdbc, khach) },
				new String[] { CuTruApi.soCccdCua(this.jdbc, ngung), CuTruApi.soDienThoaiCua(this.jdbc, ngung) },
				new String[] { "001026000002", CuTruApi.BQL });
		for (String[] c : cap) {
			this.mockMvc
				.perform(CuTruApi.gui(this.tokenToi,
						CuTruApi.deNghi(this.ma, c[0], c[1], "Khách thuê", "Thường trú", "2026-12-01", null, null)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errorCode").value("CUTRU_KHONG_KHOP_CU_DAN"))
				.andExpect(jsonPath("$.message")
					.value("Không tìm thấy tài khoản cư dân khớp đồng thời số CCCD và số điện thoại đã nhập"));
		}
	}

	// AC-3 bước 4 và 5: một đề nghị chờ cho mỗi cặp, tối đa 10 đề nghị chờ mỗi căn
	@Test
	void deNghiTrungVaTranMuoi() throws Exception {
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Trùng", "Tạm khóa");
		Map<String, Object> body = CuTruApi.deNghi(this.ma, CuTruApi.soCccdCua(this.jdbc, khach),
				CuTruApi.soDienThoaiCua(this.jdbc, khach), "Khách thuê", "Thường trú", "2026-12-01", null, null);
		this.mockMvc.perform(CuTruApi.gui(this.tokenToi, body)).andExpect(status().isCreated());
		this.mockMvc.perform(CuTruApi.gui(this.tokenToi, body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH"))
			.andExpect(jsonPath("$.message").value("Cư dân này đã có đề nghị cư trú đang chờ xác minh tại căn hộ"));
		for (int i = 0; i < 9; i++) {
			String khac = CuTruApi.taoCuDanThu(this.jdbc, "Lý Chờ " + i, "Hoạt động");
			this.jdbc.update("""
					INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, vai_tro_cu_tru, tinh_trang_cu_tru, ngay_bat_dau,
					    trang_thai_ho_so, ma_cu_dan_de_nghi)
					VALUES (?, ?, 'Khách thuê', 'Thường trú', DATE '2026-12-01', 'Chờ xác minh', ?)""", this.ma, khac,
					this.toi);
		}
		String thuMuoiMot = CuTruApi.taoCuDanThu(this.jdbc, "Lý Mười Một", "Hoạt động");
		this.mockMvc
			.perform(CuTruApi.gui(this.tokenToi,
					CuTruApi.deNghi(this.ma, CuTruApi.soCccdCua(this.jdbc, thuMuoiMot),
							CuTruApi.soDienThoaiCua(this.jdbc, thuMuoiMot), "Khách thuê", "Thường trú", "2026-12-01",
							null, null)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH"));
	}

	// AC-2: mỗi trường sai một mục theo thứ tự; CCCD và SĐT chỉ kiểm khi laChinhToi =
	// false
	@Test
	void duLieuSaiTheoThuTuTruong() throws Exception {
		this.mockMvc.perform(CuTruApi.guiRaw(this.tokenToi, """
				{"maCanHo": 5, "laChinhToi": "false", "soCccd": 1, "vaiTroCuTru": "Chủ căn hộ",
				 "tinhTrangCuTru": "X", "ngayBatDau": "2026-11-15", "ngayKetThuc": "2026-11-14", "ghiChuDeNghi": 3}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("maCanHo", "laChinhToi", "vaiTroCuTru",
					"tinhTrangCuTru", "ngayBatDau", "ngayKetThuc", "ghiChuDeNghi")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Căn hộ không hợp lệ", "Vui lòng chọn đối tượng cư trú",
						"Cư dân chỉ được đề nghị vai trò Thành viên gia đình hoặc Khách thuê",
						"Tình trạng cư trú không hợp lệ", "Ngày bắt đầu của đề nghị phải từ hôm nay trở đi",
						"Ngày kết thúc phải từ ngày bắt đầu trở đi", "Ghi chú đề nghị không hợp lệ")));
		this.mockMvc.perform(CuTruApi.guiRaw(this.tokenToi, """
				{"maCanHo": "  ", "laChinhToi": false, "soCccd": " - ", "ngayKetThuc": ""}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].message").value(contains("Vui lòng chọn căn hộ", "Vui lòng nhập số CCCD",
					"Vui lòng nhập số điện thoại", "Vui lòng chọn vai trò cư trú", "Vui lòng chọn tình trạng cư trú",
					"Vui lòng chọn ngày bắt đầu", "Ngày kết thúc không hợp lệ")));
		Map<String, Object> body = CuTruApi.deNghi(this.ma, "12345", "84", "Khách thuê", "Tạm trú", "2026-12-01", null,
				"ạ".repeat(501));
		this.mockMvc.perform(CuTruApi.gui(this.tokenToi, body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field")
				.value(contains("soCccd", "soDienThoai", "ngayKetThuc", "ghiChuDeNghi")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Số CCCD phải gồm đúng 12 chữ số", "Số điện thoại không hợp lệ",
						"Tạm trú bắt buộc có ngày kết thúc", "Ghi chú đề nghị tối đa 500 ký tự")));
	}

	// AC-3 bước 1, AC-12: căn không ở thì 404, vai trò khác 403, chưa đăng nhập 401
	@Test
	void phamViVaPhanQuyen() throws Exception {
		Map<String, Object> body = CuTruApi.deNghi("A-0101", null, null, "Khách thuê", "Thường trú", "2026-12-01", null,
				null);
		this.mockMvc.perform(CuTruApi.gui(this.tokenToi, body))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
		body.put("maCanHo", "Z-9999");
		this.mockMvc.perform(CuTruApi.gui(this.tokenToi, body)).andExpect(status().isNotFound());
		this.mockMvc.perform(CuTruApi.gui(this.bql, body)).andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.gui(this.auth.accessToken(CuTruApi.KE_TOAN), body))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.gui("khong-hop-le", body)).andExpect(status().isUnauthorized());
	}

}
