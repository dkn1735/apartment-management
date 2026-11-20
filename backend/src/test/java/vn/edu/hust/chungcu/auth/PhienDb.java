package vn.edu.hust.chungcu.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;

/** Đọc bảng {@code phien_dang_nhap} trong test bằng một kết nối riêng (đã commit). */
final class PhienDb {

	private final JdbcTemplate jdbc;

	PhienDb(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Dòng của một refresh token thô. */
	Map<String, Object> dong(String refreshToken) {
		return this.jdbc.queryForMap("SELECT * FROM phien_dang_nhap WHERE token_hash = ?",
				RefreshTokenGenerator.sha256Hex(refreshToken));
	}

	UUID hoPhien(String refreshToken) {
		return (UUID) dong(refreshToken).get("ma_ho_phien");
	}

	int soDong(UUID maHoPhien) {
		return dem("SELECT count(*) FROM phien_dang_nhap WHERE ma_ho_phien = ?", maHoPhien);
	}

	int soDongConHieuLuc(UUID maHoPhien) {
		return dem("SELECT count(*) FROM phien_dang_nhap WHERE ma_ho_phien = ? AND thu_hoi_luc IS NULL", maHoPhien);
	}

	int soDongCua(String maNguoiDung) {
		return dem("SELECT count(*) FROM phien_dang_nhap WHERE ma_nguoi_dung = ?", maNguoiDung);
	}

	/** {@code ly_do_thu_hoi} của mọi dòng trong họ, theo thứ tự tạo. */
	java.util.List<String> lyDo(UUID maHoPhien) {
		return this.jdbc.queryForList(
				"SELECT coalesce(ly_do_thu_hoi, 'con') FROM phien_dang_nhap WHERE ma_ho_phien = ? ORDER BY ma_phien",
				String.class, maHoPhien);
	}

	private int dem(String sql, Object arg) {
		Integer count = this.jdbc.queryForObject(sql, Integer.class, arg);
		return (count != null) ? count : 0;
	}

	static Instant instant(Object value) {
		return switch (value) {
			case Timestamp ts -> ts.toInstant();
			case OffsetDateTime odt -> odt.toInstant();
			default -> throw new IllegalArgumentException("Không phải mốc giờ: " + value);
		};
	}

}
