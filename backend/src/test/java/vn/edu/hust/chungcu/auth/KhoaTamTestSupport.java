package vn.edu.hust.chungcu.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;

/**
 * Đặt lại khóa tạm đăng nhập (spec 0009) cho mọi tài khoản và xóa bộ đếm của định danh
 * ảo. Các test dùng chung một database, nên lần sai của lớp test này cộng dồn vào lớp
 * test kia và làm tài khoản seed bị khóa; mọi integration test có gửi đăng nhập gọi hàm
 * này ở {@code @BeforeEach}. Cũng có vài hàm đọc ba cột khóa tạm cho test.
 */
public final class KhoaTamTestSupport {

	private KhoaTamTestSupport() {
	}

	public static void datLai(JdbcTemplate jdbc) {
		jdbc.update("""
				UPDATE nguoi_dung_he_thong SET so_lan_dang_nhap_sai = 0, khoa_tam_den = NULL, lan_sai_cuoi_luc = NULL
				WHERE so_lan_dang_nhap_sai <> 0 OR khoa_tam_den IS NOT NULL OR lan_sai_cuoi_luc IS NOT NULL""");
		jdbc.update("TRUNCATE dang_nhap_sai_dinh_danh");
	}

	/** Ba cột khóa tạm cùng các cột không được đổi của một tài khoản. */
	public record TrangThai(int soLanSai, Instant khoaTamDen, Instant lanSaiCuoiLuc, long phienBan, Instant capNhatLuc,
			String trangThaiTaiKhoan) {
	}

	public static TrangThai cua(JdbcTemplate jdbc, String maNguoiDung) {
		Map<String, Object> d = jdbc.queryForMap("""
				SELECT so_lan_dang_nhap_sai, khoa_tam_den, lan_sai_cuoi_luc, phien_ban, cap_nhat_luc,
				       trang_thai_tai_khoan
				FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?""", maNguoiDung);
		return new TrangThai((Integer) d.get("so_lan_dang_nhap_sai"), instant(d.get("khoa_tam_den")),
				instant(d.get("lan_sai_cuoi_luc")), (Long) d.get("phien_ban"), instant(d.get("cap_nhat_luc")),
				(String) d.get("trang_thai_tai_khoan"));
	}

	/** Dòng của một định danh ảo theo chuỗi đã chuẩn hóa, hoặc {@code null}. */
	public static Map<String, Object> dinhDanh(JdbcTemplate jdbc, String chuanHoa) {
		return jdbc
			.queryForList("SELECT * FROM dang_nhap_sai_dinh_danh WHERE dinh_danh_hash = ?",
					RefreshTokenGenerator.sha256Hex(chuanHoa))
			.stream()
			.findFirst()
			.orElse(null);
	}

	public static int soDongDinhDanh(JdbcTemplate jdbc) {
		Integer n = jdbc.queryForObject("SELECT count(*) FROM dang_nhap_sai_dinh_danh", Integer.class);
		return (n != null) ? n : 0;
	}

	public static Instant instant(Object value) {
		return (value instanceof Timestamp ts) ? ts.toInstant() : null;
	}

}
