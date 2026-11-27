package vn.edu.hust.chungcu.cutru;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;

/**
 * Tra cứu hồ sơ cư trú toàn tòa có phân trang, lọc và sắp xếp (spec 0015, AC-9, AC-10,
 * AC-12, AC-14). Ba hồ sơ thử ở căn thử, mốc tạo khác nhau để thứ tự xác định.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class DanhSachHoSoCuTruIntegrationTest {

	private static final String CHEN = """
			INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
			    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so, tao_luc, cap_nhat_luc, ma_cu_dan_de_nghi)
			VALUES (?, ?, CASE WHEN ? THEN (SELECT min(ma_nv) FROM nhan_vien_bql) END, ?, ?, CAST(? AS date),
			    CAST(? AS date), ?, CAST(? AS timestamptz), CAST(? AS timestamptz), ?)
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

	String toa;

	String hopLe;

	String choXacMinh;

	String hetHieuLuc;

	String sdtKhach;

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.bql = this.auth.accessToken(CuTruApi.BQL);
		this.ma = CuTruApi.taoCanThu(this.mockMvc, this.bql);
		this.toa = this.ma.substring(0, this.ma.indexOf('-'));
		String khach = CuTruApi.taoCuDanThu(this.jdbc, "Lý Khách Thuê", "Hoạt động");
		String nguoiThan = CuTruApi.taoCuDanThu(this.jdbc, "Phạm Đề Nghị", "Ngừng hoạt động");
		this.sdtKhach = CuTruApi.soDienThoaiCua(this.jdbc, khach);
		this.hopLe = chen(khach, true, "Khách thuê", "Tạm trú", "2026-10-01", "2027-01-31", "Hợp lệ", "2026-03-01",
				null);
		this.choXacMinh = chen(nguoiThan, false, "Thành viên gia đình", "Thường trú", "2026-12-01", null,
				"Chờ xác minh", "2026-05-01", khach);
		this.hetHieuLuc = chen(nguoiThan, true, "Khách thuê", "Tạm trú", "2025-01-01", "2025-06-30", "Hết hiệu lực",
				"2026-04-01", null);
	}

	@AfterEach
	void tearDown() {
		CuTruApi.donDep(this.jdbc);
		this.clock.reset();
	}

	// AC-9: mặc định tạo lúc giảm dần, phân trang ổn định, đủ trường cho BQL
	@Test
	void macDinhVaPhanTrang() throws Exception {
		this.mockMvc.perform(theoToa())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Tra cứu danh sách hồ sơ cư trú thành công"))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.choXacMinh, this.hetHieuLuc, this.hopLe)))
			.andExpect(jsonPath("$.data.items[0].hoTenNguoiDeNghi").value("Lý Khách Thuê"))
			.andExpect(jsonPath("$.data.items[0].soCccd").value(matchesPattern("^044[0-9]{9}$")))
			.andExpect(jsonPath("$.data.items[2].hoTenNguoiXacMinh").value("Trần Ban Quản Lý"))
			.andExpect(jsonPath("$.data.totalItems").value(3));
		this.mockMvc.perform(theoToa().queryParam("size", "2").queryParam("page", " 2 "))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.hopLe)))
			.andExpect(jsonPath("$.data.page").value(2))
			.andExpect(jsonPath("$.data.totalPages").value(2));
		this.mockMvc.perform(theoToa().queryParam("sort", "ngayKetThuc,asc"))
			.andExpect(
					jsonPath("$.data.items[*].maHoSo").value(contains(this.hetHieuLuc, this.hopLe, this.choXacMinh)));
		this.mockMvc.perform(theoToa().queryParam("sort", "ngayKetThuc,desc"))
			.andExpect(
					jsonPath("$.data.items[*].maHoSo").value(contains(this.choXacMinh, this.hopLe, this.hetHieuLuc)));
		this.mockMvc.perform(theoToa().queryParam("sort", "ngayBatDau,asc"))
			.andExpect(
					jsonPath("$.data.items[*].maHoSo").value(contains(this.hetHieuLuc, this.hopLe, this.choXacMinh)));
	}

	// AC-10: các bộ lọc kết hợp bằng AND, q khớp người, mã căn và mã hồ sơ
	@Test
	void boLocVaTuKhoa() throws Exception {
		this.mockMvc.perform(theoToa().queryParam("trangThaiHoSo", "Hợp lệ", " ", "Hết hiệu lực"))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.hetHieuLuc, this.hopLe)));
		this.mockMvc.perform(theoToa().queryParam("trangThaiHoSo", "Chờ xác minh").queryParam("size", "1"))
			.andExpect(jsonPath("$.data.totalItems").value(1));
		this.mockMvc.perform(theoToa().queryParam("vaiTroCuTru", "Khách thuê"))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.hetHieuLuc, this.hopLe)));
		this.mockMvc.perform(theoToa().queryParam("tinhTrangCuTru", " Thường trú "))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.choXacMinh)));
		this.mockMvc.perform(theoToa().queryParam("soTang", "1")).andExpect(jsonPath("$.data.totalItems").value(3));
		this.mockMvc.perform(theoToa().queryParam("soTang", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalItems").value(0));
		this.mockMvc.perform(theoToa().queryParam("q", "pham de nghi"))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.choXacMinh, this.hetHieuLuc)));
		this.mockMvc.perform(theoToa().queryParam("q", this.sdtKhach))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.hopLe)));
		this.mockMvc.perform(theoToa().queryParam("q", this.hopLe.toLowerCase()))
			.andExpect(jsonPath("$.data.items[*].maHoSo").value(contains(this.hopLe)));
		this.mockMvc.perform(theoToa().queryParam("q", this.ma.toLowerCase()))
			.andExpect(jsonPath("$.data.totalItems").value(3));
		this.mockMvc.perform(theoToa().queryParam("q", "%")).andExpect(jsonPath("$.data.totalItems").value(0));
	}

	// AC-9, AC-14: không lọc tòa thì thấy cả đề nghị seed bị từ chối ở A-0101
	@Test
	void toanToaThayDeNghiSeed() throws Exception {
		this.mockMvc.perform(CuTruApi.danhSach(this.bql).queryParam("trangThaiHoSo", "Từ chối"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].maCanHo").value(hasItem("A-0101")))
			.andExpect(jsonPath("$.data.items[*].lyDoTuChoi")
				.value(hasItem("Chưa nộp bản sao giấy tờ chứng minh quan hệ gia đình")));
		this.mockMvc
			.perform(
					CuTruApi.danhSach(this.bql).queryParam("maToaNha", "a").queryParam("trangThaiHoSo", "Chờ xác minh"))
			.andExpect(jsonPath("$.data.items[*].ghiChuDeNghi")
				.value(hasItem("Người thuê phòng nhỏ, đã có hợp đồng thuê 6 tháng")));
	}

	// AC-10: mỗi tham số sai một mục theo thứ tự, rồi mới tới phạm vi tòa
	@Test
	void thamSoSaiVaPhamVi() throws Exception {
		this.mockMvc
			.perform(CuTruApi.danhSach(this.bql)
				.queryParam("page", "0")
				.queryParam("size", "101")
				.queryParam("sort", "hoTen,asc")
				.queryParam("soTang", "0")
				.queryParam("trangThaiHoSo", "Hợp lệ", "Mới")
				.queryParam("vaiTroCuTru", "Chủ hộ")
				.queryParam("tinhTrangCuTru", "Vãng lai")
				.queryParam("q", "ạ".repeat(101))
				.queryParam("maToaNha", "ZZ"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(
					contains("page", "size", "sort", "soTang", "trangThaiHoSo", "vaiTroCuTru", "tinhTrangCuTru", "q")))
			.andExpect(jsonPath("$.errors[3].message").value("Số tầng phải là số nguyên từ 1 đến 99"))
			.andExpect(jsonPath("$.errors[4].message").value("Trạng thái hồ sơ không hợp lệ"))
			.andExpect(jsonPath("$.errors[5].message").value("Vai trò cư trú không hợp lệ"))
			.andExpect(jsonPath("$.errors[6].message").value("Tình trạng cư trú không hợp lệ"))
			.andExpect(jsonPath("$.errors[7].message").value("Từ khóa tối đa 100 ký tự"));
		this.mockMvc.perform(CuTruApi.danhSach(this.bql).queryParam("soTang", "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Chọn tòa nhà trước khi lọc theo tầng"));
		this.mockMvc.perform(CuTruApi.danhSach(this.bql).queryParam("maToaNha", "ZZ"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
	}

	// AC-12: chỉ BAN_QUAN_LY
	@Test
	void chiBanQuanLy() throws Exception {
		for (String so : new String[] { CuTruApi.CU_DAN_A0201, CuTruApi.KE_TOAN, CuTruApi.BQT_CU_DAN_A0101 }) {
			this.mockMvc.perform(CuTruApi.danhSach(this.auth.accessToken(so))).andExpect(status().isForbidden());
		}
		this.mockMvc.perform(CuTruApi.danhSach("khong-hop-le")).andExpect(status().isUnauthorized());
	}

	private MockHttpServletRequestBuilder theoToa() {
		return CuTruApi.danhSach(this.bql).queryParam("maToaNha", this.toa.toLowerCase());
	}

	private String chen(String maCuDan, boolean coXacMinh, String vaiTro, String tinhTrang, String batDau,
			String ketThuc, String trangThai, String taoLuc, String deNghi) {
		String moc = taoLuc + "T08:00:00+07:00";
		return this.jdbc.queryForObject(CHEN, String.class, this.ma, maCuDan, coXacMinh, vaiTro, tinhTrang, batDau,
				ketThuc, trangThai, moc, moc, deNghi);
	}

}
