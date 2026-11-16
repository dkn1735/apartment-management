package vn.edu.hust.chungcu.auth;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tạo tài khoản riêng cho test cần đổi trạng thái hay hash, để không đụng tài khoản seed
 * dùng chung. Mỗi tài khoản có số điện thoại và email ngẫu nhiên, ghi thẳng bằng SQL (đã
 * commit).
 */
public final class TaiKhoanThu {

	private final JdbcTemplate jdbc;

	public TaiKhoanThu(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public record TaiKhoan(String maNguoiDung, String soDienThoai, String email) {
	}

	public TaiKhoan tao(String matKhauHash) {
		return tao(matKhauHash, "Hoạt động");
	}

	public TaiKhoan tao(String matKhauHash, String trangThai) {
		String so = "091" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
		String email = "thu" + so + "@chungcu.test";
		String ma = this.jdbc.queryForObject("""
				INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash, trang_thai_tai_khoan)
				VALUES ('Người Thử', ?, ?, ?, ?) RETURNING ma_nguoi_dung""", String.class, so, email, matKhauHash,
				trangThai);
		return new TaiKhoan(ma, so, email);
	}

	public String hash(String maNguoiDung) {
		return this.jdbc.queryForObject("SELECT mat_khau_hash FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				String.class, maNguoiDung);
	}

	public void doiTrangThai(String maNguoiDung, String trangThai) {
		this.jdbc.update("UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = ? WHERE ma_nguoi_dung = ?", trangThai,
				maNguoiDung);
	}

	public int soPhien(String maNguoiDung) {
		Integer count = this.jdbc.queryForObject("SELECT count(*) FROM phien_dang_nhap WHERE ma_nguoi_dung = ?",
				Integer.class, maNguoiDung);
		return (count != null) ? count : 0;
	}

}
