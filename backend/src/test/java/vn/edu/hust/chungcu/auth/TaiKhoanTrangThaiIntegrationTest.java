package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Khóa, mở lại, đặt lại mật khẩu, tự bảo vệ, Admin cuối (spec 0008, AC-10 đến AC-12,
 * AC-15).
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class TaiKhoanTrangThaiIntegrationTest {

	static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	AuthApi auth;

	NhatKyDb nhatKy;

	PhienDb phien;

	String admin;

	@BeforeEach
	void setUp() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.phien = new PhienDb(this.jdbc);
		this.admin = this.auth.accessToken(TaiKhoanApi.ADMIN);
	}

	@SafeVarargs
	private JsonNode tao(Map<String, Object>... vaiTro) throws Exception {
		String so = TaiKhoanApi.soMoi();
		MvcResult r = this.mockMvc.perform(TaiKhoanApi.tao(this.admin,
				TaiKhoanApi.body("Người Khóa", so, "khoa" + so + "@chungcu.test", new ArrayList<>(List.of(vaiTro)))))
			.andExpect(status().isCreated())
			.andReturn();
		JsonNode tk = JSON.readTree(r.getResponse().getContentAsString()).get("data").get("taiKhoan");
		// Đăng nhập được bằng mật khẩu seed, không bị bắt đổi
		this.jdbc.update(
				"UPDATE nguoi_dung_he_thong SET mat_khau_hash = ?, phai_doi_mat_khau = false WHERE ma_nguoi_dung = ?",
				this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED), tk.get("maNguoiDung").asString());
		return tk;
	}

	static String ma(JsonNode tk) {
		return tk.get("maNguoiDung").asString();
	}

	static String so(JsonNode tk) {
		return tk.get("soDienThoai").asString();
	}

	long soPhienSong(String maNguoiDung) {
		Long n = this.jdbc.queryForObject(
				"SELECT count(*) FROM phien_dang_nhap WHERE ma_nguoi_dung = ? AND thu_hoi_luc IS NULL", Long.class,
				maNguoiDung);
		return (n != null) ? n : -1;
	}

	List<String> lyDoThuHoi(String maNguoiDung) {
		return this.jdbc.queryForList(
				"SELECT DISTINCT ly_do_thu_hoi FROM phien_dang_nhap WHERE ma_nguoi_dung = ? AND thu_hoi_luc IS NOT NULL",
				String.class, maNguoiDung);
	}

	@Test
	void khoaThuHoiPhienChanDangNhapRoiMoLaiDatLaiBoDem() throws Exception {
		JsonNode tk = tao(TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "Cổng"));
		String access = this.auth.accessToken(so(tk));
		String refresh = this.auth.refreshToken(so(tk));
		assertThat(soPhienSong(ma(tk))).isEqualTo(2);

		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Tạm khóa", "  Nghỉ phép dài  "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Cập nhật trạng thái thành công"))
			.andExpect(jsonPath("$.data.trangThaiTaiKhoan").value("Tạm khóa"))
			.andExpect(jsonPath("$.data.phienBan").value(tk.get("phienBan").asLong() + 1));
		assertThat(soPhienSong(ma(tk))).isZero();
		assertThat(lyDoThuHoi(ma(tk))).containsExactly("Tài khoản bị khóa");
		assertThat(this.auth.refresh(refresh).getResponse().getStatus()).isEqualTo(401);
		this.mockMvc.perform(this.auth.profile(access))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ACCOUNT_LOCKED"));
		MvcResult login = this.auth.login(so(tk), AuthApi.MAT_KHAU_SEED);
		assertThat(login.getResponse().getStatus()).isEqualTo(403);

		Map<String, Object> khoa = this.nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma(tk))
			.stream()
			.filter((d) -> "AUTH_KHOA_TAI_KHOAN".equals(d.get("loai_thao_tac")))
			.findFirst()
			.orElseThrow();
		assertThat(NhatKyDb.chiTiet(khoa).toString()).isEqualTo(
				"{\"lyDo\":\"Nghỉ phép dài\",\"trangThaiCu\":\"Hoạt động\",\"trangThaiMoi\":\"Tạm khóa\",\"soPhienThuHoi\":2}");

		// Cùng trạng thái: không đổi gì
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Tạm khóa", "Lần nữa"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phienBan").value(tk.get("phienBan").asLong() + 1));

		// Mở lại: lý do không bắt buộc, bộ đếm về 0
		this.jdbc.update(
				"UPDATE nguoi_dung_he_thong SET so_lan_dang_nhap_sai = 4, khoa_tam_den = now() WHERE ma_nguoi_dung = ?",
				ma(tk));
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Hoạt động", null))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.trangThaiTaiKhoan").value("Hoạt động"));
		Map<String, Object> dong = this.jdbc.queryForMap(
				"SELECT so_lan_dang_nhap_sai, khoa_tam_den FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?", ma(tk));
		assertThat(dong.get("so_lan_dang_nhap_sai")).isEqualTo(0);
		assertThat(dong.get("khoa_tam_den")).isNull();
		assertThat(this.auth.login(so(tk), AuthApi.MAT_KHAU_SEED).getResponse().getStatus()).isEqualTo(200);
		assertThat(this.nhatKy.loaiCua(TaiKhoanApi.maTheoSo(this.jdbc, TaiKhoanApi.ADMIN)))
			.contains("AUTH_MO_KHOA_TAI_KHOAN");
	}

	@Test
	void duLieuTrangThaiSai() throws Exception {
		JsonNode tk = tao(TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "Cổng"));
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Đóng băng", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("trangThai"))
			.andExpect(jsonPath("$.errors[0].message").value("Trạng thái không tồn tại"));
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Tạm khóa", "  "))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("lyDo"))
			.andExpect(jsonPath("$.errors[0].message").value("Vui lòng nhập lý do"));
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Tạm khóa", "x".repeat(501)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Lý do tối đa 500 ký tự"));
	}

	@Test
	void khongTuKhoaKhongTuDatLai() throws Exception {
		String maAdmin = TaiKhoanApi.maTheoSo(this.jdbc, TaiKhoanApi.ADMIN);
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, maAdmin, "Tạm khóa", "Thử"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("AUTH_SELF_PROTECTED"));
		this.mockMvc.perform(TaiKhoanApi.datLai(this.admin, maAdmin))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("AUTH_SELF_PROTECTED"))
			.andExpect(jsonPath("$.message")
				.value("Bạn không thể tự đặt lại mật khẩu của mình. Hãy dùng trang Đổi mật khẩu."));
		// Phiên của Admin còn nguyên
		this.mockMvc.perform(this.auth.profile(this.admin)).andExpect(status().isOk());
	}

	@Test
	void bqlKhongDoiTrangThaiTaiKhoanNgungHoatDong() throws Exception {
		String bql = this.auth.accessToken(TaiKhoanApi.BQL);
		JsonNode cd = tao(TaiKhoanApi.vaiTro("CU_DAN", "soCccd", TaiKhoanApi.cccdMoi()));
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(bql, ma(cd), "Tạm khóa", "Nợ phí")).andExpect(status().isOk());
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(bql, ma(cd), "Hoạt động", null)).andExpect(status().isOk());
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(bql, ma(cd), "Ngừng hoạt động", "Chuyển đi"))
			.andExpect(status().isOk());
		for (String trangThai : List.of("Hoạt động", "Tạm khóa")) {
			this.mockMvc.perform(TaiKhoanApi.doiTrangThai(bql, ma(cd), trangThai, "Quay lại"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		}
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(cd), "Hoạt động", null))
			.andExpect(status().isOk());
		// Tài khoản nhân viên: 404
		this.mockMvc
			.perform(TaiKhoanApi.doiTrangThai(bql, TaiKhoanApi.maTheoSo(this.jdbc, TaiKhoanApi.KY_THUAT), "Tạm khóa",
					"X"))
			.andExpect(status().isNotFound());
	}

	@Test
	void datLaiMatKhauThuHoiPhienVaBatDoi() throws Exception {
		JsonNode tk = tao(TaiKhoanApi.vaiTro("KY_THUAT", "chuyenMonKyThuat", "Nước"));
		this.auth.accessToken(so(tk));
		this.auth.accessToken(so(tk));
		String maLogTruoc = this.nhatKy.maLogCuoi();

		MvcResult r = this.mockMvc.perform(TaiKhoanApi.datLai(this.admin, ma(tk)))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"))
			.andReturn();
		String matKhauTam = JSON.readTree(r.getResponse().getContentAsString())
			.get("data")
			.get("matKhauTam")
			.asString();
		assertThat(matKhauTam).hasSize(12);
		assertThat(soPhienSong(ma(tk))).isZero();
		assertThat(lyDoThuHoi(ma(tk))).containsExactly("Đặt lại mật khẩu");
		assertThat(this.auth.login(so(tk), AuthApi.MAT_KHAU_SEED).getResponse().getStatus()).isEqualTo(401);
		MvcResult login = this.auth.login(so(tk), matKhauTam);
		assertThat(login.getResponse().getStatus()).isEqualTo(200);
		assertThat(
				JSON.readTree(login.getResponse().getContentAsString()).get("data").get("phaiDoiMatKhau").asBoolean())
			.isTrue();
		assertThat(this.jdbc.queryForObject("SELECT doi_mat_khau_luc FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				Object.class, ma(tk)))
			.isNotNull();
		Map<String, Object> log = this.nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma(tk))
			.stream()
			.filter((d) -> "AUTH_DAT_LAI_MAT_KHAU".equals(d.get("loai_thao_tac")))
			.findFirst()
			.orElseThrow();
		assertThat(NhatKyDb.chiTiet(log).toString()).isEqualTo("{\"soPhienThuHoi\":2}");
		assertThat(this.nhatKy.toanBoSau(maLogTruoc)).doesNotContain(matKhauTam);

		// Tài khoản đang khóa vẫn đặt lại được, nhưng chưa đăng nhập được
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Tạm khóa", "Khóa"))
			.andExpect(status().isOk());
		MvcResult lan2 = this.mockMvc.perform(TaiKhoanApi.datLai(this.admin, ma(tk)))
			.andExpect(status().isOk())
			.andReturn();
		String mk2 = JSON.readTree(lan2.getResponse().getContentAsString()).get("data").get("matKhauTam").asString();
		assertThat(this.auth.login(so(tk), mk2).getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void lamMoiChayDuaVoiKhoaKhongDeLaiPhienSong() throws Exception {
		JsonNode tk = tao(TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "Cổng"));
		String refresh = this.auth.refreshToken(so(tk));
		AuthApi.songSong(this.mockMvc, AuthApi.refreshRequest(refresh),
				TaiKhoanApi.doiTrangThai(this.admin, ma(tk), "Tạm khóa", "Chạy đua"));
		assertThat(soPhienSong(ma(tk))).isZero();
	}

	@Test
	void bqlDatLaiChayDuaVoiAdminThemVaiTroNhanVien() throws Exception {
		String bql = this.auth.accessToken(TaiKhoanApi.BQL);
		JsonNode cd = tao(TaiKhoanApi.vaiTro("CU_DAN", "soCccd", TaiKhoanApi.cccdMoi()));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("phienBan", cd.get("phienBan").asLong());
		body.put("phamViPhuTrach", "Tòa A");
		List<MvcResult> kq = AuthApi.songSong(this.mockMvc, TaiKhoanApi.datLai(bql, ma(cd)),
				TaiKhoanApi.suaVaiTro(this.admin, ma(cd), "KE_TOAN", body));
		List<Integer> ma = List.of(kq.get(0).getResponse().getStatus(), kq.get(1).getResponse().getStatus());
		// Hai thao tác chạy lần lượt dưới khóa dòng: đặt lại trước thì phienBan tăng nên
		// Admin
		// nhận 409; Admin trước thì tài khoản không còn là cư dân thuần nên BQL nhận 404.
		// Không
		// bao giờ cả hai cùng thành công.
		assertThat(ma).isIn(List.of(200, 409), List.of(404, 200));
	}

	@Test
	void haiAdminGoQuyenNhauCungLucConDungMotAdmin() throws Exception {
		JsonNode a = tao(TaiKhoanApi.vaiTro("QUAN_TRI_VIEN"), TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "A"));
		JsonNode b = tao(TaiKhoanApi.vaiTro("QUAN_TRI_VIEN"), TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "B"));
		String tokenA = this.auth.accessToken(so(a));
		String tokenB = this.auth.accessToken(so(b));
		List<String> tamKhoa = tamKhoaAdminKhac(ma(a), ma(b));
		try {
			List<MvcResult> kq = AuthApi.songSong(this.mockMvc,
					TaiKhoanApi.goVaiTro(tokenA, ma(b), "QUAN_TRI_VIEN", b.get("phienBan").asLong()),
					TaiKhoanApi.goVaiTro(tokenB, ma(a), "QUAN_TRI_VIEN", a.get("phienBan").asLong()));
			assertThat(kq.stream().map((r) -> r.getResponse().getStatus()).sorted()).containsExactly(200, 422);
			MvcResult thua = kq.stream().filter((r) -> r.getResponse().getStatus() == 422).findFirst().orElseThrow();
			assertThat(JSON.readTree(thua.getResponse().getContentAsString()).get("errorCode").asString())
				.isEqualTo("AUTH_LAST_ADMIN");
			assertThat(demAdminHoatDong()).isEqualTo(1);
		}
		finally {
			moLai(tamKhoa);
		}
	}

	@Test
	void haiAdminKhoaNhauCungLucConDungMotAdmin() throws Exception {
		JsonNode a = tao(TaiKhoanApi.vaiTro("QUAN_TRI_VIEN"));
		JsonNode b = tao(TaiKhoanApi.vaiTro("QUAN_TRI_VIEN"));
		String tokenA = this.auth.accessToken(so(a));
		String tokenB = this.auth.accessToken(so(b));
		List<String> tamKhoa = tamKhoaAdminKhac(ma(a), ma(b));
		try {
			List<MvcResult> kq = AuthApi.songSong(this.mockMvc,
					TaiKhoanApi.doiTrangThai(tokenA, ma(b), "Tạm khóa", "Chạy đua"),
					TaiKhoanApi.doiTrangThai(tokenB, ma(a), "Tạm khóa", "Chạy đua"));
			assertThat(kq.stream().map((r) -> r.getResponse().getStatus()).sorted()).containsExactly(200, 422);
			assertThat(demAdminHoatDong()).isEqualTo(1);
		}
		finally {
			moLai(tamKhoa);
			this.jdbc.update(
					"UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = 'Hoạt động' WHERE ma_nguoi_dung IN (?, ?)",
					ma(a), ma(b));
		}
	}

	/**
	 * Tạm khóa mọi Admin đang hoạt động khác {@code giu}, để luật Admin cuối có tác dụng;
	 * trả danh sách đã khóa.
	 */
	List<String> tamKhoaAdminKhac(String... giu) {
		List<String> ds = this.jdbc
			.queryForList(
					"""
							SELECT n.ma_nguoi_dung FROM nguoi_dung_he_thong n JOIN quan_tri_vien q ON q.ma_nguoi_dung = n.ma_nguoi_dung
							WHERE n.trang_thai_tai_khoan = 'Hoạt động'""",
					String.class)
			.stream()
			.filter((m) -> !List.of(giu).contains(m))
			.toList();
		ds.forEach((m) -> this.jdbc
			.update("UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = 'Tạm khóa' WHERE ma_nguoi_dung = ?", m));
		return ds;
	}

	void moLai(List<String> ds) {
		ds.forEach((m) -> this.jdbc
			.update("UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = 'Hoạt động' WHERE ma_nguoi_dung = ?", m));
	}

	long demAdminHoatDong() {
		Long n = this.jdbc.queryForObject("""
				SELECT count(*) FROM nguoi_dung_he_thong n JOIN quan_tri_vien q ON q.ma_nguoi_dung = n.ma_nguoi_dung
				WHERE n.trang_thai_tai_khoan = 'Hoạt động'""", Long.class);
		return (n != null) ? n : -1;
	}

}
