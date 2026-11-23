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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/** Tìm kiếm danh sách và giữ bí mật trong nhật ký (spec 0008, AC-5, AC-16). */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class TaiKhoanTimKiemIntegrationTest {

	static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	AuthApi auth;

	String admin;

	@BeforeEach
	void setUp() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.admin = this.auth.accessToken(TaiKhoanApi.ADMIN);
	}

	JsonNode tao(String hoTen, String so, String email, Map<String, Object> vaiTro) throws Exception {
		MvcResult r = this.mockMvc
			.perform(TaiKhoanApi.tao(this.admin, TaiKhoanApi.body(hoTen, so, email, new ArrayList<>(List.of(vaiTro)))))
			.andExpect(status().isCreated())
			.andReturn();
		return JSON.readTree(r.getResponse().getContentAsString()).get("data");
	}

	List<String> tim(String token, String... thamSo) throws Exception {
		var req = TaiKhoanApi.danhSach(token).param("size", "100");
		for (int i = 0; i < thamSo.length; i += 2) {
			req = req.param(thamSo[i], thamSo[i + 1]);
		}
		MvcResult r = this.mockMvc.perform(req).andExpect(status().isOk()).andReturn();
		List<String> ma = new ArrayList<>();
		JSON.readTree(r.getResponse().getContentAsString())
			.get("data")
			.get("items")
			.forEach((it) -> ma.add(it.get("maNguoiDung").asString()));
		return ma;
	}

	@Test
	void timKhongDauTheoSoEmailMaVaThoatKyTuDacBiet() throws Exception {
		String so = TaiKhoanApi.soMoi();
		String duoi = so.substring(5);
		String ma = tao("Nguyễn Văn Đức Tìm", so, "duc.tim_" + duoi + "@chungcu.test",
				TaiKhoanApi.vaiTro("BAO_VE", "viTriTruc", "Cổng"))
			.get("taiKhoan")
			.get("maNguoiDung")
			.asString();

		assertThat(tim(this.admin, "q", "nguyen van duc tim")).contains(ma);
		assertThat(tim(this.admin, "q", "ĐỨC TÌM")).contains(ma);
		assertThat(tim(this.admin, "q", "+84 " + so.substring(1, 4) + "." + so.substring(4))).containsExactly(ma);
		assertThat(tim(this.admin, "q", "tim_" + duoi)).containsExactly(ma);
		assertThat(tim(this.admin, "q", ma.toLowerCase())).containsExactly(ma);
		// % và _ được so như chữ thường, không phải ký tự đại diện
		assertThat(tim(this.admin, "q", "%")).doesNotContain(ma);
		assertThat(tim(this.admin, "q", "tim%" + duoi)).isEmpty();
		// Kết hợp bằng AND
		assertThat(tim(this.admin, "q", "duc tim", "vaiTro", "BAO_VE", "trangThai", "Hoạt động")).contains(ma);
		assertThat(tim(this.admin, "q", "duc tim", "vaiTro", "KE_TOAN")).doesNotContain(ma);
		assertThat(tim(this.admin, "q", "duc tim", "trangThai", "Tạm khóa")).doesNotContain(ma);
		// Mã seed theo thứ tự mới tạo trước
		List<String> tatCa = tim(this.admin);
		assertThat(tatCa.indexOf(TaiKhoanApi.maTheoSo(this.jdbc, "0900000008")))
			.isLessThan(tatCa.indexOf(TaiKhoanApi.maTheoSo(this.jdbc, "0900000001")));
	}

	@Test
	void thamSoSaiTraLoiTheoThuTu() throws Exception {
		this.mockMvc
			.perform(TaiKhoanApi.danhSach(this.admin)
				.param("page", "0")
				.param("sort", "hoTen,asc")
				.param("q", "x".repeat(101))
				.param("vaiTro", "ADMIN")
				.param("trangThai", "Khóa"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("page"))
			.andExpect(jsonPath("$.errors[1].field").value("sort"))
			.andExpect(jsonPath("$.errors[2].field").value("q"))
			.andExpect(jsonPath("$.errors[2].message").value("Từ khóa tối đa 100 ký tự"))
			.andExpect(jsonPath("$.errors[3].field").value("vaiTro"))
			.andExpect(jsonPath("$.errors[3].message").value("Vai trò không tồn tại"))
			.andExpect(jsonPath("$.errors[4].field").value("trangThai"))
			.andExpect(jsonPath("$.errors[4].message").value("Trạng thái không tồn tại"));
		// Trang vượt quá: 200, rỗng, tổng đúng
		this.mockMvc.perform(TaiKhoanApi.danhSach(this.admin).param("page", "9999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items.length()").value(0))
			.andExpect(jsonPath("$.data.totalItems").isNumber());
	}

	@Test
	void bqlLocVaiTroKhacCuDanThiRong() throws Exception {
		String bql = this.auth.accessToken(TaiKhoanApi.BQL);
		assertThat(tim(bql, "vaiTro", "KE_TOAN")).isEmpty();
		assertThat(tim(bql, "vaiTro", "CU_DAN")).isNotEmpty();
	}

	@Test
	void nhatKyCuaMoiThaoTacKhongChuaSoDienThoaiEmailCccdHayMatKhauTam() throws Exception {
		NhatKyDb nhatKy = new NhatKyDb(this.jdbc);
		String maLogTruoc = nhatKy.maLogCuoi();
		String so = TaiKhoanApi.soMoi();
		String email = "bimat" + so + "@chungcu.test";
		String cccd = TaiKhoanApi.cccdMoi();
		JsonNode tao = tao("Người Bí Mật", so, email, TaiKhoanApi.vaiTro("CU_DAN", "soCccd", cccd));
		String ma = tao.get("taiKhoan").get("maNguoiDung").asString();
		List<String> biMat = new ArrayList<>(List.of(so, email, cccd, tao.get("matKhauTam").asString()));

		String soMoi = TaiKhoanApi.soMoi();
		String emailMoi = "moi" + soMoi + "@chungcu.test";
		Map<String, Object> sua = new LinkedHashMap<>();
		sua.put("hoTen", "Người Bí Mật");
		sua.put("soDienThoai", soMoi);
		sua.put("email", emailMoi);
		sua.put("phienBan", 0);
		this.mockMvc.perform(TaiKhoanApi.sua(this.admin, ma, sua)).andExpect(status().isOk());
		String cccdMoi = TaiKhoanApi.cccdMoi();
		Map<String, Object> vt = new LinkedHashMap<>();
		vt.put("phienBan", 1);
		vt.put("soCccd", cccdMoi);
		this.mockMvc.perform(TaiKhoanApi.suaVaiTro(this.admin, ma, "CU_DAN", vt)).andExpect(status().isOk());
		Map<String, Object> them = new LinkedHashMap<>();
		them.put("phienBan", 2);
		them.put("viTriTruc", "Cổng");
		this.mockMvc.perform(TaiKhoanApi.suaVaiTro(this.admin, ma, "BAO_VE", them)).andExpect(status().isOk());
		this.mockMvc.perform(TaiKhoanApi.goVaiTro(this.admin, ma, "BAO_VE", 3)).andExpect(status().isOk());
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma, "Tạm khóa", "Thử")).andExpect(status().isOk());
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, ma, "Hoạt động", null)).andExpect(status().isOk());
		MvcResult datLai = this.mockMvc.perform(TaiKhoanApi.datLai(this.admin, ma))
			.andExpect(status().isOk())
			.andReturn();
		biMat.addAll(List.of(soMoi, emailMoi, cccdMoi,
				JSON.readTree(datLai.getResponse().getContentAsString()).get("data").get("matKhauTam").asString()));

		assertThat(nhatKy.theoDoiTuong("NguoiDungHeThong:" + ma)).extracting((d) -> d.get("loai_thao_tac"))
			.containsExactly("AUTH_TAO_TAI_KHOAN", "AUTH_CAP_NHAT_TAI_KHOAN", "AUTH_CAP_NHAT_VAI_TRO",
					"AUTH_GAN_VAI_TRO", "AUTH_GO_VAI_TRO", "AUTH_KHOA_TAI_KHOAN", "AUTH_MO_KHOA_TAI_KHOAN",
					"AUTH_DAT_LAI_MAT_KHAU");
		String toanBo = nhatKy.toanBoSau(maLogTruoc);
		for (String s : biMat) {
			assertThat(toanBo).doesNotContain(s);
		}
	}

}
