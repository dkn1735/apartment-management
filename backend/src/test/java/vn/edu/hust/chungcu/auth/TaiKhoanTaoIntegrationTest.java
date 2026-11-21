package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

/** Tạo tài khoản và danh sách (spec 0008, AC-1 đến AC-5, AC-15, AC-16). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class TaiKhoanTaoIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

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

	JsonNode data(MvcResult result) throws Exception {
		return JSON.readTree(result.getResponse().getContentAsString()).get("data");
	}

	@Test
	void adminTaoTaiKhoanHaiVaiTroNhanMatKhauTamMotLan() throws Exception {
		String so = TaiKhoanApi.soMoi();
		String cccd = TaiKhoanApi.cccdMoi();
		Map<String, Object> body = TaiKhoanApi.body("  nguyễn   văn  AN ",
				"+84 " + so.substring(1, 4) + "." + so.substring(4), "  An." + so + "@ChungCu.Test ",
				List.of(TaiKhoanApi.vaiTro("CU_DAN", "soCccd", cccd),
						TaiKhoanApi.vaiTro("BAN_QUAN_LY", "chucVu", " Trưởng ca ", "boPhan", "Vận hành")));
		String maLogTruoc = this.nhatKy.maLogCuoi();
		// Sau giờ tạo của mọi tài khoản seed (seed dùng now() thật), để tài khoản mới
		// đứng đầu danh sách
		this.clock.set(Instant.parse("2099-01-01T00:00:00Z"));
		this.admin = this.auth.accessToken(TaiKhoanApi.ADMIN);

		MvcResult result = this.mockMvc.perform(TaiKhoanApi.tao(this.admin, body))
			.andExpect(status().isCreated())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.message").value("Tạo tài khoản thành công"))
			.andExpect(jsonPath("$.data.taiKhoan.hoTen").value("nguyễn văn AN"))
			.andExpect(jsonPath("$.data.taiKhoan.soDienThoai").value(so))
			.andExpect(jsonPath("$.data.taiKhoan.email").value("an." + so + "@chungcu.test"))
			.andExpect(jsonPath("$.data.taiKhoan.trangThaiTaiKhoan").value("Hoạt động"))
			.andExpect(jsonPath("$.data.taiKhoan.phaiDoiMatKhau").value(true))
			.andExpect(jsonPath("$.data.taiKhoan.taoLuc").value("2099-01-01T00:00:00Z"))
			// Thứ tự enum: BAN_QUAN_LY trước CU_DAN
			.andExpect(jsonPath("$.data.taiKhoan.vaiTro[0].vaiTro").value("BAN_QUAN_LY"))
			.andExpect(jsonPath("$.data.taiKhoan.vaiTro[0].chucVu").value("Trưởng ca"))
			.andExpect(jsonPath("$.data.taiKhoan.vaiTro[0].soCccd").doesNotExist())
			.andExpect(jsonPath("$.data.taiKhoan.vaiTro[1].vaiTro").value("CU_DAN"))
			.andExpect(jsonPath("$.data.taiKhoan.vaiTro[1].soCccd").value(cccd))
			.andExpect(jsonPath("$.data.taiKhoan.matKhauHash").doesNotExist())
			.andReturn();

		JsonNode data = data(result);
		String ma = data.get("taiKhoan").get("maNguoiDung").asString();
		String matKhauTam = data.get("matKhauTam").asString();
		assertThat(matKhauTam).hasSize(12).matches("[A-HJ-NP-Za-km-z2-9]+").matches(".*[A-Za-z].*").matches(".*\\d.*");
		assertThat(data.get("taiKhoan").get("vaiTro").get(0).get("maVaiTro").asString()).matches("^BQL[0-9]{8}$");

		Map<String, Object> dong = this.jdbc.queryForMap(
				"SELECT mat_khau_hash, phai_doi_mat_khau, so_lan_dang_nhap_sai FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				ma);
		assertThat((String) dong.get("mat_khau_hash")).startsWith("{bcrypt}");
		assertThat(this.passwordEncoder.matches(matKhauTam, (String) dong.get("mat_khau_hash"))).isTrue();
		assertThat(dong.get("phai_doi_mat_khau")).isEqualTo(true);
		assertThat(dong.get("so_lan_dang_nhap_sai")).isEqualTo(0);

		// Mật khẩu tạm không bao giờ hiện lại ở chi tiết
		this.mockMvc.perform(TaiKhoanApi.chiTiet(this.admin, ma))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.matKhauTam").doesNotExist());

		// Một dòng nhật ký, chỉ có mã vai trò
		List<Map<String, Object>> log = this.nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma);
		assertThat(log).hasSize(1);
		assertThat(log.get(0).get("loai_thao_tac")).isEqualTo("AUTH_TAO_TAI_KHOAN");
		assertThat(NhatKyDb.chiTiet(log.get(0)).toString()).isEqualTo("{\"vaiTro\":[\"CU_DAN\",\"BAN_QUAN_LY\"]}");
		String toanBo = this.nhatKy.toanBoSau(maLogTruoc);
		assertThat(toanBo).doesNotContain(matKhauTam, cccd, so, "an." + so);

		// Tài khoản mới đứng đầu danh sách
		this.mockMvc.perform(TaiKhoanApi.danhSach(this.admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Tra cứu tài khoản thành công"))
			.andExpect(jsonPath("$.data.items[0].maNguoiDung").value(ma))
			.andExpect(jsonPath("$.data.items[0].vaiTro[0]").value("BAN_QUAN_LY"))
			.andExpect(jsonPath("$.data.items[0].vaiTro[1]").value("CU_DAN"))
			.andExpect(jsonPath("$.data.items[0].soCccd").doesNotExist());
	}

	@Test
	void duLieuSaiTraLoiTheoTungTruongDungThuTu() throws Exception {
		Map<String, Object> body = TaiKhoanApi.body("   ", "12345", "a@b",
				List.of(TaiKhoanApi.vaiTro("KE_TOAN", "phamViPhuTrach", "x".repeat(151), "soCccd", "123"),
						TaiKhoanApi.vaiTro("KHONG_CO"), TaiKhoanApi.vaiTro("KE_TOAN", "phamViPhuTrach", "A"),
						TaiKhoanApi.vaiTro("CU_DAN", "soCccd", "12ab")));
		this.mockMvc.perform(TaiKhoanApi.tao(this.admin, body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("hoTen"))
			.andExpect(jsonPath("$.errors[0].message").value("Vui lòng nhập họ tên"))
			.andExpect(jsonPath("$.errors[1].field").value("soDienThoai"))
			.andExpect(jsonPath("$.errors[1].message").value("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0"))
			.andExpect(jsonPath("$.errors[2].field").value("email"))
			.andExpect(jsonPath("$.errors[2].message").value("Email không hợp lệ"))
			.andExpect(jsonPath("$.errors[3].field").value("vaiTro[0].phamViPhuTrach"))
			.andExpect(jsonPath("$.errors[3].message").value("Phạm vi phụ trách tối đa 150 ký tự"))
			.andExpect(jsonPath("$.errors[4].field").value("vaiTro[0].soCccd"))
			.andExpect(jsonPath("$.errors[4].message").value("Trường này không dùng cho vai trò đã chọn"))
			.andExpect(jsonPath("$.errors[5].field").value("vaiTro[1].vaiTro"))
			.andExpect(jsonPath("$.errors[5].message").value("Vai trò không tồn tại"))
			.andExpect(jsonPath("$.errors[6].field").value("vaiTro[2].vaiTro"))
			.andExpect(jsonPath("$.errors[6].message").value("Mỗi vai trò chỉ chọn một lần"))
			.andExpect(jsonPath("$.errors[7].field").value("vaiTro[3].soCccd"))
			.andExpect(jsonPath("$.errors[7].message").value("Số CCCD phải gồm 12 chữ số"))
			.andExpect(jsonPath("$.errors.length()").value(8));

		this.mockMvc.perform(TaiKhoanApi.tao(this.admin, TaiKhoanApi.body("A", TaiKhoanApi.soMoi(), "", List.of())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("email"))
			.andExpect(jsonPath("$.errors[0].message").value("Vui lòng nhập email"))
			.andExpect(jsonPath("$.errors[1].field").value("vaiTro"))
			.andExpect(jsonPath("$.errors[1].message").value("Vui lòng chọn ít nhất một vai trò"));

		this.mockMvc
			.perform(TaiKhoanApi.tao(this.admin,
					TaiKhoanApi.body("A", TaiKhoanApi.soMoi(), "x@chungcu.test",
							List.of(TaiKhoanApi.vaiTro("BAN_QUAN_LY", "chucVu", " ")))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("vaiTro[0].chucVu"))
			.andExpect(jsonPath("$.errors[0].message").value("Vui lòng nhập chức vụ"))
			.andExpect(jsonPath("$.errors[1].field").value("vaiTro[0].boPhan"))
			.andExpect(jsonPath("$.errors[1].message").value("Vui lòng nhập bộ phận"));
	}

	@Test
	void trungThongTinBaoTheoTungTruong() throws Exception {
		Map<String, Object> dau = TaiKhoanApi.cuDanMoi();
		this.mockMvc.perform(TaiKhoanApi.tao(this.admin, dau)).andExpect(status().isCreated());
		@SuppressWarnings("unchecked")
		String cccd = (String) ((List<Map<String, Object>>) dau.get("vaiTro")).get(0).get("soCccd");

		Map<String, Object> trung = TaiKhoanApi.body("Người Khác", (String) dau.get("soDienThoai"),
				(String) dau.get("email"), List.of(TaiKhoanApi.vaiTro("CU_DAN", "soCccd", cccd)));
		this.mockMvc.perform(TaiKhoanApi.tao(this.admin, trung))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("AUTH_IDENTITY_TAKEN"))
			.andExpect(jsonPath("$.message").value("Thông tin đã được dùng cho tài khoản khác"))
			.andExpect(jsonPath("$.errors[0].field").value("soDienThoai"))
			.andExpect(jsonPath("$.errors[0].message").value("Số điện thoại đã được dùng cho tài khoản khác"))
			.andExpect(jsonPath("$.errors[1].field").value("email"))
			.andExpect(jsonPath("$.errors[2].field").value("soCccd"))
			.andExpect(jsonPath("$.errors[2].message").value("Số CCCD đã được dùng cho cư dân khác"));
	}

	@Test
	void haiRequestCungSoDienThoaiChiMotBenThanhCong() throws Exception {
		Map<String, Object> a = TaiKhoanApi.cuDanMoi();
		Map<String, Object> b = TaiKhoanApi.cuDanMoi();
		b.put("soDienThoai", a.get("soDienThoai"));
		List<MvcResult> ketQua = AuthApi.songSong(this.mockMvc, TaiKhoanApi.tao(this.admin, a),
				TaiKhoanApi.tao(this.admin, b));
		List<Integer> ma = ketQua.stream().map((r) -> r.getResponse().getStatus()).sorted().toList();
		assertThat(ma).containsExactly(201, 409);
		MvcResult thua = ketQua.stream().filter((r) -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
		JsonNode body = JSON.readTree(thua.getResponse().getContentAsString());
		assertThat(body.get("errorCode").asString()).isEqualTo("AUTH_IDENTITY_TAKEN");
		assertThat(body.get("errors").get(0).get("field").asString()).isEqualTo("soDienThoai");
	}

	@Test
	void bqlChiTaoDuocTaiKhoanCuDan() throws Exception {
		String bql = this.auth.accessToken(TaiKhoanApi.BQL);
		this.mockMvc.perform(TaiKhoanApi.tao(bql, TaiKhoanApi.cuDanMoi())).andExpect(status().isCreated());

		Map<String, Object> keToan = TaiKhoanApi.cuDanMoi();
		keToan.put("vaiTro", List.of(TaiKhoanApi.vaiTro("KE_TOAN", "phamViPhuTrach", "Tòa A")));
		this.mockMvc.perform(TaiKhoanApi.tao(bql, keToan))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

		Map<String, Object> haiVaiTro = TaiKhoanApi.cuDanMoi();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> vt = (List<Map<String, Object>>) haiVaiTro.get("vaiTro");
		vt.add(TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "Cổng chính"));
		this.mockMvc.perform(TaiKhoanApi.tao(bql, haiVaiTro)).andExpect(status().isForbidden());
	}

	@Test
	void vaiTroKhacBiChan() throws Exception {
		String kyThuat = this.auth.accessToken(TaiKhoanApi.KY_THUAT);
		this.mockMvc.perform(TaiKhoanApi.tao(kyThuat, TaiKhoanApi.cuDanMoi()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		this.mockMvc.perform(TaiKhoanApi.danhSach(kyThuat)).andExpect(status().isForbidden());
	}

}
