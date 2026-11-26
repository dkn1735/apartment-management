package vn.edu.hust.chungcu.canho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.Normalizer;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * Tạm dừng và mở khóa quyền tiện ích căn hộ (spec 0013). Căn được đổi nằm ở tòa test, trừ
 * căn của cư dân seed {@code A-0201}, được trả về bình thường sau mỗi test.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class TamDungTienIchIntegrationTest {

	private static final String CAN_CU_DAN = "A-0201";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MutableClock clock;

	AuthApi auth;

	NhatKyDb nhatKy;

	String bql;

	String keToan;

	String ma;

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.bql = this.auth.accessToken(ToaNhaApi.BQL);
		this.keToan = this.auth.accessToken(ToaNhaApi.KE_TOAN);
		String toa = ToaNhaApi.maMoi();
		this.mockMvc.perform(ToaNhaApi.themToa(this.bql, ToaNhaApi.toaMoi(toa, 1))).andExpect(status().isCreated());
		this.ma = toa + "-0101";
		this.mockMvc.perform(CanHoApi.them(this.bql, CanHoApi.body(toa + "-01", 1, 60)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.lanDoiTienIch").value(nullValue()));
	}

	@AfterEach
	void tearDown() {
		this.jdbc.update("""
				UPDATE can_ho SET bi_tam_dung_tien_ich = FALSE, tien_ich_ly_do = NULL, tien_ich_doi_luc = NULL,
				       tien_ich_doi_boi = NULL WHERE ma_can_ho = ?""", CAN_CU_DAN);
		ToaNhaApi.donDep(this.jdbc);
		this.clock.reset();
	}

	// AC-1, AC-2, AC-7, AC-8: kế toán tạm dừng, BQL mở khóa, hai dòng nhật ký
	@Test
	void tamDungRoiMoKhoa() throws Exception {
		this.mockMvc.perform(CanHoApi.doiTienIch(this.keToan, this.ma.toLowerCase(), true, " Nợ phí quý 3 "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Tạm dừng quyền tiện ích căn hộ thành công"))
			.andExpect(jsonPath("$.data.maCanHo").value(this.ma))
			.andExpect(jsonPath("$.data.biTamDungTienIch").value(true))
			.andExpect(jsonPath("$.data.lanDoiTienIch.lyDo").value("Nợ phí quý 3"))
			.andExpect(jsonPath("$.data.lanDoiTienIch.doiLuc").value(MutableClock.MAC_DINH.toString()))
			.andExpect(jsonPath("$.data.lanDoiTienIch.maNguoiDoi").value(maNguoiDung(ToaNhaApi.KE_TOAN)))
			.andExpect(jsonPath("$.data.lanDoiTienIch.hoTenNguoiDoi").value("Phạm Kế Toán"));

		Instant sau = MutableClock.MAC_DINH.plusSeconds(60);
		this.clock.set(sau);
		this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, this.ma, false, "Đã thanh toán"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Mở khóa quyền tiện ích căn hộ thành công"))
			.andExpect(jsonPath("$.data.biTamDungTienIch").value(false))
			.andExpect(jsonPath("$.data.lanDoiTienIch.lyDo").value("Đã thanh toán"))
			.andExpect(jsonPath("$.data.lanDoiTienIch.doiLuc").value(sau.toString()))
			.andExpect(jsonPath("$.data.lanDoiTienIch.hoTenNguoiDoi").value("Trần Ban Quản Lý"));

		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong("CanHo:" + this.ma);
		assertThat(dong).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("CANHO_THEM_CAN_HO", "CANHO_TAM_DUNG_TIEN_ICH", "CANHO_MO_KHOA_TIEN_ICH");
		assertThat(NhatKyDb.chiTiet(dong.get(1)).get("lyDo").asString()).isEqualTo("Nợ phí quý 3");
		assertThat(NhatKyDb.chiTiet(dong.get(2)).get("lyDo").asString()).isEqualTo("Đã thanh toán");
		assertThat(dong.get(1).get("ma_nguoi_dung")).isEqualTo(maNguoiDung(ToaNhaApi.KE_TOAN));

		// AC-8: PUT trả cùng lần đổi kèm họ tên
		this.mockMvc.perform(CanHoApi.sua(this.bql, this.ma, 61, "Trống"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.lanDoiTienIch.lyDo").value("Đã thanh toán"))
			.andExpect(jsonPath("$.data.lanDoiTienIch.hoTenNguoiDoi").value("Trần Ban Quản Lý"));
	}

	// AC-5: trùng trạng thái thì 409, không đổi gì
	@Test
	void trungTrangThaiThi409() throws Exception {
		this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, this.ma, false, "Thử"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("CANHO_TIEN_ICH_KHONG_DOI"))
			.andExpect(jsonPath("$.message").value("Căn hộ đang dùng tiện ích bình thường"))
			.andExpect(jsonPath("$.errors").isEmpty());
		this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, this.ma, true, "Lần đầu")).andExpect(status().isOk());
		this.mockMvc.perform(CanHoApi.doiTienIch(this.keToan, this.ma, true, "Lần hai"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("Căn hộ đang bị tạm dừng tiện ích"));
		assertThat(this.jdbc.queryForObject("SELECT tien_ich_ly_do FROM can_ho WHERE ma_can_ho = ?", String.class,
				this.ma))
			.isEqualTo("Lần đầu");
		assertThat(this.nhatKy.theoDoiTuong("CanHo:" + this.ma)).hasSize(2);
	}

	// AC-3, AC-4: mục lỗi theo thứ tự; 400 trước 404; 404 ngoài phạm vi
	@Test
	void duLieuSaiThi400() throws Exception {
		this.mockMvc.perform(CanHoApi.doiTienIchRaw(this.bql, this.ma, "{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("biTamDungTienIch", "lyDo")))
			.andExpect(jsonPath("$.errors[*].message")
				.value(contains("Vui lòng chọn tạm dừng hoặc mở khóa", "Vui lòng nhập lý do")));
		for (String body : List.of("{\"biTamDungTienIch\": \"true\", \"lyDo\": \"x\"}",
				"{\"biTamDungTienIch\": 1, \"lyDo\": \"x\"}", "{\"biTamDungTienIch\": null, \"lyDo\": \"x\"}")) {
			this.mockMvc.perform(CanHoApi.doiTienIchRaw(this.bql, this.ma, body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("biTamDungTienIch")));
		}
		for (String lyDo : List.of("null", "\"   \"", "5")) {
			this.mockMvc
				.perform(CanHoApi.doiTienIchRaw(this.bql, this.ma,
						"{\"biTamDungTienIch\": true, \"lyDo\": " + lyDo + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].message").value(contains("Vui lòng nhập lý do")));
		}
		this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, this.ma, true, "x".repeat(501)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].message").value(contains("Lý do tối đa 500 ký tự")));
		this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, "ZZ-9999", null, "x")).andExpect(status().isBadRequest());
		for (String sai : List.of("ZZ-9999", "KHONGCO")) {
			this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, sai, true, "x"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Không tìm thấy dữ liệu yêu cầu"));
		}
		// 500 emoji là 500 code point, hợp lệ
		this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, this.ma, true, "😀".repeat(500))).andExpect(status().isOk());
	}

	// AC-13: chỉ BQL và kế toán được đổi
	@Test
	void vaiTroKhacThi403() throws Exception {
		for (String so : List.of(ToaNhaApi.BQT, ToaNhaApi.KY_THUAT, ToaNhaApi.CU_DAN)) {
			this.mockMvc.perform(CanHoApi.doiTienIch(this.auth.accessToken(so), this.ma, true, "x"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		}
		this.mockMvc.perform(CanHoApi.doiTienIch("khong-hop-le", this.ma, true, "x"))
			.andExpect(status().isUnauthorized());
	}

	// AC-9: cư dân thấy lý do và thời điểm, không thấy người đổi; BQL thấy đủ
	@Test
	void cuDanKhongThayNguoiDoi() throws Exception {
		this.mockMvc.perform(CanHoApi.doiTienIch(this.bql, CAN_CU_DAN, true, "Nợ phí")).andExpect(status().isOk());
		this.mockMvc.perform(CanHoApi.chiTiet(this.auth.accessToken(ToaNhaApi.CU_DAN), CAN_CU_DAN))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.biTamDungTienIch").value(true))
			.andExpect(jsonPath("$.data.lanDoiTienIch.lyDo").value("Nợ phí"))
			.andExpect(jsonPath("$.data.lanDoiTienIch.doiLuc").value(MutableClock.MAC_DINH.toString()))
			.andExpect(jsonPath("$.data.lanDoiTienIch.maNguoiDoi").value(nullValue()))
			.andExpect(jsonPath("$.data.lanDoiTienIch.hoTenNguoiDoi").value(nullValue()));
		this.mockMvc.perform(CanHoApi.chiTiet(this.bql, CAN_CU_DAN))
			.andExpect(jsonPath("$.data.lanDoiTienIch.hoTenNguoiDoi").value("Trần Ban Quản Lý"));
	}

	// AC-6: hai yêu cầu cùng chiều song song, đúng một 200 và một 409
	@Test
	void haiYeuCauSongSong() throws Exception {
		List<Integer> trangThai = songSong(CanHoApi.doiTienIch(this.bql, this.ma, true, "Một"),
				CanHoApi.doiTienIch(this.keToan, this.ma, true, "Hai"));
		assertThat(trangThai).containsExactlyInAnyOrder(200, 409);
		assertThat(this.nhatKy.theoDoiTuong("CanHo:" + this.ma)).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("CANHO_THEM_CAN_HO", "CANHO_TAM_DUNG_TIEN_ICH");
	}

	// covers: AC-6 (hai yêu cầu ngược chiều song song xử lý lần lượt; nhật ký khớp thứ
	// tự)
	@Test
	void haiYeuCauNguocChieuSongSong() throws Exception {
		List<Integer> trangThai = songSong(CanHoApi.doiTienIch(this.bql, this.ma, true, "Tạm dừng"),
				CanHoApi.doiTienIch(this.keToan, this.ma, false, "Mở khóa"));
		List<Object> loai = this.nhatKy.theoDoiTuong("CanHo:" + this.ma)
			.stream()
			.map(d -> d.get("loai_thao_tac"))
			.filter(l -> !"CANHO_THEM_CAN_HO".equals(l))
			.toList();
		Boolean coCuoi = this.jdbc.queryForObject("SELECT bi_tam_dung_tien_ich FROM can_ho WHERE ma_can_ho = ?",
				Boolean.class, this.ma);
		if (trangThai.equals(List.of(200, 200))) {
			// tạm dừng chạy trước rồi mở khóa
			assertThat(loai).containsExactly("CANHO_TAM_DUNG_TIEN_ICH", "CANHO_MO_KHOA_TIEN_ICH");
			assertThat(coCuoi).isFalse();
		}
		else {
			// mở khóa chạy trước gặp căn đang bình thường: 409, rồi tạm dừng 200
			assertThat(trangThai).containsExactly(200, 409);
			assertThat(loai).containsExactly("CANHO_TAM_DUNG_TIEN_ICH");
			assertThat(coCuoi).isTrue();
		}
	}

	// covers: AC-1 (trường khác trong body bị bỏ qua), AC-2 (lý do lưu NFC đã cắt, cột
	// khác không đổi, người đổi là người gọi)
	@Test
	void truongLaBiBoQuaLyDoLuuNfcCotKhacGiuNguyen() throws Exception {
		String chonCot = "SELECT ma_tang, dien_tich, trang_thai_su_dung FROM can_ho WHERE ma_can_ho = ?";
		Map<String, Object> truoc = this.jdbc.queryForMap(chonCot, this.ma);
		String nfd = Normalizer.normalize(" Nợ phí ", Normalizer.Form.NFD);
		String body = JsonMapper.builder()
			.build()
			.writeValueAsString(Map.of("biTamDungTienIch", true, "lyDo", nfd, "trangThaiSuDung", "Đã bàn giao",
					"dienTich", 999, "maNguoiDoi", "ND99999999", "tienIchDoiLuc", "2000-01-01T00:00:00Z"));
		this.mockMvc.perform(CanHoApi.doiTienIchRaw(this.keToan, this.ma, body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.lanDoiTienIch.lyDo").value("Nợ phí"))
			.andExpect(jsonPath("$.data.lanDoiTienIch.doiLuc").value(MutableClock.MAC_DINH.toString()));

		assertThat(this.jdbc.queryForMap(chonCot, this.ma)).isEqualTo(truoc);
		Map<String, Object> tienIch = this.jdbc
			.queryForMap("SELECT tien_ich_ly_do, tien_ich_doi_boi FROM can_ho WHERE ma_can_ho = ?", this.ma);
		String luu = (String) tienIch.get("tien_ich_ly_do");
		assertThat(luu).isEqualTo(Normalizer.normalize("Nợ phí", Normalizer.Form.NFC));
		assertThat(Normalizer.isNormalized(luu, Normalizer.Form.NFC)).isTrue();
		assertThat(tienIch.get("tien_ich_doi_boi")).isEqualTo(maNguoiDung(ToaNhaApi.KE_TOAN));
	}

	// AC-10: CHECK của V17 chặn cả khi sửa tay
	@Test
	void checkChanKhiSuaTay() {
		String nguoi = maNguoiDung(ToaNhaApi.BQL);
		for (String sql : List.of("UPDATE can_ho SET bi_tam_dung_tien_ich = TRUE WHERE ma_can_ho = ?",
				"UPDATE can_ho SET tien_ich_ly_do = 'x' WHERE ma_can_ho = ?",
				"UPDATE can_ho SET tien_ich_ly_do = '  ', tien_ich_doi_luc = now(), tien_ich_doi_boi = '" + nguoi
						+ "' WHERE ma_can_ho = ?",
				"UPDATE can_ho SET tien_ich_ly_do = 'x', tien_ich_doi_luc = now(), tien_ich_doi_boi = 'ND99999999'"
						+ " WHERE ma_can_ho = ?")) {
			assertThatThrownBy(() -> this.jdbc.update(sql, this.ma)).as(sql)
				.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	// AC-11, AC-12: lọc trên seed, A-0204 bị tạm dừng bởi BQL seed
	@Test
	void locTheoTienIch() throws Exception {
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A").param("biTamDungTienIch", " true "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].maCanHo").value(contains("A-0204")))
			.andExpect(
					jsonPath("$.data.items[0].lanDoiTienIch.lyDo").value("Nợ phí dịch vụ quá hạn 2 kỳ (dữ liệu mẫu)"))
			.andExpect(jsonPath("$.data.items[0].lanDoiTienIch.doiLuc").value("2026-11-01T02:00:00Z"))
			.andExpect(jsonPath("$.data.items[0].lanDoiTienIch.hoTenNguoiDoi").value("Trần Ban Quản Lý"));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&biTamDungTienIch=false"))
			.andExpect(jsonPath("$.data.totalItems").value(11));
		this.mockMvc.perform(CanHoApi.tim(this.bql, "?maToaNha=A&biTamDungTienIch="))
			.andExpect(jsonPath("$.data.totalItems").value(12));
		for (String sai : List.of("TRUE", "1")) {
			this.mockMvc.perform(CanHoApi.tim(this.bql, "?q=" + "x".repeat(21) + "&biTamDungTienIch=" + sai))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("q", "biTamDungTienIch")))
				.andExpect(jsonPath("$.errors[1].message").value("Giá trị lọc tiện ích không hợp lệ"));
		}
	}

	private String maNguoiDung(String soDienThoai) {
		return this.jdbc.queryForObject("SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?",
				String.class, soDienThoai);
	}

	private List<Integer> songSong(MockHttpServletRequestBuilder... request) throws Exception {
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
