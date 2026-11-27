package vn.edu.hust.chungcu.cutru;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.LinkedHashMap;
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
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Sửa, kết thúc hồ sơ cư trú và chặn sửa tay trạng thái căn mâu thuẫn (spec 0014, AC-7,
 * AC-8, AC-10, AC-12, AC-13).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class HoSoCuTruCapNhatIntegrationTest {

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

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.bql = this.auth.accessToken(CuTruApi.BQL);
		this.ma = CuTruApi.taoCanThu(this.mockMvc, this.bql);
	}

	@AfterEach
	void tearDown() {
		CuTruApi.donDep(this.jdbc);
		this.clock.reset();
	}

	// AC-7, AC-10, AC-12, AC-13: kết thúc cư trú hôm nay, căn về Trống, cư dân mất quyền
	// xem
	@Test
	void ketThucCuTruThiCanVeTrongVaCuDanMatQuyenXem() throws Exception {
		String cuDan = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		String maHoSo = them(cuDan, "Chủ căn hộ", "Thường trú", CuTruApi.HOM_NAY, null);
		String tokenCuDan = this.auth.accessToken(CuTruApi.CU_DAN_A0201);
		this.mockMvc.perform(CuTruApi.chiTietCanHo(tokenCuDan, this.ma)).andExpect(status().isOk());

		this.clock.advance(Duration.ofMinutes(5));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, " " + maHoSo.toLowerCase() + " ",
					CuTruApi.capNhat("Chủ căn hộ", "Thường trú", CuTruApi.HOM_NAY, CuTruApi.HOM_NAY, "Hết hiệu lực")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Cập nhật hồ sơ cư trú thành công"))
			.andExpect(jsonPath("$.data.trangThaiHoSo").value("Hết hiệu lực"))
			.andExpect(jsonPath("$.data.ngayKetThuc").value(CuTruApi.HOM_NAY))
			.andExpect(jsonPath("$.data.capNhatLuc").value(MutableClock.MAC_DINH.plusSeconds(300).toString()))
			.andExpect(jsonPath("$.data.taoLuc").value(MutableClock.MAC_DINH.toString()));
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Trống");
		this.mockMvc.perform(CuTruApi.chiTietCanHo(tokenCuDan, this.ma)).andExpect(status().isNotFound());
		this.mockMvc.perform(CuTruApi.xem(tokenCuDan, this.ma)).andExpect(status().isNotFound());

		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo);
		assertThat(dong).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("CUTRU_THEM_HO_SO", "CUTRU_KET_THUC_HO_SO");
		JsonNode ketThuc = NhatKyDb.chiTiet(dong.get(1));
		assertThat(ketThuc.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "ngayKetThuc", "tuDong",
				"trangThaiCanHo");
		assertThat(ketThuc.get("ngayKetThuc").asString()).isEqualTo(CuTruApi.HOM_NAY);
		assertThat(ketThuc.get("tuDong").asBoolean()).isFalse();
		assertThat(ketThuc.get("trangThaiCanHo").get("moi").asString()).isEqualTo("Trống");

		// AC-8: hồ sơ đã hết hiệu lực không sửa được
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, maHoSo,
					CuTruApi.capNhat("Chủ căn hộ", "Thường trú", CuTruApi.HOM_NAY, null, "Hợp lệ")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_HO_SO_DA_HET_HIEU_LUC"))
			.andExpect(jsonPath("$.message").value("Hồ sơ cư trú đã hết hiệu lực, không thể chỉnh sửa"));
	}

	// AC-7, AC-12: gửi lại y nguyên thì không ghi gì; đổi một phần thì nhật ký chỉ có
	// phần đổi
	@Test
	void suaKhongDoiThiKhongGhiDoiMotPhanThiGhiPhanDoi() throws Exception {
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Sửa", "Hoạt động");
		String maHoSo = them(cuDan, "Khách thuê", "Thường trú", "2026-11-01", null);
		this.clock.advance(Duration.ofMinutes(5));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, maHoSo,
					CuTruApi.capNhat("Khách thuê", " Thường trú ", "2026-11-01", null, "Hợp lệ")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.capNhatLuc").value(MutableClock.MAC_DINH.toString()));
		assertThat(this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo)).hasSize(1);

		this.mockMvc
			.perform(CuTruApi.sua(this.bql, maHoSo,
					CuTruApi.capNhat("Khách thuê", "Tạm trú", "2026-11-01", "2027-06-30", "Hợp lệ")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tinhTrangCuTru").value("Tạm trú"))
			.andExpect(jsonPath("$.data.capNhatLuc").value(MutableClock.MAC_DINH.plusSeconds(300).toString()));
		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo);
		assertThat(dong).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("CUTRU_THEM_HO_SO", "CUTRU_CAP_NHAT_HO_SO");
		JsonNode chiTiet = NhatKyDb.chiTiet(dong.get(1));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "tinhTrangCuTru",
				"ngayKetThuc");
		assertThat(chiTiet.get("ngayKetThuc").get("cu").isNull()).isTrue();
		assertThat(chiTiet.get("ngayKetThuc").get("moi").asString()).isEqualTo("2027-06-30");
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Đang sử dụng");
	}

	// AC-8, AC-12: hủy sớm hồ sơ chưa bắt đầu bằng ngày kết thúc bằng ngày bắt đầu
	@Test
	void huySomHoSoChuaBatDau() throws Exception {
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Hủy", "Hoạt động");
		String maHoSo = them(cuDan, "Khách thuê", "Tạm trú", "2026-12-01", "2027-01-31");
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, maHoSo,
					CuTruApi.capNhat("Khách thuê", "Tạm trú", "2026-12-01", "2026-12-05", "Hết hiệu lực")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Hủy hồ sơ chưa tới ngày bắt đầu cần đặt ngày kết thúc bằng ngày bắt đầu")));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, maHoSo,
					CuTruApi.capNhat("Thành viên gia đình", "Tạm trú", "2026-12-01", "2026-12-01", "Hết hiệu lực")))
			.andExpect(status().isOk());
		this.mockMvc.perform(CuTruApi.xem(this.bql, this.ma))
			.andExpect(jsonPath("$.data.sapBatDau").isEmpty())
			.andExpect(jsonPath("$.data.lichSuCuTru[*].maHoSo").value(contains(maHoSo)));
		JsonNode chiTiet = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo).get(1));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "ngayKetThuc", "tuDong",
				"vaiTroCuTru");
		assertThat(chiTiet.get("vaiTroCuTru").get("moi").asString()).isEqualTo("Thành viên gia đình");
	}

	// AC-8: thứ tự 400, rồi 404, rồi 409 trạng thái hiện tại
	@Test
	void loiTheoDungThuTu() throws Exception {
		this.mockMvc.perform(CuTruApi.sua(this.bql, "HS99999999", Map.of()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field")
				.value(contains("vaiTroCuTru", "tinhTrangCuTru", "ngayBatDau", "trangThaiHoSo")))
			.andExpect(jsonPath("$.errors[3].message").value("Vui lòng chọn trạng thái hồ sơ"));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, "HS99999999",
					CuTruApi.capNhat("Khách thuê", "Thường trú", "2026-11-01", null, "Chờ xác minh")))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Trạng thái hồ sơ không hợp lệ")));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, "HS99999999",
					CuTruApi.capNhat("Khách thuê", "Thường trú", "2026-11-01", null, "Hết hiệu lực")))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Kết thúc hồ sơ bắt buộc có ngày kết thúc")));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, "HS99999999",
					CuTruApi.capNhat("Khách thuê", "Thường trú", "2026-11-01", "2026-11-17", "Hết hiệu lực")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Ngày kết thúc khi đóng hồ sơ không được lớn hơn hôm nay")));
		Map<String, Object> hopLe = CuTruApi.capNhat("Khách thuê", "Thường trú", "2026-11-01", null, "Hợp lệ");
		for (String sai : List.of("HS99999999", "abc", "HS1")) {
			this.mockMvc.perform(CuTruApi.sua(this.bql, sai, hopLe))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
		}
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Chờ", "Hoạt động");
		String cho = this.jdbc.queryForObject("""
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
				    ngay_bat_dau, trang_thai_ho_so, ma_cu_dan_de_nghi)
				VALUES (?, ?, NULL, 'Khách thuê', 'Thường trú', DATE '2026-11-01', 'Chờ xác minh', ?)
				RETURNING ma_ho_so""", String.class, this.ma, cuDan, cuDan);
		this.mockMvc.perform(CuTruApi.sua(this.bql, cho, hopLe))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_HO_SO_DANG_CHO_XAC_MINH"))
			.andExpect(jsonPath("$.message").value("Hồ sơ đang chờ xác minh, chưa thể chỉnh sửa tại đây"));
		this.mockMvc.perform(CuTruApi.sua(this.auth.accessToken(CuTruApi.CU_DAN_A0201), cho, hopLe))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.sua(this.auth.accessToken(CuTruApi.KE_TOAN), cho, hopLe))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.sua("khong-hop-le", cho, hopLe)).andExpect(status().isUnauthorized());
	}

	// covers: AC-8, AC-12 (PUT trùng ngày cùng cư dân trả 409 và vừa sửa nhiều trường vừa
	// kết thúc ghi đủ nhật ký)
	@Test
	void suaTrungNgayCungCuDanThi409VaVuaSuaVuaDongGhiDuTruong() throws Exception {
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Hai Đợt", "Hoạt động");
		String dotMot = them(cuDan, "Khách thuê", "Tạm trú", "2026-11-01", "2026-11-30");
		String dotHai = them(cuDan, "Khách thuê", "Tạm trú", "2026-12-10", "2027-01-31");

		this.mockMvc
			.perform(CuTruApi.sua(this.bql, dotHai,
					CuTruApi.capNhat("Khách thuê", "Tạm trú", "2026-11-25", "2027-01-31", "Hợp lệ")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN"));

		this.mockMvc
			.perform(CuTruApi.sua(this.bql, dotMot, CuTruApi.capNhat("Thành viên gia đình", "Thường trú", "2026-10-15",
					CuTruApi.HOM_NAY, "Hết hiệu lực")))
			.andExpect(status().isOk());
		JsonNode chiTiet = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("HoSoCuTru:" + dotMot).get(1));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "ngayKetThuc", "tuDong",
				"vaiTroCuTru", "tinhTrangCuTru", "ngayBatDau", "trangThaiCanHo");
		assertThat(chiTiet.has("trangThaiHoSo")).isFalse();
		assertThat(chiTiet.get("vaiTroCuTru").get("moi").asString()).isEqualTo("Thành viên gia đình");
		assertThat(chiTiet.get("tinhTrangCuTru").get("moi").asString()).isEqualTo("Thường trú");
		assertThat(chiTiet.get("ngayBatDau").get("moi").asString()).isEqualTo("2026-10-15");
	}

	// AC-8: đổi sang chủ căn hộ khi căn đã có chủ thì 409, trừ chính hồ sơ đang sửa
	@Test
	void suaThanhChuCanHoKhiDaCoChuThi409() throws Exception {
		String a = CuTruApi.taoCuDanThu(this.jdbc, "Lý Chủ Cũ", "Hoạt động");
		String b = CuTruApi.taoCuDanThu(this.jdbc, "Lý Con", "Hoạt động");
		String chu = them(a, "Chủ căn hộ", "Thường trú", "2026-11-01", null);
		String con = them(b, "Thành viên gia đình", "Thường trú", "2026-11-01", null);
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, con,
					CuTruApi.capNhat("Chủ căn hộ", "Thường trú", "2026-11-01", null, "Hợp lệ")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_DA_CO_CHU_CAN_HO"));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, chu,
					CuTruApi.capNhat("Chủ căn hộ", "Thường trú", "2026-10-01", null, "Hợp lệ")))
			.andExpect(status().isOk());
	}

	// AC-10: sửa tay trạng thái căn phải khớp hồ sơ cư trú; Đang sửa chữa luôn giữ nguyên
	@Test
	void chanSuaTayTrangThaiCanMauThuan() throws Exception {
		this.mockMvc.perform(CuTruApi.suaCanHo(this.bql, this.ma, 60, "Đang sử dụng"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("CANHO_TRANG_THAI_MAU_THUAN_CU_TRU"))
			.andExpect(jsonPath("$.message").value("Trạng thái sử dụng không khớp với hồ sơ cư trú hiện tại"))
			.andExpect(jsonPath("$.errors[0].field").value("trangThaiSuDung"))
			.andExpect(jsonPath("$.errors[0].message")
				.value("Căn hộ chưa có người đang cư trú, không thể đặt trạng thái Đang sử dụng"));
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Sửa Chữa", "Hoạt động");
		String maHoSo = them(cuDan, "Chủ căn hộ", "Thường trú", "2026-11-01", null);
		for (String trangThai : List.of("Trống", "Đã bàn giao")) {
			this.mockMvc.perform(CuTruApi.suaCanHo(this.bql, this.ma, 60, trangThai))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errors[0].message")
					.value("Căn hộ đang có người cư trú, không thể đặt trạng thái Trống hoặc Đã bàn giao"));
		}
		this.mockMvc.perform(CuTruApi.suaCanHo(this.bql, this.ma, 60, "Đang sửa chữa")).andExpect(status().isOk());
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, maHoSo,
					CuTruApi.capNhat("Chủ căn hộ", "Thường trú", "2026-11-01", CuTruApi.HOM_NAY, "Hết hiệu lực")))
			.andExpect(status().isOk());
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Đang sửa chữa");
		assertThat(NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo).get(1)).has("trangThaiCanHo"))
			.isFalse();

		// POST /apartments với Đang sử dụng: 422 trước kiểm trùng
		Map<String, Object> canMoi = new LinkedHashMap<>();
		canMoi.put("maTang", this.ma.substring(0, this.ma.length() - 2));
		canMoi.put("soCan", 1);
		canMoi.put("dienTich", 50);
		canMoi.put("trangThaiSuDung", "Đang sử dụng");
		this.mockMvc
			.perform(CuTruApi.json(
					org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/apartments"),
					this.bql, canMoi))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("CANHO_TRANG_THAI_MAU_THUAN_CU_TRU"))
			.andExpect(jsonPath("$.errors[0].field").value("trangThaiSuDung"));
	}

	private String them(String maCuDan, String vaiTro, String tinhTrang, String batDau, String ketThuc)
			throws Exception {
		String body = this.mockMvc
			.perform(CuTruApi.them(this.bql, CuTruApi.hoSo(this.ma, maCuDan, vaiTro, tinhTrang, batDau, ketThuc)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JSON.readTree(body).get("data").get("maHoSo").asString();
	}

}
