package vn.edu.hust.chungcu.cutru;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
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
import org.springframework.dao.DataIntegrityViolationException;
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
import vn.edu.hust.chungcu.common.error.LoiCoSoDuLieu;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Thêm mới hồ sơ cư trú (spec 0014, AC-4 đến AC-6, AC-9, AC-10, AC-12, AC-13). Mọi hồ sơ
 * tạo ở căn của tòa thử, được {@link CuTruApi#donDep} xóa sau mỗi test.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class HoSoCuTruThemIntegrationTest {

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

	// AC-4, AC-10, AC-12: thêm chủ căn hộ bắt đầu hôm nay, căn tự sang Đang sử dụng
	@Test
	void themChuCanHoThiCanSangDangSuDung() throws Exception {
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Thử Một", "Hoạt động");
		MvcResult kq = this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(" " + this.ma.toLowerCase() + " ", cuDan.toLowerCase(), "Chủ căn hộ", " Thường trú ",
							CuTruApi.HOM_NAY, null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("Thêm mới hồ sơ cư trú thành công"))
			.andExpect(jsonPath("$.data.maHoSo").value(matchesPattern("^HS[0-9]{8}$")))
			.andExpect(jsonPath("$.data.maCanHo").value(this.ma))
			.andExpect(jsonPath("$.data.maCuDan").value(cuDan))
			.andExpect(jsonPath("$.data.hoTen").value("Lý Thử Một"))
			.andExpect(jsonPath("$.data.soCccd").value(matchesPattern("^044[0-9]{9}$")))
			.andExpect(jsonPath("$.data.vaiTroCuTru").value("Chủ căn hộ"))
			.andExpect(jsonPath("$.data.tinhTrangCuTru").value("Thường trú"))
			.andExpect(jsonPath("$.data.ngayBatDau").value(CuTruApi.HOM_NAY))
			.andExpect(jsonPath("$.data.ngayKetThuc").value(nullValue()))
			.andExpect(jsonPath("$.data.trangThaiHoSo").value("Hợp lệ"))
			.andExpect(jsonPath("$.data.maNvbqlXacMinh").value(matchesPattern("^BQL[0-9]{8}$")))
			.andExpect(jsonPath("$.data.hoTenNguoiXacMinh").value("Trần Ban Quản Lý"))
			.andExpect(jsonPath("$.data.taoLuc").value(MutableClock.MAC_DINH.toString()))
			.andExpect(jsonPath("$.data.capNhatLuc").value(MutableClock.MAC_DINH.toString()))
			.andReturn();
		String maHoSo = JSON.readTree(kq.getResponse().getContentAsString()).get("data").get("maHoSo").asString();
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Đang sử dụng");
		this.mockMvc.perform(CuTruApi.chiTietCanHo(this.bql, this.ma))
			.andExpect(jsonPath("$.data.trangThaiSuDung").value("Đang sử dụng"));

		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo);
		assertThat(dong).extracting(d -> d.get("loai_thao_tac")).containsExactly("CUTRU_THEM_HO_SO");
		assertThat(dong.get(0).get("ma_nguoi_dung")).isEqualTo(CuTruApi.maNguoiDungCua(this.jdbc, CuTruApi.BQL));
		JsonNode chiTiet = NhatKyDb.chiTiet(dong.get(0));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "vaiTroCuTru",
				"tinhTrangCuTru", "ngayBatDau", "ngayKetThuc", "trangThaiCanHo");
		assertThat(chiTiet.get("ngayKetThuc").isNull()).isTrue();
		assertThat(chiTiet.get("trangThaiCanHo").get("cu").asString()).isEqualTo("Trống");
		assertThat(chiTiet.get("trangThaiCanHo").get("moi").asString()).isEqualTo("Đang sử dụng");
		assertThat(dong.get(0).get("chi_tiet").toString()).doesNotContain("Lý Thử Một").doesNotContain("044");
	}

	// AC-10, AC-12: hồ sơ chưa bắt đầu không đổi trạng thái căn, nhật ký không có
	// trangThaiCanHo
	@Test
	void hoSoChuaBatDauKhongDoiTrangThaiCan() throws Exception {
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Thử Hai", "Tạm khóa");
		MvcResult kq = this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, cuDan, "Khách thuê", "Tạm trú", "2026-12-01", "2027-05-31")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.ngayKetThuc").value("2027-05-31"))
			.andReturn();
		String maHoSo = JSON.readTree(kq.getResponse().getContentAsString()).get("data").get("maHoSo").asString();
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Trống");
		JsonNode chiTiet = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo).get(0));
		assertThat(chiTiet.has("trangThaiCanHo")).isFalse();
		assertThat(chiTiet.get("ngayKetThuc").asString()).isEqualTo("2027-05-31");
	}

	// AC-5: mỗi trường sai một mục, theo thứ tự trường
	@Test
	void duLieuSaiThi400TheoThuTuTruong() throws Exception {
		this.mockMvc.perform(CuTruApi.themRaw(this.bql, "{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
			.andExpect(jsonPath("$.errors[*].field")
				.value(contains("maCanHo", "maCuDan", "vaiTroCuTru", "tinhTrangCuTru", "ngayBatDau")))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Vui lòng chọn căn hộ", "Vui lòng chọn cư dân",
					"Vui lòng chọn vai trò cư trú", "Vui lòng chọn tình trạng cư trú", "Vui lòng chọn ngày bắt đầu")));
		this.mockMvc.perform(CuTruApi.themRaw(this.bql, """
				{"maCanHo": 5, "maCuDan": "CD1", "vaiTroCuTru": "Chủ", "tinhTrangCuTru": "Tạm trú",
				 "ngayBatDau": "2026-02-30", "ngayKetThuc": null}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field")
				.value(contains("maCanHo", "maCuDan", "vaiTroCuTru", "ngayBatDau", "ngayKetThuc")))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Căn hộ không hợp lệ", "Mã cư dân không hợp lệ",
					"Vai trò cư trú không hợp lệ", "Ngày bắt đầu không hợp lệ", "Tạm trú bắt buộc có ngày kết thúc")));
		String cuDan = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, cuDan, "Khách thuê", "Tạm trú", "2026-11-10", "2026-11-09")))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Ngày kết thúc phải từ ngày bắt đầu trở đi")));
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, cuDan, "Khách thuê", "Tạm trú", "2026-11-10", "2026-11-15")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Ngày kết thúc của hồ sơ hợp lệ không được nhỏ hơn hôm nay")));
		this.mockMvc.perform(CuTruApi.them(this.bql, CuTruApi.hoSo(this.ma, cuDan, "Khách thuê", "Thường trú", "", "")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Vui lòng chọn ngày bắt đầu", "Ngày kết thúc không hợp lệ")));
	}

	// AC-6 bước 1 và 3: căn ngoài phạm vi, cư dân không có, tài khoản ngừng hoạt động
	@Test
	void canHoVaCuDanKhongHopLe() throws Exception {
		String cuDan = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo("KHONG-CO", cuDan, "Khách thuê", "Thường trú", CuTruApi.HOM_NAY, null)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, "CD99999999", "Khách thuê", "Thường trú", CuTruApi.HOM_NAY, null)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Không tìm thấy cư dân"));
		String ngung = CuTruApi.taoCuDanThu(this.jdbc, "Lý Ngừng", "Ngừng hoạt động");
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, ngung, "Khách thuê", "Thường trú", CuTruApi.HOM_NAY, null)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG"))
			.andExpect(jsonPath("$.message")
				.value("Tài khoản cư dân đã ngừng hoạt động, không thể ghi nhận hoặc duyệt hồ sơ cư trú"));
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM ho_so_cu_tru WHERE ma_can_ho = ?", Integer.class,
				this.ma))
			.isZero();
	}

	// AC-6 bước 4 và 5: trùng ngày cùng cư dân, hai chủ căn hộ
	@Test
	void chongNgayThi409() throws Exception {
		String a = CuTruApi.taoCuDanThu(this.jdbc, "Lý Chủ", "Hoạt động");
		String b = CuTruApi.taoCuDanThu(this.jdbc, "Lý Thành Viên", "Hoạt động");
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, a, "Chủ căn hộ", "Thường trú", CuTruApi.HOM_NAY, null)))
			.andExpect(status().isCreated());
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, a, "Khách thuê", "Tạm trú", "2027-01-01", "2027-02-01")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN"))
			.andExpect(jsonPath("$.message").value("Cư dân đã có hồ sơ cư trú hợp lệ giao khoảng ngày tại căn hộ này"));
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, b, "Chủ căn hộ", "Tạm trú", "2030-01-01", "2030-01-01")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_DA_CO_CHU_CAN_HO"))
			.andExpect(jsonPath("$.message").value("Căn hộ đã có chủ căn hộ hợp lệ trong khoảng ngày này"));
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, b, "Thành viên gia đình", "Thường trú", CuTruApi.HOM_NAY, null)))
			.andExpect(status().isCreated());
	}

	// AC-6, AC-9: hai request cùng thêm chủ căn hộ chạy song song, đúng một request thắng
	@Test
	void haiChuCanHoChaySongSong() throws Exception {
		String a = CuTruApi.taoCuDanThu(this.jdbc, "Lý Đua Một", "Hoạt động");
		String b = CuTruApi.taoCuDanThu(this.jdbc, "Lý Đua Hai", "Hoạt động");
		List<MvcResult> kq = AuthApi.songSong(this.mockMvc,
				CuTruApi.them(this.bql, CuTruApi.hoSo(this.ma, a, "Chủ căn hộ", "Thường trú", CuTruApi.HOM_NAY, null)),
				CuTruApi.them(this.bql, CuTruApi.hoSo(this.ma, b, "Chủ căn hộ", "Thường trú", CuTruApi.HOM_NAY, null)));
		assertThat(kq).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(201, 409);
		MvcResult thua = kq.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
		assertThat(thua.getResponse().getContentAsString()).contains("CUTRU_DA_CO_CHU_CAN_HO");
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM ho_so_cu_tru WHERE ma_can_ho = ?", Integer.class,
				this.ma))
			.isEqualTo(1);
	}

	// AC-9: PostgreSQL tự chặn chồng ngày và Tạm trú thiếu ngày kết thúc
	@Test
	void v18ChanDuLieuSaiNgayTrongDb() {
		String a = CuTruApi.taoCuDanThu(this.jdbc, "Lý Sql Một", "Hoạt động");
		String b = CuTruApi.taoCuDanThu(this.jdbc, "Lý Sql Hai", "Hoạt động");
		String sql = """
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
				    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so)
				VALUES (?, ?, (SELECT min(ma_nv) FROM nhan_vien_bql), ?, ?, CAST(? AS date), CAST(? AS date), ?)""";
		this.jdbc.update(sql, this.ma, a, "Chủ căn hộ", "Thường trú", "2026-01-01", null, "Hợp lệ");
		assertThatThrownBy(
				() -> this.jdbc.update(sql, this.ma, a, "Khách thuê", "Tạm trú", "2030-01-01", "2030-02-01", "Hợp lệ"))
			.isInstanceOfSatisfying(DataIntegrityViolationException.class, ex -> {
				assertThat(LoiCoSoDuLieu.sqlState(ex)).hasValue(LoiCoSoDuLieu.LOAI_TRU_GIST);
				assertThat(LoiCoSoDuLieu.tenRangBuoc(ex)).hasValue("ex_hsct_cu_dan_khong_chong_ngay");
			});
		assertThatThrownBy(() -> this.jdbc.update(sql, this.ma, b, "Chủ căn hộ", "Thường trú", "2025-06-01",
				"2026-01-01", "Hợp lệ"))
			.isInstanceOfSatisfying(DataIntegrityViolationException.class,
					ex -> assertThat(LoiCoSoDuLieu.tenRangBuoc(ex)).hasValue("ex_hsct_mot_chu_can_ho"));
		assertThatThrownBy(
				() -> this.jdbc.update(sql, this.ma, b, "Khách thuê", "Tạm trú", "2026-01-01", null, "Hợp lệ"))
			.isInstanceOfSatisfying(DataIntegrityViolationException.class,
					ex -> assertThat(LoiCoSoDuLieu.tenRangBuoc(ex)).hasValue("chk_hsct_ngay_ket_thuc_bat_buoc"));
		assertThatThrownBy(
				() -> this.jdbc.update(sql, this.ma, b, "Khách thuê", "Thường trú", "2026-01-01", null, "Hết hiệu lực"))
			.isInstanceOfSatisfying(DataIntegrityViolationException.class,
					ex -> assertThat(LoiCoSoDuLieu.tenRangBuoc(ex)).hasValue("chk_hsct_ngay_ket_thuc_bat_buoc"));
		// Dòng Hết hiệu lực không tham gia ràng buộc loại trừ
		this.jdbc.update(sql, this.ma, b, "Chủ căn hộ", "Thường trú", "2025-06-01", "2026-01-01", "Hết hiệu lực");
	}

	// covers: AC-6, AC-9 (hai request cùng một cư dân giao khoảng ngày chạy song song,
	// đúng một 201 và một 409)
	@Test
	void haiHoSoCungCuDanChaySongSong() throws Exception {
		String cuDan = CuTruApi.taoCuDanThu(this.jdbc, "Lý Đua Cư Dân", "Hoạt động");
		List<MvcResult> kq = AuthApi.songSong(this.mockMvc,
				CuTruApi.them(this.bql,
						CuTruApi.hoSo(this.ma, cuDan, "Khách thuê", "Tạm trú", CuTruApi.HOM_NAY, "2027-06-30")),
				CuTruApi.them(this.bql,
						CuTruApi.hoSo(this.ma, cuDan, "Thành viên gia đình", "Thường trú", "2027-01-01", null)));
		assertThat(kq).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(201, 409);
		MvcResult thua = kq.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
		assertThat(thua.getResponse().getContentAsString()).contains("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN");
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM ho_so_cu_tru WHERE ma_can_ho = ?", Integer.class,
				this.ma))
			.isEqualTo(1);
	}

	// AC-13: chỉ BQL được thêm
	@Test
	void vaiTroKhacThi403ChuaDangNhapThi401() throws Exception {
		String cuDan = CuTruApi.maCuDanCua(this.jdbc, CuTruApi.CU_DAN_A0201);
		Map<String, Object> body = CuTruApi.hoSo(this.ma, cuDan, "Khách thuê", "Thường trú", CuTruApi.HOM_NAY, null);
		for (String sdt : List.of(CuTruApi.KE_TOAN, CuTruApi.CU_DAN_A0201, CuTruApi.BQT_CU_DAN_A0101)) {
			this.mockMvc.perform(CuTruApi.them(this.auth.accessToken(sdt), body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		}
		this.mockMvc.perform(CuTruApi.them("khong-hop-le", body)).andExpect(status().isUnauthorized());
	}

}
