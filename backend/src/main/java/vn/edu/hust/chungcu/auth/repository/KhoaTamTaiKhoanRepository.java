package vn.edu.hust.chungcu.auth.repository;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bộ đếm đăng nhập sai và khóa tạm trên {@code nguoi_dung_he_thong} (spec 0009). Entity
 * không map ba cột này, nên lưu entity không ghi đè chúng. Không câu nào đổi
 * {@code phien_ban} hay {@code cap_nhat_luc}.
 */
@Repository
public class KhoaTamTaiKhoanRepository {

	private static final String GIU_LUOT = CauGiuLuot.sql("nguoi_dung_he_thong", "ma_nguoi_dung",
			"so_lan_dang_nhap_sai");

	private final JdbcClient jdbc;

	public KhoaTamTaiKhoanRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Giữ lượt cho một tài khoản; tài khoản không còn thì rỗng. */
	public Optional<KetQuaGiuLuot> giuLuot(String maNguoiDung, Instant now, int n, Duration thoiGian) {
		return CauGiuLuot.chay(this.jdbc, GIU_LUOT, maNguoiDung, now, n, thoiGian);
	}

	/** Bộ đếm về 0 và xóa khóa; chỉ ghi khi có gì để xóa (AC-4). */
	public int datLai(String maNguoiDung) {
		return this.jdbc.sql("""
				UPDATE nguoi_dung_he_thong
				SET so_lan_dang_nhap_sai = 0, khoa_tam_den = NULL, lan_sai_cuoi_luc = NULL
				WHERE ma_nguoi_dung = :ma AND (so_lan_dang_nhap_sai <> 0 OR khoa_tam_den IS NOT NULL)""")
			.param("ma", maNguoiDung)
			.update();
	}

	/**
	 * Mở khóa sớm (AC-8): đặt lại cả ba cột và trả {@code khoa_tam_den} trước khi xóa
	 * ({@code RETURNING old.}, PostgreSQL 18). Tài khoản không có thì rỗng.
	 */
	public Optional<KhoaCu> moKhoa(String maNguoiDung) {
		return this.jdbc.sql("""
				UPDATE nguoi_dung_he_thong
				SET so_lan_dang_nhap_sai = 0, khoa_tam_den = NULL, lan_sai_cuoi_luc = NULL
				WHERE ma_nguoi_dung = :ma
				RETURNING old.khoa_tam_den AS khoa_cu""").param("ma", maNguoiDung).query((rs, rowNum) -> {
			OffsetDateTime cu = rs.getObject("khoa_cu", OffsetDateTime.class);
			return new KhoaCu((cu != null) ? cu.toInstant() : null);
		}).optional();
	}

	/** Giá trị {@code khoa_tam_den} trước khi mở khóa, có thể {@code null}. */
	public record KhoaCu(Instant khoaTamDen) {
	}

}
