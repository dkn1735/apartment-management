package vn.edu.hust.chungcu.canho.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Ghi căn hộ bằng SQL thuần (spec 0012). Không dùng JPA: mã do server ghép, nên
 * {@code save()} sẽ chạy {@code merge} thay vì báo trùng. Mọi lệnh chạy trong transaction
 * của service; lỗi ràng buộc để nguyên cho service đổi nghĩa.
 */
@Repository
public class CanHoGhi {

	/** Giá trị đang có của các trường sửa được. */
	public record GiaTri(BigDecimal dienTich, String trangThaiSuDung) {
	}

	private final JdbcClient jdbc;

	public CanHoGhi(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Thêm một dòng; trùng khóa chính thì nổ ngay tại lệnh này (AC-9). */
	public void them(String maCanHo, String maTang, BigDecimal dienTich, String trangThaiSuDung) {
		this.jdbc.sql("""
				INSERT INTO can_ho (ma_can_ho, ma_tang, dien_tich, trang_thai_su_dung)
				VALUES (:ma, :maTang, :dienTich, :trangThai)""")
			.param("ma", maCanHo)
			.param("maTang", maTang)
			.param("dienTich", dienTich)
			.param("trangThai", trangThaiSuDung)
			.update();
	}

	/** Khóa dòng căn tới hết transaction và đọc giá trị hiện có (AC-11, AC-13). */
	public Optional<GiaTri> khoaDong(String maCanHo) {
		return this.jdbc.sql("SELECT dien_tich, trang_thai_su_dung FROM can_ho WHERE ma_can_ho = :ma FOR UPDATE")
			.param("ma", maCanHo)
			.query((rs, rowNum) -> new GiaTri(rs.getBigDecimal("dien_tich"), rs.getString("trang_thai_su_dung")))
			.optional();
	}

	/**
	 * Khóa dòng căn tới hết transaction và đọc cờ tạm dừng tiện ích (spec 0013, AC-2,
	 * AC-6).
	 */
	public Optional<Boolean> khoaCoTienIch(String maCanHo) {
		return this.jdbc.sql("SELECT bi_tam_dung_tien_ich FROM can_ho WHERE ma_can_ho = :ma FOR UPDATE")
			.param("ma", maCanHo)
			.query(Boolean.class)
			.optional();
	}

	/**
	 * Đặt cờ và lần đổi tiện ích bằng một lệnh; gọi sau {@link #khoaCoTienIch} trong cùng
	 * transaction.
	 */
	public void capNhatTienIch(String maCanHo, boolean tamDung, String lyDo, Instant luc, String maNguoiDoi) {
		this.jdbc.sql("""
				UPDATE can_ho SET bi_tam_dung_tien_ich = :tamDung, tien_ich_ly_do = :lyDo,
				       tien_ich_doi_luc = :luc, tien_ich_doi_boi = :nguoiDoi
				WHERE ma_can_ho = :ma""")
			.param("tamDung", tamDung)
			.param("lyDo", lyDo)
			.param("luc", luc.atOffset(ZoneOffset.UTC))
			.param("nguoiDoi", maNguoiDoi)
			.param("ma", maCanHo)
			.update();
	}

	/**
	 * Chỉ đổi trạng thái sử dụng, khi đồng bộ theo hồ sơ cư trú (spec 0014, AC-10). Gọi
	 * sau {@link #khoaDong} trong cùng transaction.
	 */
	public void capNhatTrangThai(String maCanHo, String trangThaiSuDung) {
		this.jdbc.sql("UPDATE can_ho SET trang_thai_su_dung = :trangThai WHERE ma_can_ho = :ma")
			.param("trangThai", trangThaiSuDung)
			.param("ma", maCanHo)
			.update();
	}

	/** Gọi sau {@link #khoaDong} trong cùng transaction. */
	public void capNhat(String maCanHo, BigDecimal dienTich, String trangThaiSuDung) {
		this.jdbc.sql("UPDATE can_ho SET dien_tich = :dienTich, trang_thai_su_dung = :trangThai WHERE ma_can_ho = :ma")
			.param("dienTich", dienTich)
			.param("trangThai", trangThaiSuDung)
			.param("ma", maCanHo)
			.update();
	}

}
