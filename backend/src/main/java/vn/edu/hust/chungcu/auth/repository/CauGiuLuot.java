package vn.edu.hust.chungcu.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Câu giữ lượt dùng chung cho tài khoản thật và định danh ảo (spec 0009, mục Câu giữ
 * lượt): khóa dòng, tăng bộ đếm hiệu lực, tự đặt khóa ở lượt thứ {@code n}; không làm gì
 * khi đang khóa. Một câu, tự commit, chạy trước bcrypt. Mọi mốc giờ lấy từ {@code Clock}
 * của nơi gọi, không dùng {@code now()} của PostgreSQL.
 */
final class CauGiuLuot {

	private CauGiuLuot() {
	}

	/**
	 * Câu SQL cho bảng {@code bang}, khóa chính {@code cotKhoa}, bộ đếm {@code cotDem}.
	 */
	static String sql(String bang, String cotKhoa, String cotDem) {
		return """
				WITH cu AS (
				    SELECT %2$s, khoa_tam_den,
				           CASE WHEN lan_sai_cuoi_luc > CAST(:moc AS timestamptz) THEN %3$s ELSE 0 END AS dem
				    FROM %1$s
				    WHERE %2$s = :khoa
				    FOR NO KEY UPDATE
				), giu AS (
				    UPDATE %1$s t
				    SET %3$s = CASE WHEN cu.dem + 1 >= :n THEN 0 ELSE cu.dem + 1 END,
				        khoa_tam_den = CASE WHEN cu.dem + 1 >= :n THEN CAST(:den AS timestamptz) END,
				        lan_sai_cuoi_luc = CAST(:now AS timestamptz)
				    FROM cu
				    WHERE t.%2$s = cu.%2$s
				      AND (cu.khoa_tam_den IS NULL OR cu.khoa_tam_den <= CAST(:now AS timestamptz))
				    RETURNING (t.khoa_tam_den IS NOT NULL) AS vua_khoa
				)
				SELECT cu.khoa_tam_den AS khoa_cu, giu.vua_khoa
				FROM cu LEFT JOIN giu ON true""".formatted(bang, cotKhoa, cotDem);
	}

	/** Chạy câu đã dựng với giá trị khóa {@code khoa}; không có dòng thì rỗng. */
	static Optional<KetQuaGiuLuot> chay(JdbcClient jdbc, String sql, String khoa, Instant now, int n,
			Duration thoiGian) {
		return jdbc.sql(sql)
			.param("khoa", khoa)
			.param("now", utc(now))
			.param("moc", utc(now.minus(thoiGian)))
			.param("den", utc(now.plus(thoiGian)))
			.param("n", n)
			.query((rs, rowNum) -> ketQua(rs))
			.optional();
	}

	static OffsetDateTime utc(Instant value) {
		return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}

	private static KetQuaGiuLuot ketQua(ResultSet rs) throws SQLException {
		OffsetDateTime khoaCu = rs.getObject("khoa_cu", OffsetDateTime.class);
		Object vuaKhoa = rs.getObject("vua_khoa");
		return new KetQuaGiuLuot((khoaCu != null) ? khoaCu.toInstant() : null, (Boolean) vuaKhoa);
	}

}
