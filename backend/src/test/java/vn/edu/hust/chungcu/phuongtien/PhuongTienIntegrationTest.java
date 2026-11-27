package vn.edu.hust.chungcu.phuongtien;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.AuthApi;
import vn.edu.hust.chungcu.auth.KhoaTamTestSupport;
import vn.edu.hust.chungcu.nhatky.NhatKyDb;

/**
 * API phương tiện gửi xe (spec 0016, AC-2 đến AC-12). Tòa thử có mã bắt đầu bằng
 * {@code T}; cư dân seed {@code 0900000007} (cư dân thuần) và {@code 0900000006} (cư dân
 * kiêm BQT) được cho ở căn thử. Dọn xong thì dữ liệu seed giữ nguyên.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class PhuongTienIntegrationTest {

	private static final String BQL = "0900000002";

	private static final String KE_TOAN = "0900000004";

	private static final String BQT_CU_DAN = "0900000006";

	private static final String CU_DAN = "0900000007";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final AtomicInteger DEM = new AtomicInteger();

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	DataSource dataSource;

	@Autowired
	MutableClock clock;

	AuthApi auth;

	NhatKyDb nhatKy;

	String bql;

	String ma;

	String toi;

	String hangXom;

	String tokenToi;

	@BeforeEach
	void setUp() throws Exception {
		this.clock.reset();
		KhoaTamTestSupport.datLai(this.jdbc);
		this.auth = new AuthApi(this.mockMvc);
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.bql = this.auth.accessToken(BQL);
		this.ma = taoCan();
		this.toi = choO(CU_DAN, this.ma);
		this.hangXom = choO(BQT_CU_DAN, this.ma);
		this.tokenToi = this.auth.accessToken(CU_DAN);
	}

	@AfterEach
	void tearDown() {
		this.jdbc.update("DELETE FROM phuong_tien WHERE ma_can_ho LIKE 'T%'");
		this.jdbc.update("DELETE FROM ho_so_cu_tru WHERE ma_can_ho LIKE 'T%'");
		this.jdbc.update("DELETE FROM can_ho WHERE ma_tang LIKE 'T%'");
		this.jdbc.update("DELETE FROM tang WHERE ma_toa_nha LIKE 'T%'");
		this.jdbc.update("DELETE FROM toa_nha WHERE ma_toa_nha LIKE 'T%'");
		this.jdbc.update(
				"UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = 'Hoạt động' " + "WHERE so_dien_thoai = ?",
				CU_DAN);
		this.clock.reset();
	}

	// AC-2, AC-3, AC-5, AC-10, AC-12
	@Test
	void dangKyDuyetVaXemTheoGocNhin() throws Exception {
		String bien = bienMoi();
		String coGach = bien.substring(0, 3).toLowerCase() + "-" + bien.substring(3);
		MvcResult kq = dangKy(this.bql, this.ma, this.toi, "Ô tô", coGach).andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("Đăng ký phương tiện gửi xe thành công"))
			.andExpect(jsonPath("$.data.bienSo").value(bien))
			.andExpect(jsonPath("$.data.trangThaiDangKy").value("Đang hoạt động"))
			.andExpect(jsonPath("$.data.maCuDanDangKy").value(nullValue()))
			.andExpect(jsonPath("$.data.hoTenNguoiCapNhat").value("Trần Ban Quản Lý"))
			.andExpect(jsonPath("$.data.chuXeConCuTru").value(true))
			.andExpect(jsonPath("$.data.coTheRutDon").value(false))
			.andReturn();
		String oTo = data(kq, "maPhuongTien");
		String xeMay = data(
				dangKy(this.tokenToi, this.ma, this.hangXom, "Xe máy", bienMoi()).andExpect(status().isCreated())
					.andExpect(jsonPath("$.data.trangThaiDangKy").value("Chờ duyệt"))
					.andExpect(jsonPath("$.data.maCuDanDangKy").value(this.toi))
					.andExpect(jsonPath("$.data.maNvbqlCapNhat").value(nullValue()))
					.andExpect(jsonPath("$.data.coTheRutDon").value(true))
					.andReturn(),
				"maPhuongTien");
		xem(this.tokenToi, this.ma).andExpect(jsonPath("$.data.dangGui[*].maPhuongTien").value(contains(oTo)))
			.andExpect(jsonPath("$.data.dangGui[0].hoTenNguoiCapNhat").value(nullValue()))
			.andExpect(jsonPath("$.data.donVaLichSu[*].maPhuongTien").value(contains(xeMay)));
		doi(this.bql, xeMay, "Đang hoạt động", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Cập nhật trạng thái thẻ xe/phương tiện thành công"))
			.andExpect(jsonPath("$.data.trangThaiDangKy").value("Đang hoạt động"));
		MvcResult ds = xem(this.auth.accessToken(KE_TOAN), this.ma).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.dangGui[*].maPhuongTien").value(contains(oTo, xeMay)))
			.andExpect(jsonPath("$.data.donVaLichSu").isEmpty())
			.andReturn();
		assertThat(ds.getResponse().getContentAsString()).doesNotContain("@chungcu.test").doesNotContain(CU_DAN);
		assertThat(this.nhatKy.theoDoiTuong("PhuongTien:" + xeMay)).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("PHUONGTIEN_DANG_KY", "PHUONGTIEN_DOI_TRANG_THAI");
		JsonNode chiTiet = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("PhuongTien:" + oTo).get(0));
		assertThat(chiTiet.propertyNames()).containsExactlyInAnyOrder("maCanHo", "maCuDanSoHuu", "loaiPhuongTien",
				"bienSo", "trangThai");
		assertThat(chiTiet.get("trangThai").get("cu").isNull()).isTrue();
	}

	// AC-6, AC-7, AC-10: trùng biển và đăng ký lại biển đã hủy
	@Test
	void trungBienSoVaDungLaiBienDaHuy() throws Exception {
		String khac = taoCan();
		String chuKhac = choO(CU_DAN, khac);
		String bien = bienMoi();
		String dau = data(dangKy(this.bql, this.ma, this.toi, "Ô tô", bien).andExpect(status().isCreated()).andReturn(),
				"maPhuongTien");
		dangKy(this.bql, khac, chuKhac, "Ô tô", bien).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_TRUNG_BIEN_SO"))
			.andExpect(jsonPath("$.errors[*].field").value(contains("bienSo")))
			.andExpect(jsonPath("$.errors[0].message")
				.value("Biển số đang được giữ bởi phương tiện " + dau + " của căn hộ " + this.ma));
		dangKy(this.auth.accessToken(CU_DAN), khac, chuKhac, "Ô tô", bien).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors[0].message").value("Biển số này đang được đăng ký cho một phương tiện khác"));
		doi(this.bql, dau, "Đã hủy", "Đã bán xe").andExpect(status().isOk());
		dangKy(this.bql, khac, chuKhac, "Ô tô", bien).andExpect(status().isCreated());
		dangKy(this.bql, this.ma, this.toi, "Xe đạp", null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.bienSo").value(nullValue()));
		dangKy(this.bql, this.ma, this.toi, "Xe máy", null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Vui lòng nhập biển số"));
	}

	// AC-7, AC-10: đơn chờ không giữ chỗ, duyệt đơn sau bị chặn
	@Test
	void donChoCungBienChiMotDonDuocDuyet() throws Exception {
		String khac = taoCan();
		choO(CU_DAN, khac);
		String bien = bienMoi();
		String token = this.auth.accessToken(CU_DAN);
		String mot = data(dangKy(token, this.ma, this.toi, "Xe máy", bien).andExpect(status().isCreated()).andReturn(),
				"maPhuongTien");
		String hai = data(dangKy(token, khac, this.toi, "Xe máy", bien).andExpect(status().isCreated()).andReturn(),
				"maPhuongTien");
		doi(this.bql, mot, "Đang hoạt động", null).andExpect(status().isOk());
		doi(this.bql, hai, "Đang hoạt động", null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_TRUNG_BIEN_SO"))
			.andExpect(jsonPath("$.errors[0].message")
				.value("Biển số đang được giữ bởi phương tiện " + mot + " của căn hộ " + this.ma));
	}

	// AC-8: hai request song song trên hai căn
	@Test
	void haiDangKySongSongCungBienChiMotThanhCong() throws Exception {
		String khac = taoCan();
		String chuKhac = choO(CU_DAN, khac);
		String bien = bienMoi();
		CountDownLatch batDau = new CountDownLatch(1);
		List<CompletableFuture<MvcResult>> chay = new ArrayList<>();
		for (String[] c : List.of(new String[] { this.ma, this.toi }, new String[] { khac, chuKhac })) {
			chay.add(CompletableFuture.supplyAsync(() -> {
				try {
					batDau.await();
					return dangKy(this.bql, c[0], c[1], "Ô tô", bien).andReturn();
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}));
		}
		batDau.countDown();
		List<MvcResult> ketQua = chay.stream().map(CompletableFuture::join).toList();
		assertThat(ketQua).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(201, 409);
		MvcResult thua = ketQua.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
		assertThat(thua.getResponse().getContentAsString()).contains("PHUONGTIEN_TRUNG_BIEN_SO");
	}

	// AC-8: bước kiểm lọt (dòng kia chưa commit) thì index chặn, đổi sang 409 câu chung
	@Test
	void indexChanKhiBuocKiemLot() throws Exception {
		String khac = taoCan();
		String bien = bienMoi();
		try (Connection ketNoi = this.dataSource.getConnection()) {
			ketNoi.setAutoCommit(false);
			try (PreparedStatement chen = ketNoi.prepareStatement("""
					INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, bien_so)
					VALUES (?, ?, 'Ô tô', ?)""")) {
				chen.setString(1, khac);
				chen.setString(2, this.toi);
				chen.setString(3, bien);
				chen.executeUpdate();
			}
			CompletableFuture<MvcResult> request = CompletableFuture.supplyAsync(() -> {
				try {
					return dangKy(this.bql, this.ma, this.toi, "Ô tô", bien).andReturn();
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			});
			choCoRequestBiChan();
			ketNoi.commit();
			MvcResult kq = request.get(30, TimeUnit.SECONDS);
			assertThat(kq.getResponse().getStatus()).isEqualTo(409);
			JsonNode body = JSON.readTree(kq.getResponse().getContentAsString());
			assertThat(body.get("errorCode").asString()).isEqualTo("PHUONGTIEN_TRUNG_BIEN_SO");
			assertThat(body.get("errors").get(0).get("message").asString())
				.isEqualTo("Biển số này đang được đăng ký cho một phương tiện khác");
		}
	}

	// AC-8: duyệt đơn chạy đua với một xe cùng biển chưa commit; index chặn, BQL cũng chỉ
	// nhận câu chung
	@Test
	void indexChanKhiDuyetDonChayDua() throws Exception {
		String khac = taoCan();
		String bien = bienMoi();
		String don = data(dangKy(this.tokenToi, this.ma, this.toi, "Ô tô", bien).andReturn(), "maPhuongTien");
		try (Connection ketNoi = this.dataSource.getConnection()) {
			ketNoi.setAutoCommit(false);
			try (PreparedStatement chen = ketNoi.prepareStatement("""
					INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, bien_so)
					VALUES (?, ?, 'Ô tô', ?)""")) {
				chen.setString(1, khac);
				chen.setString(2, this.toi);
				chen.setString(3, bien);
				chen.executeUpdate();
			}
			CompletableFuture<MvcResult> request = CompletableFuture.supplyAsync(() -> {
				try {
					return doi(this.bql, don, "Đang hoạt động", null).andReturn();
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			});
			choCoRequestBiChan();
			ketNoi.commit();
			MvcResult kq = request.get(30, TimeUnit.SECONDS);
			assertThat(kq.getResponse().getStatus()).isEqualTo(409);
			JsonNode body = JSON.readTree(kq.getResponse().getContentAsString());
			assertThat(body.get("errorCode").asString()).isEqualTo("PHUONGTIEN_TRUNG_BIEN_SO");
			assertThat(body.get("errors").get(0).get("message").asString())
				.isEqualTo("Biển số này đang được đăng ký cho một phương tiện khác");
		}
	}

	// AC-4, AC-7, AC-9, AC-10
	@Test
	void vongDoiVaKiemLaiChuXe() throws Exception {
		String xe = data(dangKy(this.bql, this.ma, this.toi, "Ô tô", bienMoi()).andReturn(), "maPhuongTien");
		doi(this.bql, xe, "Tạm dừng", null).andExpect(status().isBadRequest());
		doi(this.bql, xe, "Tạm dừng", "Nợ phí").andExpect(status().isOk());
		xem(this.auth.accessToken(BQT_CU_DAN), this.ma)
			.andExpect(jsonPath("$.data.dangGui[0].lyDoTrangThai").value("Nợ phí"));
		this.jdbc.update("UPDATE ho_so_cu_tru SET trang_thai_ho_so = 'Hết hiệu lực', ngay_ket_thuc = DATE '2026-02-01' "
				+ "WHERE ma_can_ho = ? AND ma_cu_dan = ?", this.ma, this.toi);
		xem(this.bql, this.ma).andExpect(jsonPath("$.data.dangGui[0].chuXeConCuTru").value(false))
			.andExpect(jsonPath("$.data.dangGui[0].trangThaiDangKy").value("Tạm dừng"));
		doi(this.bql, xe, "Đang hoạt động", null).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_CHU_XE_KHONG_CU_TRU"))
			.andExpect(jsonPath("$.errors").isEmpty());
		dangKy(this.bql, this.ma, this.toi, "Ô tô", bienMoi()).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors[*].field").value(contains("maCuDanSoHuu")));
		doi(this.bql, xe, "Đã hủy", "Chủ xe đã đi").andExpect(status().isOk());
		doi(this.bql, xe, "Đang hoạt động", null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE"));
		doi(this.bql, "PT99999999", "Đã hủy", "x").andExpect(status().isNotFound());
	}

	// AC-7: tài khoản ngừng hoạt động và trần 10 đơn chờ
	@Test
	void taiKhoanNgungVaTranMuoiDon() throws Exception {
		for (int i = 0; i < 10; i++) {
			dangKy(this.tokenToi, this.ma, this.toi, "Xe đạp", null).andExpect(status().isCreated());
		}
		dangKy(this.tokenToi, this.ma, this.toi, "Xe đạp", null).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_VUOT_SO_DON_CHO_DUYET"));
		dangKy(this.bql, this.ma, this.toi, "Xe đạp", null).andExpect(status().isCreated());
		this.jdbc.update(
				"UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = 'Ngừng hoạt động' " + "WHERE so_dien_thoai = ?",
				CU_DAN);
		dangKy(this.bql, this.ma, this.toi, "Xe đạp", null).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG"))
			.andExpect(jsonPath("$.errors").isEmpty());
	}

	// AC-3, AC-11, AC-12
	@Test
	void rutDonVaPhanQuyen() throws Exception {
		String don = data(dangKy(this.tokenToi, this.ma, this.toi, "Xe máy", bienMoi()).andReturn(), "maPhuongTien");
		String tokenHangXom = this.auth.accessToken(BQT_CU_DAN);
		this.mockMvc.perform(rut(tokenHangXom, don)).andExpect(status().isNotFound());
		this.mockMvc.perform(rut(this.tokenToi, "PT1")).andExpect(status().isNotFound());
		this.mockMvc.perform(rut(this.tokenToi, don))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Rút đơn đăng ký phương tiện thành công"))
			.andExpect(jsonPath("$.data.trangThaiDangKy").value("Đã hủy"))
			.andExpect(jsonPath("$.data.coTheRutDon").value(false));
		this.mockMvc.perform(rut(this.tokenToi, don))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_DON_KHONG_CON_CHO_DUYET"));
		assertThat(this.nhatKy.theoDoiTuong("PhuongTien:" + don)).extracting(d -> d.get("loai_thao_tac"))
			.containsExactly("PHUONGTIEN_DANG_KY", "PHUONGTIEN_RUT_DON");
		String keToan = this.auth.accessToken(KE_TOAN);
		dangKy(keToan, this.ma, this.toi, "Xe đạp", null).andExpect(status().isForbidden());
		doi(keToan, don, "Đã hủy", "x").andExpect(status().isForbidden());
		doi(this.tokenToi, don, "Đã hủy", "x").andExpect(status().isForbidden());
		xem(this.tokenToi, "A-0103").andExpect(status().isNotFound());
		dangKy(this.tokenToi, "A-0103", this.toi, "Xe đạp", null).andExpect(status().isNotFound());
	}

	// AC-2, AC-3: lịch sử tối đa 50 dòng, cư dân không thấy lịch sử của người khác
	@Test
	void lichSuGioiHanVaLocTheoCuDan() throws Exception {
		this.jdbc.update("""
				INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, trang_thai_dang_ky,
				    ly_do_trang_thai)
				SELECT ?, ?, 'Xe đạp', 'Đã hủy', 'Lý do riêng' FROM generate_series(1, 51)""", this.ma, this.hangXom);
		xem(this.bql, this.ma).andExpect(jsonPath("$.data.donVaLichSu", hasSize(50)));
		xem(this.tokenToi, this.ma).andExpect(jsonPath("$.data.donVaLichSu").isEmpty());
		this.jdbc.update("""
				INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, trang_thai_dang_ky,
				    ly_do_trang_thai)
				VALUES (?, ?, 'Xe đạp', 'Tạm dừng', 'Lý do riêng')""", this.ma, this.hangXom);
		xem(this.tokenToi, this.ma).andExpect(jsonPath("$.data.dangGui", hasSize(1)))
			.andExpect(jsonPath("$.data.dangGui[0].lyDoTrangThai").value(nullValue()));
	}

	// AC-2: mã căn không phân biệt hoa thường; căn không tồn tại là 404
	@Test
	void xemTheoMaCanThuongVaCanKhongTonTai() throws Exception {
		xem(this.bql, this.ma.toLowerCase()).andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Danh sách phương tiện của căn hộ thành công"))
			.andExpect(jsonPath("$.data.dangGui").isEmpty())
			.andExpect(jsonPath("$.data.donVaLichSu").isEmpty());
		xem(this.bql, "T0-9999").andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
		dangKy(this.bql, "T0-9999", this.toi, "Xe đạp", null).andExpect(status().isNotFound());
	}

	// AC-2: đang gửi cũ trước; đơn chờ đứng trước lịch sử, lịch sử mới trước, giới hạn
	// không cắt đơn chờ
	@Test
	void thuTuHaiMang() throws Exception {
		String dungSau = chenXe("Đang hoạt động", "2026-03-02T00:00:00Z");
		String dungTruoc = chenXe("Tạm dừng", "2026-03-01T00:00:00Z");
		String choMoi = chenXe("Chờ duyệt", "2026-03-02T00:00:00Z");
		String choCu = chenXe("Chờ duyệt", "2026-03-01T00:00:00Z");
		this.jdbc.update("""
				INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, trang_thai_dang_ky,
				    tao_luc, cap_nhat_luc)
				SELECT ?, ?, 'Xe đạp', 'Từ chối', TIMESTAMPTZ '2026-01-01', TIMESTAMPTZ '2026-01-01'
				FROM generate_series(1, 50)""", this.ma, this.toi);
		String lichSuMoiNhat = chenXe("Đã hủy", "2026-04-01T00:00:00Z");

		xem(this.bql, this.ma).andExpect(jsonPath("$.data.dangGui[*].maPhuongTien").value(contains(dungTruoc, dungSau)))
			.andExpect(jsonPath("$.data.donVaLichSu", hasSize(52)))
			.andExpect(jsonPath("$.data.donVaLichSu[0].maPhuongTien").value(choCu))
			.andExpect(jsonPath("$.data.donVaLichSu[1].maPhuongTien").value(choMoi))
			.andExpect(jsonPath("$.data.donVaLichSu[2].maPhuongTien").value(lichSuMoiNhat));
	}

	// AC-9, AC-10, AC-12: từ chối, kích hoạt lại không lý do, giữ nguyên trạng thái, biển
	// của xe tạm dừng
	@Test
	void tuChoiTamDungKichHoatLaiVaNhatKy() throws Exception {
		String bien = bienMoi();
		String don = data(dangKy(this.tokenToi, this.ma, this.toi, "Xe máy", bien).andReturn(), "maPhuongTien");
		doi(this.bql, don, "Từ chối", "Thiếu giấy tờ").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.trangThaiDangKy").value("Từ chối"))
			.andExpect(jsonPath("$.data.lyDoTrangThai").value("Thiếu giấy tờ"));
		xem(this.tokenToi, this.ma).andExpect(jsonPath("$.data.donVaLichSu[0].lyDoTrangThai").value("Thiếu giấy tờ"));

		String xe = data(
				dangKy(this.bql, this.ma, this.toi, "Xe máy", bien).andExpect(status().isCreated()).andReturn(),
				"maPhuongTien");
		doi(this.bql, xe, "Đang hoạt động", null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE"));
		doi(this.bql, xe.toLowerCase(), "Tạm dừng", "Nợ phí").andExpect(status().isOk());
		dangKy(this.bql, this.ma, this.toi, "Xe máy", bien).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_TRUNG_BIEN_SO"));
		doi(this.bql, xe, "Đang hoạt động", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.lyDoTrangThai").value(nullValue()))
			.andExpect(jsonPath("$.data.hoTenNguoiCapNhat").value("Trần Ban Quản Lý"));

		JsonNode tamDung = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("PhuongTien:" + xe).get(1));
		assertThat(tamDung.get("trangThai").get("cu").asString()).isEqualTo("Đang hoạt động");
		assertThat(tamDung.get("trangThai").get("moi").asString()).isEqualTo("Tạm dừng");
		assertThat(tamDung.get("lyDo").asString()).isEqualTo("Nợ phí");
		JsonNode kichHoat = NhatKyDb.chiTiet(this.nhatKy.theoDoiTuong("PhuongTien:" + xe).get(2));
		assertThat(kichHoat.has("lyDo")).isFalse();
	}

	// AC-12: request thất bại (400, 404, 409, 422) không ghi nhật ký
	@Test
	void requestThatBaiKhongGhiNhatKy() throws Exception {
		String bien = bienMoi();
		String xe = data(dangKy(this.bql, this.ma, this.toi, "Ô tô", bien).andReturn(), "maPhuongTien");
		int truoc = demNhatKyPhuongTien();

		dangKy(this.bql, this.ma, this.toi, "Ô tô", null).andExpect(status().isBadRequest());
		dangKy(this.bql, this.ma, this.toi, "Ô tô", bien).andExpect(status().isConflict());
		dangKy(this.bql, this.ma, "CD99999999", "Xe đạp", null).andExpect(status().isUnprocessableContent());
		doi(this.bql, xe, "Từ chối", "x").andExpect(status().isConflict());
		// AC-9: kiểm 400 chạy trước 404
		doi(this.bql, "PT99999999", "Tạm dừng", null).andExpect(status().isBadRequest());
		this.mockMvc.perform(rut(this.tokenToi, xe)).andExpect(status().isNotFound());

		assertThat(demNhatKyPhuongTien()).isEqualTo(truoc);
	}

	// AC-7: cư trú xét theo ngày Việt Nam của bean Clock; taoLuc lấy từ Clock
	@Test
	void ngayCuTruTheoGioVietNam() throws Exception {
		this.jdbc.update(
				"UPDATE ho_so_cu_tru SET ngay_ket_thuc = DATE '2026-11-16' WHERE ma_can_ho = ? " + "AND ma_cu_dan = ?",
				this.ma, this.toi);
		this.clock.set(Instant.parse("2026-11-16T16:30:00Z"));
		dangKy(this.auth.accessToken(BQL), this.ma, this.toi, "Xe đạp", null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.taoLuc").value("2026-11-16T16:30:00Z"))
			.andExpect(jsonPath("$.data.capNhatLuc").value("2026-11-16T16:30:00Z"));

		// 00:30 ngày 17 ở Việt Nam, UTC vẫn là ngày 16
		this.clock.set(Instant.parse("2026-11-16T17:30:00Z"));
		dangKy(this.auth.accessToken(BQL), this.ma, this.toi, "Xe đạp", null)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_CHU_XE_KHONG_CU_TRU"));
	}

	// AC-7: hồ sơ bắt đầu ngày mai chưa tính là đang ở
	@Test
	void chuXeChuaDenNgayOBiChan() throws Exception {
		this.jdbc.update(
				"UPDATE ho_so_cu_tru SET ngay_bat_dau = DATE '2026-11-17' WHERE ma_can_ho = ? " + "AND ma_cu_dan = ?",
				this.ma, this.toi);

		dangKy(this.bql, this.ma, this.toi, "Xe đạp", null).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_CHU_XE_KHONG_CU_TRU"));
	}

	// AC-7, AC-10: tạm khóa được phép, ngừng hoạt động chặn cả lúc kích hoạt lại
	@Test
	void trangThaiTaiKhoanChuXe() throws Exception {
		datTaiKhoan("Tạm khóa");
		String xe = data(
				dangKy(this.bql, this.ma, this.toi, "Xe đạp", null).andExpect(status().isCreated()).andReturn(),
				"maPhuongTien");
		doi(this.bql, xe, "Tạm dừng", "Nợ phí").andExpect(status().isOk());
		datTaiKhoan("Ngừng hoạt động");

		doi(this.bql, xe, "Đang hoạt động", null).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errorCode").value("PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG"))
			.andExpect(jsonPath("$.errors").isEmpty());
		doi(this.bql, xe, "Đã hủy", "Khóa tài khoản").andExpect(status().isOk());
	}

	// AC-7, AC-11: rút một đơn thì gửi được đơn mới
	@Test
	void rutDonGiaiPhongSuatDonCho() throws Exception {
		String don = data(dangKy(this.tokenToi, this.ma, this.toi, "Xe đạp", null).andReturn(), "maPhuongTien");
		for (int i = 0; i < 9; i++) {
			dangKy(this.tokenToi, this.ma, this.toi, "Xe đạp", null).andExpect(status().isCreated());
		}
		dangKy(this.tokenToi, this.ma, this.toi, "Xe đạp", null).andExpect(status().isUnprocessableContent());
		this.mockMvc.perform(rut(this.tokenToi, don)).andExpect(status().isOk());

		dangKy(this.tokenToi, this.ma, this.toi, "Xe đạp", null).andExpect(status().isCreated());
	}

	// AC-11: người gửi đã thôi cư trú thì không rút được đơn nữa
	@Test
	void nguoiGuiDaDiKhongRutDuocDon() throws Exception {
		String don = data(dangKy(this.tokenToi, this.ma, this.toi, "Xe đạp", null).andReturn(), "maPhuongTien");
		this.jdbc.update("UPDATE ho_so_cu_tru SET trang_thai_ho_so = 'Hết hiệu lực', ngay_ket_thuc = DATE '2026-02-01' "
				+ "WHERE ma_can_ho = ? AND ma_cu_dan = ?", this.ma, this.toi);

		this.mockMvc.perform(rut(this.tokenToi, don)).andExpect(status().isNotFound());
	}

	/**
	 * Chèn thẳng một xe đạp của {@code toi} tại căn thử, thời điểm cho sẵn; trả mã xe.
	 */
	private String chenXe(String trangThai, String luc) {
		return this.jdbc.queryForObject("""
				INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, trang_thai_dang_ky,
				    tao_luc, cap_nhat_luc)
				VALUES (?, ?, 'Xe đạp', ?, CAST(? AS timestamptz), CAST(? AS timestamptz))
				RETURNING ma_phuong_tien""", String.class, this.ma, this.toi, trangThai, luc, luc);
	}

	private int demNhatKyPhuongTien() {
		return this.jdbc.queryForObject("SELECT count(*) FROM nhat_ky_he_thong WHERE loai_thao_tac LIKE 'PHUONGTIEN%'",
				Integer.class);
	}

	private void datTaiKhoan(String trangThai) {
		this.jdbc.update("UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = ? WHERE so_dien_thoai = ?", trangThai,
				CU_DAN);
	}

	private void choCoRequestBiChan() throws InterruptedException {
		for (int i = 0; i < 200; i++) {
			Integer dangCho = this.jdbc.queryForObject("SELECT count(*) FROM pg_locks WHERE NOT granted",
					Integer.class);
			if (dangCho != null && dangCho > 0) {
				return;
			}
			TimeUnit.MILLISECONDS.sleep(50);
		}
		throw new IllegalStateException("Request không bị chặn bởi index");
	}

	private String taoCan() throws Exception {
		String toa = "T" + (System.nanoTime() % 100_000) + "P" + DEM.incrementAndGet();
		gui(post("/api/v1/buildings"), this.bql,
				Map.of("maToaNha", toa, "tenToaNha", "Tòa thử " + toa, "soTang", 1, "diaChiKhuVuc", "Khu thử"))
			.andExpect(status().isCreated());
		gui(post("/api/v1/apartments"), this.bql, Map.of("maTang", toa + "-01", "soCan", 1, "dienTich", 60))
			.andExpect(status().isCreated());
		return toa + "-0101";
	}

	/** Cho cư dân seed ở hợp lệ tại căn thử; trả mã cư dân. */
	private String choO(String soDienThoai, String maCanHo) {
		return this.jdbc.queryForObject("""
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
				    ngay_bat_dau, trang_thai_ho_so)
				SELECT ?, c.ma_cu_dan, (SELECT min(ma_nv) FROM nhan_vien_bql), 'Thành viên gia đình', 'Thường trú',
				    DATE '2026-01-01', 'Hợp lệ'
				FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung
				WHERE n.so_dien_thoai = ? RETURNING ma_cu_dan""", String.class, maCanHo, soDienThoai);
	}

	private static String bienMoi() {
		return "99T" + String.format("%06d", (System.nanoTime() / 1000 + DEM.incrementAndGet()) % 1_000_000);
	}

	private static String data(MvcResult kq, String truong) throws Exception {
		return JSON.readTree(kq.getResponse().getContentAsString()).get("data").get(truong).asString();
	}

	private ResultActions dangKy(String token, String maCanHo, String chuXe, String loai, String bienSo)
			throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("maCanHo", maCanHo);
		body.put("maCuDanSoHuu", chuXe);
		body.put("loaiPhuongTien", loai);
		body.put("bienSo", bienSo);
		return gui(post("/api/v1/vehicles"), token, body);
	}

	private ResultActions doi(String token, String maXe, String trangThai, String lyDo) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("trangThaiDangKy", trangThai);
		body.put("lyDo", lyDo);
		return gui(put("/api/v1/vehicles/" + maXe + "/status"), token, body);
	}

	private static MockHttpServletRequestBuilder rut(String token, String maXe) {
		return post("/api/v1/vehicles/" + maXe + "/cancel").header("X-Requested-With", "XMLHttpRequest")
			.cookie(new Cookie("access_token", token));
	}

	private ResultActions xem(String token, String maCanHo) throws Exception {
		return this.mockMvc
			.perform(get("/api/v1/apartments/" + maCanHo + "/vehicles").cookie(new Cookie("access_token", token)));
	}

	private ResultActions gui(MockHttpServletRequestBuilder r, String token, Object body) throws Exception {
		return this.mockMvc.perform(r.header("X-Requested-With", "XMLHttpRequest")
			.cookie(new Cookie("access_token", token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(JSON.writeValueAsString(body)));
	}

}
