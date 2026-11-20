package vn.edu.hust.chungcu.nhatky;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.TaiKhoanThu;

/**
 * {@code GET /api/v1/system/audit-logs} và {@code /action-types} (spec 0007, AC-11 đến
 * AC-14). Dòng thử được chèn thẳng bằng SQL cho một tài khoản riêng (hoặc ở năm xa với
 * dòng không có người), nên các test khác ghi thêm dòng cũng không ảnh hưởng.
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class NhatKyTraCuuIntegrationTest {

	private static final String AUDIT_LOGS = "/api/v1/system/audit-logs";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	AuthApi auth;

	@BeforeEach
	void setUp() {
		this.auth = new AuthApi(this.mockMvc);
	}

	static Stream<Arguments> taiKhoanSeed() {
		return Stream.of(Arguments.of("0900000001", 200), Arguments.of("0900000002", 200),
				Arguments.of("0900000003", 403), Arguments.of("0900000004", 403), Arguments.of("0900000005", 403),
				Arguments.of("0900000006", 200), Arguments.of("0900000007", 403));
	}

	// AC-11, AC-14: chỉ Admin, BQL, BQT
	@ParameterizedTest
	@MethodSource("taiKhoanSeed")
	void maTranVaiTro(String soDienThoai, int expected) throws Exception {
		String token = this.auth.accessToken(soDienThoai);
		MvcResult danhSach = this.mockMvc.perform(goi(token, "")).andReturn();
		MvcResult danhMuc = this.mockMvc.perform(get(AUDIT_LOGS + "/action-types").cookie(cookie(token))).andReturn();
		assertThat(danhSach.getResponse().getStatus()).isEqualTo(expected);
		assertThat(danhMuc.getResponse().getStatus()).isEqualTo(expected);
		if (expected == 403) {
			assertThat(body(danhSach)).contains("\"errorCode\":\"FORBIDDEN\"");
			assertThat(body(danhMuc)).contains("\"errorCode\":\"FORBIDDEN\"");
		}
	}

	@Test
	void chuaDangNhapThi401() throws Exception {
		this.mockMvc.perform(get(AUDIT_LOGS))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
	}

	// AC-14
	@Test
	void danhMucTheoThuTuKhaiBao() throws Exception {
		this.mockMvc.perform(get(AUDIT_LOGS + "/action-types").cookie(cookie(this.auth.accessToken("0900000001"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Lấy danh mục loại thao tác thành công"))
			.andExpect(jsonPath("$.data[*].ma").value(contains("AUTH_DANG_NHAP_THANH_CONG", "AUTH_DANG_NHAP_THAT_BAI",
					"AUTH_DANG_XUAT", "AUTH_PHAT_HIEN_DUNG_LAI_TOKEN")))
			.andExpect(jsonPath("$.data[0].nhan").value("Đăng nhập thành công"));
	}

	// AC-11, AC-13: đủ trường, chi tiết là object, giờ UTC có Z, họ tên theo lô
	@Test
	void danhSachDuTruongMoiNhatTruoc() throws Exception {
		String ma = taiKhoanMoi();
		them(ma, "AUTH_DANG_NHAP_THAT_BAI", "2026-03-01T01:00:00Z", "{\"lyDo\": \"SAI_THONG_TIN\"}");
		String moiNhat = them(ma, "AUTH_DANG_NHAP_THANH_CONG", "2026-03-01T02:00:00Z", null);
		this.mockMvc.perform(goi(admin(), "?maNguoiDung=" + ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Tra cứu nhật ký kiểm toán thành công"))
			.andExpect(jsonPath("$.data.totalItems").value(2))
			.andExpect(jsonPath("$.data.items[0].maLog").value(moiNhat))
			.andExpect(jsonPath("$.data.items[0].chiTietThayDoi").value(nullValue()))
			.andExpect(jsonPath("$.data.items[1].maNguoiDung").value(ma))
			.andExpect(jsonPath("$.data.items[1].hoTen").value("Người Thử"))
			.andExpect(jsonPath("$.data.items[1].loaiThaoTac").value("AUTH_DANG_NHAP_THAT_BAI"))
			.andExpect(jsonPath("$.data.items[1].tenThaoTac").value("Đăng nhập thất bại"))
			.andExpect(jsonPath("$.data.items[1].doiTuongTacDong").value("NguoiDungHeThong:" + ma))
			.andExpect(jsonPath("$.data.items[1].chiTietThayDoi.lyDo").value("SAI_THONG_TIN"))
			.andExpect(jsonPath("$.data.items[1].diaChiIp").value("10.0.0.1"))
			.andExpect(jsonPath("$.data.items[1].requestId").value("req-thu"))
			.andExpect(jsonPath("$.data.items[1].thoiGianGhi").value("2026-03-01T01:00:00Z"));
	}

	// AC-11, AC-13: dòng không có người thì họ tên null; mã lạ thì tên là chính mã
	@Test
	void dongKhongCoNguoiVaMaLa() throws Exception {
		this.jdbc.update("""
				INSERT INTO nhat_ky_he_thong (ma_nguoi_dung, loai_thao_tac, doi_tuong_tac_dong, thoi_gian_ghi)
				VALUES (NULL, 'AUTH_DA_BO_TU_LAU', 'NguoiDungHeThong', ?)""",
				Timestamp.from(Instant.parse("1990-05-05T05:00:00Z")));
		this.mockMvc.perform(goi(admin(), "?tuNgay=1990-05-05&denNgay=1990-05-05"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalItems").value(1))
			.andExpect(jsonPath("$.data.items[0].maNguoiDung").value(nullValue()))
			.andExpect(jsonPath("$.data.items[0].hoTen").value(nullValue()))
			.andExpect(jsonPath("$.data.items[0].tenThaoTac").value("AUTH_DA_BO_TU_LAU"));
	}

	// AC-12: tuNgay = denNgay lấy đúng một ngày theo giờ Việt Nam (UTC+7)
	@Test
	void locMotNgayTheoGioVietNam() throws Exception {
		String ma = taiKhoanMoi();
		them(ma, "AUTH_DANG_XUAT", "2026-03-09T16:59:59Z", null); // 23:59:59 ngày 9
		String dau = them(ma, "AUTH_DANG_XUAT", "2026-03-09T17:00:00Z", null); // 00:00
																				// ngày 10
		String cuoi = them(ma, "AUTH_DANG_XUAT", "2026-03-10T16:59:59Z", null); // 23:59:59
																				// ngày 10
		them(ma, "AUTH_DANG_XUAT", "2026-03-10T17:00:00Z", null); // 00:00 ngày 11
		this.mockMvc.perform(goi(admin(), "?maNguoiDung=" + ma + "&tuNgay=2026-03-10&denNgay=2026-03-10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].maLog").value(contains(cuoi, dau)));
	}

	// AC-12: các bộ lọc kết hợp bằng AND; chuỗi rỗng như không gửi
	@Test
	void locTheoLoaiKetHopNguoi() throws Exception {
		String ma = taiKhoanMoi();
		them(ma, "AUTH_DANG_XUAT", "2026-04-01T01:00:00Z", null);
		String thatBai = them(ma, "AUTH_DANG_NHAP_THAT_BAI", "2026-04-01T02:00:00Z", null);
		this.mockMvc
			.perform(goi(admin(), "?maNguoiDung=" + ma + "&loaiThaoTac=AUTH_DANG_NHAP_THAT_BAI&tuNgay=&denNgay="))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].maLog").value(contains(thatBai)));
	}

	// AC-11: tiêu chí phụ và chiều sắp xếp
	@Test
	void sapXepCoTieuChiPhu() throws Exception {
		String ma = taiKhoanMoi();
		String xuat = them(ma, "AUTH_DANG_XUAT", "2026-05-01T01:00:00Z", null);
		String nhap1 = them(ma, "AUTH_DANG_NHAP_THANH_CONG", "2026-05-01T01:00:00Z", null);
		String nhap2 = them(ma, "AUTH_DANG_NHAP_THANH_CONG", "2026-05-01T01:00:00Z", null);
		String nhap3 = them(ma, "AUTH_DANG_NHAP_THANH_CONG", "2026-05-01T02:00:00Z", null);
		String loc = "?maNguoiDung=" + ma;
		this.mockMvc.perform(goi(admin(), loc))
			.andExpect(jsonPath("$.data.items[*].maLog").value(contains(nhap3, nhap2, nhap1, xuat)));
		this.mockMvc.perform(goi(admin(), loc + "&sort=thoiGianGhi,asc"))
			.andExpect(jsonPath("$.data.items[*].maLog").value(contains(xuat, nhap1, nhap2, nhap3)));
		this.mockMvc.perform(goi(admin(), loc + "&sort=loaiThaoTac,asc"))
			.andExpect(jsonPath("$.data.items[*].maLog").value(contains(nhap3, nhap2, nhap1, xuat)));
		this.mockMvc.perform(goi(admin(), loc + "&sort=loaiThaoTac,desc"))
			.andExpect(jsonPath("$.data.items[*].maLog").value(contains(xuat, nhap3, nhap2, nhap1)));
	}

	// AC-11: trang vượt quá trả rỗng, tổng vẫn đúng
	@Test
	void trangVuotQuaThiRong() throws Exception {
		String ma = taiKhoanMoi();
		them(ma, "AUTH_DANG_XUAT", "2026-06-01T01:00:00Z", null);
		them(ma, "AUTH_DANG_XUAT", "2026-06-01T02:00:00Z", null);
		them(ma, "AUTH_DANG_XUAT", "2026-06-01T03:00:00Z", null);
		this.mockMvc.perform(goi(admin(), "?maNguoiDung=" + ma + "&page=9&size=2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items").isEmpty())
			.andExpect(jsonPath("$.data.page").value(9))
			.andExpect(jsonPath("$.data.totalItems").value(3))
			.andExpect(jsonPath("$.data.totalPages").value(2));
	}

	// AC-12: một lỗi cho mỗi tham số sai, theo thứ tự cố định
	@Test
	void thamSoSaiTheoThuTu() throws Exception {
		this.mockMvc
			.perform(goi(admin(),
					"?page=0&size=500&sort=maLog,asc&maNguoiDung=nd00000001"
							+ "&loaiThaoTac=auth_dang_xuat&tuNgay=2026-02-30&denNgay=10/03/2026"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
			.andExpect(jsonPath("$.errors[*].field")
				.value(contains("page", "size", "sort", "maNguoiDung", "loaiThaoTac", "tuNgay", "denNgay")))
			.andExpect(jsonPath("$.errors[3].message").value("Mã người dùng không đúng định dạng"))
			.andExpect(jsonPath("$.errors[4].message").value("Loại thao tác không tồn tại"))
			.andExpect(jsonPath("$.errors[5].message").value("Ngày phải có dạng yyyy-MM-dd"))
			.andExpect(jsonPath("$.errors[6].message").value("Ngày phải có dạng yyyy-MM-dd"));
	}

	// AC-12
	@Test
	void denNgayTruocTuNgay() throws Exception {
		this.mockMvc.perform(goi(admin(), "?tuNgay=2026-03-11&denNgay=2026-03-10"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field").value(contains("denNgay")))
			.andExpect(jsonPath("$.errors[0].message").value("Đến ngày phải bằng hoặc sau từ ngày"));
	}

	// AC-11: việc đọc không tự ghi nhật ký
	@Test
	void docKhongGhiNhatKy() throws Exception {
		String token = admin();
		String truoc = new NhatKyDb(this.jdbc).maLogCuoi();
		this.mockMvc.perform(goi(token, "")).andExpect(status().isOk());
		this.mockMvc.perform(get(AUDIT_LOGS + "/action-types").cookie(cookie(token))).andExpect(status().isOk());
		assertThat(new NhatKyDb(this.jdbc).maLogCuoi()).isEqualTo(truoc);
	}

	private String admin() throws Exception {
		return this.auth.accessToken("0900000001");
	}

	private String taiKhoanMoi() {
		return new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED)).maNguoiDung();
	}

	private String them(String maNguoiDung, String loai, String luc, String chiTiet) {
		return this.jdbc.queryForObject("""
				INSERT INTO nhat_ky_he_thong (ma_nguoi_dung, loai_thao_tac, doi_tuong_tac_dong, thoi_gian_ghi,
				    chi_tiet_thay_doi, dia_chi_ip, request_id)
				VALUES (?, ?, ?, ?, CAST(? AS jsonb), '10.0.0.1', 'req-thu') RETURNING ma_log""", String.class,
				maNguoiDung, loai, "NguoiDungHeThong:" + maNguoiDung, Timestamp.from(Instant.parse(luc)), chiTiet);
	}

	private static MockHttpServletRequestBuilder goi(String token, String query) {
		return get(AUDIT_LOGS + query).cookie(cookie(token));
	}

	private static Cookie cookie(String token) {
		return new Cookie("access_token", token);
	}

	private static String body(MvcResult result) throws Exception {
		return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

}
