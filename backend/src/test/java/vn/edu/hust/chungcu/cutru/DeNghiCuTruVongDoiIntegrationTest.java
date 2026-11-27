package vn.edu.hust.chungcu.cutru;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.cutru.service.HetHanHoSoCuTruService;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Duyệt, từ chối, tự hủy đề nghị cư trú và khóa trạng thái kết thúc (spec 0015, AC-4 đến
 * AC-7, AC-11 đến AC-13).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class DeNghiCuTruVongDoiIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MutableClock clock;

	@Autowired
	HetHanHoSoCuTruService hetHan;

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

	// AC-4, AC-7, AC-13: duyệt sang Hợp lệ, căn đồng bộ, duyệt lại thì 409, sửa sau đó
	// giữ thông tin đề nghị
	@Test
	void duyetRoiSuaGiuThongTinDeNghi() throws Exception {
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Được Duyệt", "Hoạt động");
		String maHoSo = gui(khach, "Thành viên gia đình", "Thường trú", CuTruApi.HOM_NAY, null, "Con trai");
		this.mockMvc.perform(CuTruApi.duyet(this.bql, " " + maHoSo.toLowerCase() + " "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Duyệt đề nghị cư trú thành công"))
			.andExpect(jsonPath("$.data.trangThaiHoSo").value("Hợp lệ"))
			.andExpect(jsonPath("$.data.maNvbqlXacMinh").value(matchesPattern("^BQL[0-9]{8}$")))
			.andExpect(jsonPath("$.data.hoTenNguoiXacMinh").value("Trần Ban Quản Lý"))
			.andExpect(jsonPath("$.data.maCuDanDeNghi").value(this.toi))
			.andExpect(jsonPath("$.data.soCccd").value(matchesPattern("^044[0-9]{9}$")))
			.andExpect(jsonPath("$.data.coTheHuy").value(false));
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Đang sử dụng");
		List<Map<String, Object>> log = this.nhatKy.theoDoiTuong("HoSoCuTru:" + maHoSo);
		assertThat(log).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("CUTRU_GUI_DE_NGHI", "CUTRU_DUYET_DE_NGHI");
		JsonNode chiTiet = NhatKyDb.chiTiet(log.get(1));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "maCuDanDeNghi",
				"vaiTroCuTru", "tinhTrangCuTru", "ngayBatDau", "ngayKetThuc", "trangThaiCanHo");
		assertThat(chiTiet.get("trangThaiCanHo").get("moi").asString()).isEqualTo("Đang sử dụng");

		this.mockMvc.perform(CuTruApi.duyet(this.bql, maHoSo))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH"))
			.andExpect(jsonPath("$.message").value("Đề nghị cư trú không còn ở trạng thái chờ xác minh"));
		this.mockMvc
			.perform(CuTruApi.sua(this.bql, maHoSo,
					CuTruApi.capNhat("Khách thuê", "Thường trú", CuTruApi.HOM_NAY, null, "Hợp lệ")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.maCuDanDeNghi").value(this.toi))
			.andExpect(jsonPath("$.data.ghiChuDeNghi").value("Con trai"));
	}

	// AC-5, AC-6, AC-7, AC-8, AC-11: tự hủy rồi gửi lại được, từ chối kèm lý do, hai
	// trạng thái kết thúc không sửa được
	@Test
	void tuHuyGuiLaiRoiBiTuChoi() throws Exception {
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Gửi Lại", "Hoạt động");
		String daHuy = gui(khach, "Khách thuê", "Tạm trú", "2026-12-01", "2027-03-31", null);
		this.mockMvc.perform(CuTruApi.huy(this.tokenToi, daHuy))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Hủy đề nghị cư trú thành công"))
			.andExpect(jsonPath("$.data.trangThaiHoSo").value("Đã hủy"))
			.andExpect(jsonPath("$.data.maNvbqlXacMinh").value(nullValue()))
			.andExpect(jsonPath("$.data.soCccd").value(nullValue()))
			.andExpect(jsonPath("$.data.coTheHuy").value(false));
		assertThat(this.nhatKy.theoDoiTuong("HoSoCuTru:" + daHuy)).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("CUTRU_GUI_DE_NGHI", "CUTRU_HUY_DE_NGHI");
		this.mockMvc.perform(CuTruApi.huy(this.tokenToi, daHuy))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH"));

		this.clock.advance(Duration.ofMinutes(5));
		String biTuChoi = gui(khach, "Khách thuê", "Tạm trú", "2026-12-01", "2027-03-31", null);
		this.mockMvc.perform(CuTruApi.tuChoi(this.bql, biTuChoi, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].message").value(contains("Vui lòng nhập lý do từ chối")));
		this.mockMvc.perform(CuTruApi.tuChoi(this.bql, biTuChoi, 7))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Lý do từ chối không hợp lệ")));
		this.mockMvc.perform(CuTruApi.tuChoi(this.bql, biTuChoi, "ạ".repeat(501)))
			.andExpect(jsonPath("$.errors[*].message").value(contains("Lý do từ chối tối đa 500 ký tự")));
		this.mockMvc.perform(CuTruApi.tuChoi(this.bql, biTuChoi, "  Thiếu giấy tờ tạm trú  "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Từ chối đề nghị cư trú thành công"))
			.andExpect(jsonPath("$.data.trangThaiHoSo").value("Từ chối"))
			.andExpect(jsonPath("$.data.lyDoTuChoi").value("Thiếu giấy tờ tạm trú"))
			.andExpect(jsonPath("$.data.maNvbqlXacMinh").value(matchesPattern("^BQL[0-9]{8}$")));
		JsonNode chiTiet = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("HoSoCuTru:" + biTuChoi).get(1));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDan", "maCuDanDeNghi",
				"lyDoTuChoi");
		assertThat(CuTruApi.trangThaiCan(this.jdbc, this.ma)).isEqualTo("Trống");

		this.mockMvc.perform(CuTruApi.xem(this.tokenToi, this.ma))
			.andExpect(jsonPath("$.data.deNghiCuTru[*].maHoSo").value(contains(biTuChoi, daHuy)))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].lyDoTuChoi").value("Thiếu giấy tờ tạm trú"))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].coTheHuy").value(false))
			.andExpect(jsonPath("$.data.deNghiCuTru[0].hoTenNguoiXacMinh").value(nullValue()));
		for (String maKetThuc : List.of(daHuy, biTuChoi)) {
			this.mockMvc
				.perform(CuTruApi.sua(this.bql, maKetThuc,
						CuTruApi.capNhat("Khách thuê", "Tạm trú", "2026-12-01", "2027-03-31", "Hợp lệ")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("CUTRU_DE_NGHI_DA_KET_THUC"))
				.andExpect(jsonPath("$.message").value("Đề nghị cư trú đã kết thúc xử lý, không thể chỉnh sửa"));
		}
	}

	// AC-4, AC-7: duyệt theo ngày hôm nay, đề nghị quá hạn không bị job tự đóng, tài
	// khoản
	// ngừng hoạt động và chồng ngày bị chặn
	@Test
	void duyetTheoMocHomNay() throws Exception {
		String quaBatDau = gui(CuTruApi.taoCuDanThu(this.jdbc, "Lý Một", "Hoạt động"), "Khách thuê", "Tạm trú",
				"2026-11-20", "2026-11-25", null);
		String quaKetThuc = gui(CuTruApi.taoCuDanThu(this.jdbc, "Lý Hai", "Hoạt động"), "Khách thuê", "Tạm trú",
				"2026-11-20", "2026-11-21", null);
		this.clock.set(Instant.parse("2026-11-22T01:00:00Z"));
		// Token cũ đã hết hạn theo đồng hồ mới
		String bqlSau = this.auth.accessToken(CuTruApi.BQL);
		this.mockMvc.perform(CuTruApi.duyet(bqlSau, quaBatDau)).andExpect(status().isOk());
		this.mockMvc.perform(CuTruApi.duyet(bqlSau, quaKetThuc))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC"))
			.andExpect(jsonPath("$.message")
				.value("Đề nghị cư trú đã qua ngày kết thúc, vui lòng từ chối để cư dân gửi lại"));
		this.hetHan.chay();
		assertThat(trangThai(quaKetThuc)).isEqualTo("Chờ xác minh");
		this.clock.reset();

		String ngung = CuTruApi.taoCuDanThu(this.jdbc, "Lý Ngừng Sau", "Hoạt động");
		String deNghiNgung = gui(ngung, "Khách thuê", "Thường trú", "2026-12-01", null, null);
		this.jdbc.update("""
				UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = 'Ngừng hoạt động'
				WHERE ma_nguoi_dung = (SELECT ma_nguoi_dung FROM cu_dan WHERE ma_cu_dan = ?)""", ngung);
		this.mockMvc.perform(CuTruApi.duyet(this.bql, deNghiNgung))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG"));

		String chong = CuTruApi.taoCuDanThu(this.jdbc, "Lý Chồng", "Hoạt động");
		String deNghiChong = gui(chong, "Khách thuê", "Thường trú", "2026-12-01", null, null);
		this.mockMvc
			.perform(CuTruApi.them(this.bql,
					CuTruApi.hoSo(this.ma, chong, "Khách thuê", "Tạm trú", "2026-12-15", "2027-01-15")))
			.andExpect(status().isCreated());
		this.mockMvc.perform(CuTruApi.duyet(this.bql, deNghiChong))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN"));
	}

	// AC-6, AC-12: cư dân cùng căn không hủy được đề nghị của người khác, sai vai trò
	// thì 403
	@Test
	void cachLyVaPhanQuyen() throws Exception {
		String maHoSo = gui(CuTruApi.taoCuDanThu(this.jdbc, "Lý Cách Ly", "Hoạt động"), "Khách thuê", "Thường trú",
				"2026-12-01", null, null);
		String tokenB = this.auth.accessToken(CuTruApi.BQT_CU_DAN_A0101);
		for (String sai : List.of(maHoSo, "HS99999999", "abc")) {
			this.mockMvc.perform(CuTruApi.huy(tokenB, sai))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
		}
		this.mockMvc.perform(CuTruApi.duyet(this.bql, "HS99999999")).andExpect(status().isNotFound());
		this.mockMvc.perform(CuTruApi.tuChoi(this.bql, "HS1", "x")).andExpect(status().isNotFound());
		this.mockMvc.perform(CuTruApi.huy(this.bql, maHoSo)).andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.duyet(this.tokenToi, maHoSo)).andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.tuChoi(this.tokenToi, maHoSo, "x")).andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.duyet(this.auth.accessToken(CuTruApi.KE_TOAN), maHoSo))
			.andExpect(status().isForbidden());
		this.mockMvc.perform(CuTruApi.duyet("khong-hop-le", maHoSo)).andExpect(status().isUnauthorized());
		assertThat(trangThai(maHoSo)).isEqualTo("Chờ xác minh");
	}

	// AC-4, AC-6: BQL duyệt và người gửi hủy cùng lúc, đúng một bên thắng
	@Test
	void duyetVaHuyCungLuc() throws Exception {
		String maHoSo = gui(CuTruApi.taoCuDanThu(this.jdbc, "Lý Đua", "Hoạt động"), "Khách thuê", "Thường trú",
				"2026-12-01", null, null);
		List<Integer> ketQua = chayCungLuc(CuTruApi.duyet(this.bql, maHoSo), CuTruApi.huy(this.tokenToi, maHoSo));
		assertThat(ketQua).containsExactlyInAnyOrder(200, 409);
		assertThat(trangThai(maHoSo)).isIn("Hợp lệ", "Đã hủy");
	}

	private String gui(String maCuDan, String vaiTro, String tinhTrang, String batDau, String ketThuc, String ghiChu)
			throws Exception {
		MvcResult kq = this.mockMvc
			.perform(CuTruApi.gui(this.tokenToi,
					CuTruApi.deNghi(this.ma, CuTruApi.soCccdCua(this.jdbc, maCuDan),
							CuTruApi.soDienThoaiCua(this.jdbc, maCuDan), vaiTro, tinhTrang, batDau, ketThuc, ghiChu)))
			.andExpect(status().isCreated())
			.andReturn();
		return JSON.readTree(kq.getResponse().getContentAsString()).get("data").get("maHoSo").asString();
	}

	private String trangThai(String maHoSo) {
		return this.jdbc.queryForObject("SELECT trang_thai_ho_so FROM ho_so_cu_tru WHERE ma_ho_so = ?", String.class,
				maHoSo);
	}

	private List<Integer> chayCungLuc(MockHttpServletRequestBuilder... request) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(request.length);
		CountDownLatch batDau = new CountDownLatch(1);
		try {
			List<Future<Integer>> ketQua = new ArrayList<>();
			for (MockHttpServletRequestBuilder builder : request) {
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
