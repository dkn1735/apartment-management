package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
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

/** Chi tiết, sửa thông tin và vai trò (spec 0008, AC-3, AC-6 đến AC-9, AC-15, AC-16). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class TaiKhoanSuaIntegrationTest {

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

	String admin;

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.admin = this.auth.accessToken(TaiKhoanApi.ADMIN);
	}

	/** Admin tạo tài khoản với các vai trò cho trước; trả chi tiết. */
	JsonNode tao(List<Map<String, Object>> vaiTro) throws Exception {
		String so = TaiKhoanApi.soMoi();
		MvcResult r = this.mockMvc
			.perform(TaiKhoanApi.tao(this.admin,
					TaiKhoanApi.body("Người Sửa", so, "sua" + so + "@chungcu.test", new ArrayList<>(vaiTro))))
			.andExpect(status().isCreated())
			.andReturn();
		return JSON.readTree(r.getResponse().getContentAsString()).get("data").get("taiKhoan");
	}

	/**
	 * Cho tài khoản đăng nhập được bằng mật khẩu seed, không bị bắt đổi; trả access
	 * token.
	 */
	String dangNhapDuoc(JsonNode tk) throws Exception {
		this.jdbc.update(
				"UPDATE nguoi_dung_he_thong SET mat_khau_hash = ?, phai_doi_mat_khau = false WHERE ma_nguoi_dung = ?",
				this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED), tk.get("maNguoiDung").asString());
		return this.auth.accessToken(tk.get("soDienThoai").asString());
	}

	JsonNode chiTiet(String token, String ma) throws Exception {
		MvcResult r = this.mockMvc.perform(TaiKhoanApi.chiTiet(token, ma)).andExpect(status().isOk()).andReturn();
		return JSON.readTree(r.getResponse().getContentAsString()).get("data");
	}

	String maTheoSo(String so) {
		return this.jdbc.queryForObject("SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?",
				String.class, so);
	}

	static Map<String, Object> thongTin(JsonNode tk, String hoTen, String so, String email) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("hoTen", hoTen);
		body.put("soDienThoai", so);
		body.put("email", email);
		body.put("phienBan", tk.get("phienBan").asLong());
		return body;
	}

	static Map<String, Object> vaiTroBody(long phienBan, String... truongGiaTri) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("phienBan", phienBan);
		for (int i = 0; i < truongGiaTri.length; i += 2) {
			body.put(truongGiaTri[i], truongGiaTri[i + 1]);
		}
		return body;
	}

	@Test
	void chiTiet404KhiSaiDangKhongCoHoacNgoaiPhamVi() throws Exception {
		for (String ma : List.of("abc", "ND99999999")) {
			this.mockMvc.perform(TaiKhoanApi.chiTiet(this.admin, ma))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản"));
		}
		String bql = this.auth.accessToken(TaiKhoanApi.BQL);
		// Ban quản trị kiêm cư dân: không phải cư dân thuần
		this.mockMvc.perform(TaiKhoanApi.chiTiet(bql, maTheoSo("0900000006")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản"));
		JsonNode cuDan = chiTiet(bql, maTheoSo("0900000007"));
		assertThat(cuDan.get("vaiTro").get(0).get("soCccd").asString()).matches("^[0-9]{12}$");
		assertThat(cuDan.get("vaiTro").get(0).get("maVaiTro").asString()).matches("^CD[0-9]{8}$");
		// Danh sách của BQL chỉ có cư dân thuần
		MvcResult ds = this.mockMvc.perform(TaiKhoanApi.danhSach(bql).param("size", "100"))
			.andExpect(status().isOk())
			.andReturn();
		JsonNode items = JSON.readTree(ds.getResponse().getContentAsString()).get("data").get("items");
		assertThat(items).isNotEmpty();
		items.forEach((it) -> assertThat(it.get("vaiTro").toString()).isEqualTo("[\"CU_DAN\"]"));
	}

	@Test
	void suaThongTinChungGhiTenTruongVaPhienBanTang() throws Exception {
		JsonNode tk = tao(List.of(TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "Cổng chính")));
		String ma = tk.get("maNguoiDung").asString();
		String soMoi = TaiKhoanApi.soMoi();
		this.mockMvc
			.perform(TaiKhoanApi.sua(this.admin, ma, thongTin(tk, "Tên  Mới", soMoi, tk.get("email").asString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Cập nhật tài khoản thành công"))
			.andExpect(jsonPath("$.data.hoTen").value("Tên Mới"))
			.andExpect(jsonPath("$.data.soDienThoai").value(soMoi))
			.andExpect(jsonPath("$.data.phienBan").value(tk.get("phienBan").asLong() + 1));
		List<Map<String, Object>> log = this.nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma);
		assertThat(log).extracting((d) -> d.get("loai_thao_tac"))
			.containsExactly("AUTH_TAO_TAI_KHOAN", "AUTH_CAP_NHAT_TAI_KHOAN");
		assertThat(NhatKyDb.chiTiet(log.get(1)).toString())
			.isEqualTo("{\"truongThayDoi\":[\"hoTen\",\"soDienThoai\"]}");
		assertThat(log.get(1).toString()).doesNotContain(soMoi);

		// Không đổi gì: 200, phiên bản giữ nguyên, không thêm dòng nhật ký
		JsonNode sau = chiTiet(this.admin, ma);
		this.mockMvc
			.perform(TaiKhoanApi.sua(this.admin, ma, thongTin(sau, "Tên Mới", soMoi, sau.get("email").asString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phienBan").value(sau.get("phienBan").asLong()));
		assertThat(this.nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma)).hasSize(2);

		// Phiên bản cũ: 409
		this.mockMvc.perform(TaiKhoanApi.sua(this.admin, ma, thongTin(tk, "Khác", soMoi, sau.get("email").asString())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"))
			.andExpect(jsonPath("$.message")
				.value("Tài khoản vừa được thay đổi ở nơi khác, vui lòng tải lại và thử lại."));

		// Trùng số của tài khoản khác: 409 theo trường
		Map<String, Object> trung = thongTin(sau, "Tên Mới", "0900000003", sau.get("email").asString());
		this.mockMvc.perform(TaiKhoanApi.sua(this.admin, ma, trung))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("AUTH_IDENTITY_TAKEN"))
			.andExpect(jsonPath("$.errors[0].field").value("soDienThoai"));

		// Thiếu phienBan: 400
		Map<String, Object> thieu = thongTin(sau, "Tên Mới", soMoi, sau.get("email").asString());
		thieu.remove("phienBan");
		this.mockMvc.perform(TaiKhoanApi.sua(this.admin, ma, thieu))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("phienBan"))
			.andExpect(jsonPath("$.errors[0].message").value("Thiếu phiên bản dữ liệu, vui lòng tải lại"));
	}

	@Test
	void themSuaGoVaiTroVaNhatKy() throws Exception {
		JsonNode tk = tao(List.of(TaiKhoanApi.vaiTro("KY_THUAT", "chuyenMonKyThuat", "Điện")));
		String ma = tk.get("maNguoiDung").asString();
		long v = tk.get("phienBan").asLong();

		this.mockMvc.perform(TaiKhoanApi.suaVaiTro(this.admin, ma, "BAO_VE", vaiTroBody(v, "viTriTruc", "Hầm xe")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Cập nhật vai trò thành công"))
			.andExpect(jsonPath("$.data.vaiTro[0].vaiTro").value("KY_THUAT"))
			.andExpect(jsonPath("$.data.vaiTro[1].vaiTro").value("BAO_VE"))
			.andExpect(jsonPath("$.data.vaiTro[1].viTriTruc").value("Hầm xe"))
			.andExpect(jsonPath("$.data.phienBan").value(v + 1));
		this.mockMvc
			.perform(TaiKhoanApi.suaVaiTro(this.admin, ma, "BAO_VE", vaiTroBody(v + 1, "viTriTruc", "Cổng phụ")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phienBan").value(v + 2));
		// Không đổi gì: phiên bản giữ nguyên
		this.mockMvc
			.perform(TaiKhoanApi.suaVaiTro(this.admin, ma, "BAO_VE", vaiTroBody(v + 2, "viTriTruc", "Cổng phụ")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.phienBan").value(v + 2));
		// Trường không thuộc vai trò, mã vai trò sai
		this.mockMvc
			.perform(
					TaiKhoanApi.suaVaiTro(this.admin, ma, "BAO_VE", vaiTroBody(v + 2, "viTriTruc", "A", "chucVu", "B")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("chucVu"));
		this.mockMvc.perform(TaiKhoanApi.suaVaiTro(this.admin, ma, "KHONG_CO", vaiTroBody(v + 2)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("vaiTro"));

		// Gỡ: không có vai trò 404, phiên bản cũ 409, thành công
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(this.admin, ma, "KE_TOAN", v + 2))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Tài khoản không có vai trò này"));
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(this.admin, ma, "BAO_VE", v))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(this.admin, ma, "BAO_VE", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("phienBan"));
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(this.admin, ma, "BAO_VE", v + 2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Gỡ vai trò thành công"))
			.andExpect(jsonPath("$.data.vaiTro.length()").value(1));
		// Vai trò cuối
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(this.admin, ma, "KY_THUAT", v + 3))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("AUTH_LAST_ROLE"));

		assertThat(this.nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma)).extracting((d) -> d.get("loai_thao_tac"))
			.containsExactly("AUTH_TAO_TAI_KHOAN", "AUTH_GAN_VAI_TRO", "AUTH_CAP_NHAT_VAI_TRO", "AUTH_GO_VAI_TRO");
		Map<String, Object> capNhat = this.nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma).get(2);
		assertThat(NhatKyDb.chiTiet(capNhat).toString())
			.isEqualTo("{\"vaiTro\":\"BAO_VE\",\"truongThayDoi\":[\"viTriTruc\"]}");
	}

	@Test
	void goVaiTroDangBiThamChieuThi409() throws Exception {
		// 0900000006 có hồ sơ cư trú trỏ vào dòng cu_dan
		String ma = maTheoSo("0900000006");
		JsonNode tk = chiTiet(this.admin, ma);
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(this.admin, ma, "CU_DAN", tk.get("phienBan").asLong()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("AUTH_ROLE_IN_USE"));
		JsonNode sau = chiTiet(this.admin, ma);
		assertThat(sau.get("phienBan").asLong()).isEqualTo(tk.get("phienBan").asLong());
		assertThat(sau.get("vaiTro").toString()).contains("CU_DAN");
	}

	@Test
	void adminKhongTuGoQuyenQuanTri() throws Exception {
		JsonNode a = tao(List.of(TaiKhoanApi.vaiTro("QUAN_TRI_VIEN"), TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "X")));
		String tokenA = dangNhapDuoc(a);
		JsonNode chiTietA = chiTiet(tokenA, a.get("maNguoiDung").asString());
		this.mockMvc
			.perform(TaiKhoanApi.goVaiTro(tokenA, a.get("maNguoiDung").asString(), "QUAN_TRI_VIEN",
					chiTietA.get("phienBan").asLong()))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("AUTH_SELF_PROTECTED"));
		// Admin khác gỡ được
		this.mockMvc
			.perform(TaiKhoanApi.goVaiTro(this.admin, a.get("maNguoiDung").asString(), "QUAN_TRI_VIEN",
					chiTietA.get("phienBan").asLong()))
			.andExpect(status().isOk());
	}

	@Test
	void trungCccdKhiSuaVaiTroCuDan() throws Exception {
		JsonNode a = tao(List.of(TaiKhoanApi.vaiTro("CU_DAN", "soCccd", TaiKhoanApi.cccdMoi())));
		JsonNode b = tao(List.of(TaiKhoanApi.vaiTro("CU_DAN", "soCccd", TaiKhoanApi.cccdMoi())));
		String cccdA = a.get("vaiTro").get(0).get("soCccd").asString();
		this.mockMvc
			.perform(TaiKhoanApi.suaVaiTro(this.admin, b.get("maNguoiDung").asString(), "CU_DAN",
					vaiTroBody(b.get("phienBan").asLong(), "soCccd", cccdA)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("AUTH_IDENTITY_TAKEN"))
			.andExpect(jsonPath("$.errors[0].field").value("soCccd"));
	}

	@Test
	void bqlChiSuaSoCccdKhongGoVaiTro() throws Exception {
		String bql = this.auth.accessToken(TaiKhoanApi.BQL);
		JsonNode cd = tao(List.of(TaiKhoanApi.vaiTro("CU_DAN", "soCccd", TaiKhoanApi.cccdMoi())));
		String ma = cd.get("maNguoiDung").asString();
		long v = cd.get("phienBan").asLong();
		this.mockMvc.perform(TaiKhoanApi.suaVaiTro(bql, ma, "KE_TOAN", vaiTroBody(v, "phamViPhuTrach", "Tòa A")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(bql, ma, "CU_DAN", v)).andExpect(status().isForbidden());
		String cccdMoi = TaiKhoanApi.cccdMoi();
		this.mockMvc.perform(TaiKhoanApi.suaVaiTro(bql, ma, "CU_DAN", vaiTroBody(v, "soCccd", cccdMoi)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.vaiTro[0].soCccd").value(cccdMoi));
		// BQL sửa thông tin chung được
		JsonNode sau = chiTiet(bql, ma);
		this.mockMvc
			.perform(TaiKhoanApi.sua(bql, ma,
					thongTin(sau, "Cư Dân Đã Sửa", sau.get("soDienThoai").asString(), sau.get("email").asString())))
			.andExpect(status().isOk());
		// Tài khoản nhân viên: 404 kể cả khi gọi vai trò khác CU_DAN
		this.mockMvc
			.perform(
					TaiKhoanApi.suaVaiTro(bql, maTheoSo("0900000003"), "KE_TOAN", vaiTroBody(0, "phamViPhuTrach", "X")))
			.andExpect(status().isNotFound());
	}

}
