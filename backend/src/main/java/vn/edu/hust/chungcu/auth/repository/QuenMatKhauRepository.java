package vn.edu.hust.chungcu.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Đọc ghi {@code yeu_cau_dat_lai_mat_khau} và phần {@code nguoi_dung_he_thong} mà quên
 * mật khẩu cần (spec 0010), bằng SQL thuần. Thời gian bind kiểu {@link OffsetDateTime}
 * UTC. Chỉ nhận hash của token, không bao giờ nhận token gốc.
 */
@Repository
public class QuenMatKhauRepository {

	private final JdbcClient jdbc;

	public QuenMatKhauRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Tài khoản có thể nhận email đặt lại; {@code email} đã viết thường. */
	public record TaiKhoanNhanEmail(String maNguoiDung, String hoTen, String email, String trangThai) {

		@Override
		public String toString() {
			return "TaiKhoanNhanEmail[maNguoiDung=" + this.maNguoiDung + ", trangThai=" + this.trangThai + "]";
		}

	}

	/** Số yêu cầu của một tài khoản trong cửa sổ, và lúc tạo yêu cầu gần nhất. */
	public record ThongKeYeuCau(long soYeuCau, Instant ganNhat) {
	}

	/** Yêu cầu còn hiệu lực của một token. */
	public record YeuCauConHieuLuc(String maNguoiDung, Instant hetHanLuc) {
	}

	/** Không khóa; so bằng trên email đã chuẩn hóa (cột {@code email} viết thường). */
	public Optional<TaiKhoanNhanEmail> timTheoEmail(String email) {
		return this.jdbc.sql("""
				SELECT ma_nguoi_dung, ho_ten, email, trang_thai_tai_khoan FROM nguoi_dung_he_thong
				WHERE email = :email""")
			.param("email", email)
			.query((rs, rowNum) -> new TaiKhoanNhanEmail(rs.getString("ma_nguoi_dung"), rs.getString("ho_ten"),
					rs.getString("email"), rs.getString("trang_thai_tai_khoan")))
			.optional();
	}

	/**
	 * Khóa dòng tài khoản tới hết transaction và trả trạng thái mới nhất. Mọi luồng của
	 * spec 0010 khóa dòng này trước khi ghi dòng yêu cầu (thứ tự khóa). Dùng
	 * {@code FOR NO KEY UPDATE} như {@link TaiKhoanQuery#khoa}, để không chặn
	 * {@code FOR KEY SHARE} của khóa ngoại.
	 */
	public Optional<String> khoaTaiKhoan(String maNguoiDung) {
		return this.jdbc.sql("""
				SELECT trang_thai_tai_khoan FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = :ma
				FOR NO KEY UPDATE""").param("ma", maNguoiDung).query(String.class).optional();
	}

	/** Số yêu cầu của cả hệ thống có {@code tao_luc} sau {@code tu}. */
	public long demToanBang(Instant tu) {
		return this.jdbc.sql("SELECT count(*) FROM yeu_cau_dat_lai_mat_khau WHERE tao_luc > :tu")
			.param("tu", utc(tu))
			.query(Long.class)
			.single();
	}

	public ThongKeYeuCau thongKe(String maNguoiDung, Instant tu) {
		return this.jdbc.sql("""
				SELECT count(*) AS so, max(tao_luc) AS gan_nhat FROM yeu_cau_dat_lai_mat_khau
				WHERE ma_nguoi_dung = :ma AND tao_luc > :tu""")
			.param("ma", maNguoiDung)
			.param("tu", utc(tu))
			.query((rs, rowNum) -> new ThongKeYeuCau(rs.getLong("so"), instant(rs, "gan_nhat")))
			.single();
	}

	/** Hủy mọi yêu cầu mở của tài khoản. @return số dòng đã hủy */
	public int huyConMo(String maNguoiDung, Instant now) {
		return this.jdbc.sql("""
				UPDATE yeu_cau_dat_lai_mat_khau SET huy_luc = :now
				WHERE ma_nguoi_dung = :ma AND da_dung_luc IS NULL AND huy_luc IS NULL""")
			.param("now", utc(now))
			.param("ma", maNguoiDung)
			.update();
	}

	/**
	 * @return mã yêu cầu vừa sinh
	 */
	public long them(String maNguoiDung, String tokenHash, Instant taoLuc, Instant hetHanLuc) {
		return this.jdbc.sql("""
				INSERT INTO yeu_cau_dat_lai_mat_khau (ma_nguoi_dung, token_hash, tao_luc, het_han_luc)
				VALUES (:ma, :hash, :taoLuc, :hetHan) RETURNING ma_yeu_cau""")
			.param("ma", maNguoiDung)
			.param("hash", tokenHash)
			.param("taoLuc", utc(taoLuc))
			.param("hetHan", utc(hetHanLuc))
			.query(Long.class)
			.single();
	}

	/**
	 * Yêu cầu mở, chưa hết hạn, của một tài khoản đang {@code Hoạt động}; không khóa.
	 */
	public Optional<YeuCauConHieuLuc> timConHieuLuc(String tokenHash, Instant now) {
		return this.jdbc.sql("""
				SELECT y.ma_nguoi_dung, y.het_han_luc FROM yeu_cau_dat_lai_mat_khau y
				JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = y.ma_nguoi_dung
				WHERE y.token_hash = :hash AND y.da_dung_luc IS NULL AND y.huy_luc IS NULL
				  AND y.het_han_luc > :now AND n.trang_thai_tai_khoan = 'Hoạt động'""")
			.param("hash", tokenHash)
			.param("now", utc(now))
			.query((rs, rowNum) -> new YeuCauConHieuLuc(rs.getString("ma_nguoi_dung"), instant(rs, "het_han_luc")))
			.optional();
	}

	/**
	 * Tiêu token: chỉ một lần gọi thành công cho mỗi token, vì điều kiện còn mở nằm trong
	 * chính câu {@code UPDATE}. @return số dòng đã tiêu, 0 hoặc 1
	 */
	public int tieuToken(String tokenHash, String maNguoiDung, Instant now) {
		return this.jdbc.sql("""
				UPDATE yeu_cau_dat_lai_mat_khau SET da_dung_luc = :now
				WHERE token_hash = :hash AND ma_nguoi_dung = :ma AND da_dung_luc IS NULL AND huy_luc IS NULL
				  AND het_han_luc > :now""")
			.param("now", utc(now))
			.param("hash", tokenHash)
			.param("ma", maNguoiDung)
			.update();
	}

	/** Xóa mọi yêu cầu có {@code tao_luc} trước {@code truoc}. @return số dòng đã xóa */
	public int xoaTruoc(Instant truoc) {
		return this.jdbc.sql("DELETE FROM yeu_cau_dat_lai_mat_khau WHERE tao_luc < :truoc")
			.param("truoc", utc(truoc))
			.update();
	}

	private static OffsetDateTime utc(Instant instant) {
		return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	private static Instant instant(ResultSet rs, String cot) throws SQLException {
		OffsetDateTime value = rs.getObject(cot, OffsetDateTime.class);
		return (value != null) ? value.toInstant() : null;
	}

}
