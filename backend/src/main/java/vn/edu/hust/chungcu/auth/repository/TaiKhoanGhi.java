package vn.edu.hust.chungcu.auth.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Ghi tài khoản và dòng vai trò cho màn quản lý (spec 0008) bằng SQL thuần. Không có
 * entity cho bảng con; tên bảng và cột lấy từ {@link BangVaiTro}, không từ dữ liệu người
 * dùng. Lỗi ràng buộc để nguyên cho service đổi nghĩa.
 */
@Repository
public class TaiKhoanGhi {

	private final JdbcClient jdbc;

	public TaiKhoanGhi(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Thêm người dùng {@code Hoạt động}, phải đổi mật khẩu ở lần đăng nhập đầu (AC-1).
	 * @return mã người dùng vừa sinh
	 */
	public String themNguoiDung(String hoTen, String soDienThoai, String email, String matKhauHash, Instant luc) {
		return this.jdbc.sql("""
				INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash, trang_thai_tai_khoan,
				    phai_doi_mat_khau, tao_luc, cap_nhat_luc)
				VALUES (:hoTen, :soDienThoai, :email, :hash, 'Hoạt động', true, :luc, :luc)
				RETURNING ma_nguoi_dung""")
			.param("hoTen", hoTen)
			.param("soDienThoai", soDienThoai)
			.param("email", email)
			.param("hash", matKhauHash)
			.param("luc", OffsetDateTime.ofInstant(luc, ZoneOffset.UTC))
			.query(String.class)
			.single();
	}

	/** Thêm một dòng bảng con; mã bảng con sinh từ sequence của bảng. */
	public void themVaiTro(String maNguoiDung, VaiTroGhi vaiTro) {
		BangVaiTro bang = BangVaiTro.cua(vaiTro.vaiTro());
		List<String> cot = new ArrayList<>(List.of("ma_nguoi_dung"));
		List<String> giaTri = new ArrayList<>(List.of(":maNguoiDung"));
		for (TruongVaiTro truong : bang.truong()) {
			cot.add(truong.cot());
			giaTri.add(":" + truong.ten());
		}
		JdbcClient.StatementSpec sql = this.jdbc
			.sql("INSERT INTO " + bang.bang() + " (" + String.join(", ", cot) + ") VALUES (" + String.join(", ", giaTri)
					+ ")")
			.param("maNguoiDung", maNguoiDung);
		for (TruongVaiTro truong : bang.truong()) {
			sql = sql.param(truong.ten(), vaiTro.giaTri().get(truong));
		}
		sql.update();
	}

}
