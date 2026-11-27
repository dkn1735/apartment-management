package vn.edu.hust.chungcu.cutru;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dựng request và dữ liệu thử cho API hồ sơ cư trú (spec 0014). Tòa thử có mã bắt đầu
 * bằng {@code T}, cư dân thử có số điện thoại bắt đầu bằng {@code 0944}; {@link #donDep}
 * xóa cả hai cùng mọi hồ sơ cư trú của chúng, nên dữ liệu seed giữ nguyên.
 */
final class CuTruApi {

	static final String BQL = "0900000002";

	static final String KE_TOAN = "0900000004";

	static final String BQT_CU_DAN_A0101 = "0900000006";

	static final String CU_DAN_A0201 = "0900000007";

	/** Ngày theo giờ Việt Nam của {@code MutableClock.MAC_DINH}. */
	static final String HOM_NAY = "2026-11-16";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final AtomicInteger DEM = new AtomicInteger();

	private CuTruApi() {
	}

	/** Tạo một tòa thử một tầng và một căn {@code <toa>-0101} ở trạng thái Trống. */
	static String taoCanThu(MockMvc mockMvc, String bql) throws Exception {
		String toa = "T" + (System.nanoTime() % 100_000) + DEM.incrementAndGet();
		Map<String, Object> toaNha = new LinkedHashMap<>();
		toaNha.put("maToaNha", toa);
		toaNha.put("tenToaNha", "Tòa thử " + toa);
		toaNha.put("soTang", 1);
		toaNha.put("diaChiKhuVuc", "Khu thử nghiệm");
		mockMvc.perform(json(post("/api/v1/buildings"), bql, toaNha)).andExpect(status().isCreated());
		Map<String, Object> canHo = new LinkedHashMap<>();
		canHo.put("maTang", toa + "-01");
		canHo.put("soCan", 1);
		canHo.put("dienTich", 60);
		mockMvc.perform(json(post("/api/v1/apartments"), bql, canHo)).andExpect(status().isCreated());
		return toa + "-0101";
	}

	/** Thêm một cư dân thử có tài khoản ở {@code trangThai}; trả mã cư dân. */
	static String taoCuDanThu(JdbcTemplate jdbc, String hoTen, String trangThai) {
		int so = DEM.incrementAndGet();
		String sdt = String.format("0944%06d", (System.nanoTime() / 1000 + so) % 1_000_000);
		String maNguoiDung = jdbc.queryForObject("""
				INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash, trang_thai_tai_khoan)
				VALUES (?, ?, ?, '{noop}x', ?) RETURNING ma_nguoi_dung""", String.class, hoTen, sdt,
				"thu" + sdt + "@chungcu.test", trangThai);
		return jdbc.queryForObject("INSERT INTO cu_dan (ma_nguoi_dung, so_cccd) VALUES (?, ?) RETURNING ma_cu_dan",
				String.class, maNguoiDung, "044" + sdt.substring(1));
	}

	static String maCuDanCua(JdbcTemplate jdbc, String soDienThoai) {
		return jdbc.queryForObject("""
				SELECT c.ma_cu_dan FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung
				WHERE n.so_dien_thoai = ?""", String.class, soDienThoai);
	}

	static String maNguoiDungCua(JdbcTemplate jdbc, String soDienThoai) {
		return jdbc.queryForObject("SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?",
				String.class, soDienThoai);
	}

	/** Số CCCD của một cư dân theo số điện thoại. */
	static String soCccdCua(JdbcTemplate jdbc, String maCuDan) {
		return jdbc.queryForObject("SELECT so_cccd FROM cu_dan WHERE ma_cu_dan = ?", String.class, maCuDan);
	}

	static String soDienThoaiCua(JdbcTemplate jdbc, String maCuDan) {
		return jdbc.queryForObject("""
				SELECT n.so_dien_thoai FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung
				WHERE c.ma_cu_dan = ?""", String.class, maCuDan);
	}

	/**
	 * Cho cư dân seed {@code soDienThoai} đang ở hợp lệ tại căn thử từ đầu năm 2026, để
	 * có phạm vi căn của cư dân (spec 0015).
	 */
	static String choO(JdbcTemplate jdbc, String soDienThoai, String maCanHo) {
		return jdbc.queryForObject("""
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
				    ngay_bat_dau, trang_thai_ho_so)
				VALUES (?, ?, (SELECT min(ma_nv) FROM nhan_vien_bql), 'Thành viên gia đình', 'Thường trú',
				    DATE '2026-01-01', 'Hợp lệ') RETURNING ma_ho_so""", String.class, maCanHo,
				maCuDanCua(jdbc, soDienThoai));
	}

	static String trangThaiCan(JdbcTemplate jdbc, String maCanHo) {
		return jdbc.queryForObject("SELECT trang_thai_su_dung FROM can_ho WHERE ma_can_ho = ?", String.class, maCanHo);
	}

	static void donDep(JdbcTemplate jdbc) {
		// Xe của căn thử giữ khóa ngoại tới căn và cư dân thử, phải xóa trước (spec 0016)
		jdbc.update("""
				DELETE FROM phuong_tien WHERE ma_can_ho LIKE 'T%' OR ma_cu_dan_so_huu IN (
				    SELECT c.ma_cu_dan FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung
				    WHERE n.so_dien_thoai LIKE '0944%')""");
		jdbc.update("""
				DELETE FROM ho_so_cu_tru WHERE ma_can_ho LIKE 'T%' OR ma_cu_dan IN (
				    SELECT c.ma_cu_dan FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung
				    WHERE n.so_dien_thoai LIKE '0944%') OR ma_cu_dan_de_nghi IN (
				    SELECT c.ma_cu_dan FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung
				    WHERE n.so_dien_thoai LIKE '0944%')""");
		jdbc.update("DELETE FROM can_ho WHERE ma_tang LIKE 'T%'");
		jdbc.update("DELETE FROM tang WHERE ma_toa_nha LIKE 'T%'");
		jdbc.update("DELETE FROM toa_nha WHERE ma_toa_nha LIKE 'T%'");
		jdbc.update("""
				DELETE FROM cu_dan WHERE ma_nguoi_dung IN (
				    SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai LIKE '0944%')""");
		jdbc.update("DELETE FROM nguoi_dung_he_thong WHERE so_dien_thoai LIKE '0944%'");
	}

	static Map<String, Object> hoSo(String maCanHo, String maCuDan, String vaiTro, String tinhTrang, String batDau,
			String ketThuc) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("maCanHo", maCanHo);
		body.put("maCuDan", maCuDan);
		body.put("vaiTroCuTru", vaiTro);
		body.put("tinhTrangCuTru", tinhTrang);
		body.put("ngayBatDau", batDau);
		body.put("ngayKetThuc", ketThuc);
		return body;
	}

	static Map<String, Object> capNhat(String vaiTro, String tinhTrang, String batDau, String ketThuc,
			String trangThai) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("vaiTroCuTru", vaiTro);
		body.put("tinhTrangCuTru", tinhTrang);
		body.put("ngayBatDau", batDau);
		body.put("ngayKetThuc", ketThuc);
		body.put("trangThaiHoSo", trangThai);
		return body;
	}

	/**
	 * Body đề nghị cho người khác; {@code soCccd} {@code null} là đề nghị cho chính mình.
	 */
	static Map<String, Object> deNghi(String maCanHo, String soCccd, String soDienThoai, String vaiTro,
			String tinhTrang, String batDau, String ketThuc, String ghiChu) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("maCanHo", maCanHo);
		body.put("laChinhToi", soCccd == null);
		if (soCccd != null) {
			body.put("soCccd", soCccd);
			body.put("soDienThoai", soDienThoai);
		}
		body.put("vaiTroCuTru", vaiTro);
		body.put("tinhTrangCuTru", tinhTrang);
		body.put("ngayBatDau", batDau);
		body.put("ngayKetThuc", ketThuc);
		body.put("ghiChuDeNghi", ghiChu);
		return body;
	}

	static MockHttpServletRequestBuilder gui(String token, Object body) {
		return json(post("/api/v1/residency-records/resident-request"), token, body);
	}

	static MockHttpServletRequestBuilder guiRaw(String token, String body) {
		return raw(post("/api/v1/residency-records/resident-request"), token, body);
	}

	static MockHttpServletRequestBuilder duyet(String token, String maHoSo) {
		return raw(post("/api/v1/residency-records/" + maHoSo + "/approve"), token, "");
	}

	static MockHttpServletRequestBuilder tuChoi(String token, String maHoSo, Object lyDo) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("lyDoTuChoi", lyDo);
		return json(post("/api/v1/residency-records/" + maHoSo + "/reject"), token, body);
	}

	static MockHttpServletRequestBuilder huy(String token, String maHoSo) {
		return raw(post("/api/v1/residency-records/" + maHoSo + "/cancel"), token, "");
	}

	static MockHttpServletRequestBuilder danhSach(String token) {
		return get("/api/v1/residency-records").cookie(cookie(token));
	}

	static MockHttpServletRequestBuilder xem(String token, String maCanHo) {
		return get("/api/v1/apartments/" + maCanHo + "/residents").cookie(cookie(token));
	}

	static MockHttpServletRequestBuilder timCuDan(String token, String q) {
		MockHttpServletRequestBuilder r = get("/api/v1/residency-records/resident-options").cookie(cookie(token));
		return (q != null) ? r.queryParam("q", q) : r;
	}

	static MockHttpServletRequestBuilder them(String token, Object body) {
		return json(post("/api/v1/residency-records"), token, body);
	}

	static MockHttpServletRequestBuilder themRaw(String token, String body) {
		return raw(post("/api/v1/residency-records"), token, body);
	}

	static MockHttpServletRequestBuilder sua(String token, String maHoSo, Object body) {
		return json(put("/api/v1/residency-records/" + maHoSo), token, body);
	}

	static MockHttpServletRequestBuilder suaCanHo(String token, String maCanHo, Object dienTich, String trangThai) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("dienTich", dienTich);
		body.put("trangThaiSuDung", trangThai);
		return json(put("/api/v1/apartments/" + maCanHo), token, body);
	}

	static MockHttpServletRequestBuilder chiTietCanHo(String token, String maCanHo) {
		return get("/api/v1/apartments/" + maCanHo).cookie(cookie(token));
	}

	static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, String token, Object body) {
		return raw(r, token, JSON.writeValueAsString(body));
	}

	private static MockHttpServletRequestBuilder raw(MockHttpServletRequestBuilder r, String token, String body) {
		return r.header("X-Requested-With", "XMLHttpRequest")
			.cookie(cookie(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);
	}

	private static Cookie cookie(String token) {
		return new Cookie("access_token", token);
	}

}
