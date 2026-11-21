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

	/** Khóa {@code pg_advisory_xact_lock} của luật Admin cuối cùng; một hằng cố định. */
	static final long KHOA_ADMIN = 800_811L;

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

	/**
	 * Sửa họ tên, số điện thoại, email và tăng {@code phien_ban} nếu phiên bản còn khớp
	 * (spec 0008, AC-7). @return số dòng đã sửa, 0 khi phiên bản lệch
	 */
	public int capNhatChung(String maNguoiDung, String hoTen, String soDienThoai, String email, long phienBan,
			Instant luc) {
		return this.jdbc.sql("""
				UPDATE nguoi_dung_he_thong
				SET ho_ten = :hoTen, so_dien_thoai = :soDienThoai, email = :email,
				    phien_ban = phien_ban + 1, cap_nhat_luc = :luc
				WHERE ma_nguoi_dung = :ma AND phien_ban = :phienBan""")
			.param("hoTen", hoTen)
			.param("soDienThoai", soDienThoai)
			.param("email", email)
			.param("luc", OffsetDateTime.ofInstant(luc, ZoneOffset.UTC))
			.param("ma", maNguoiDung)
			.param("phienBan", phienBan)
			.update();
	}

	/**
	 * Đổi trạng thái và tăng {@code phien_ban} trên dòng đã khóa (spec 0008, AC-10). Mở
	 * lại ({@code moLai}) thì cũng đặt lại bộ đếm đăng nhập sai và khóa tạm.
	 */
	public void capNhatTrangThai(String maNguoiDung, String trangThai, boolean moLai, Instant luc) {
		this.jdbc.sql("""
				UPDATE nguoi_dung_he_thong
				SET trang_thai_tai_khoan = :trangThai, phien_ban = phien_ban + 1, cap_nhat_luc = :luc,
				    so_lan_dang_nhap_sai = CASE WHEN :moLai THEN 0 ELSE so_lan_dang_nhap_sai END,
				    khoa_tam_den = CASE WHEN :moLai THEN NULL ELSE khoa_tam_den END
				WHERE ma_nguoi_dung = :ma""")
			.param("trangThai", trangThai)
			.param("moLai", moLai)
			.param("luc", OffsetDateTime.ofInstant(luc, ZoneOffset.UTC))
			.param("ma", maNguoiDung)
			.update();
	}

	/**
	 * Đặt mật khẩu tạm, bật cờ bắt đổi, tăng {@code phien_ban} trên dòng đã khóa (spec
	 * 0008, AC-12).
	 */
	public void datLaiMatKhau(String maNguoiDung, String matKhauHash, Instant luc) {
		this.jdbc.sql("""
				UPDATE nguoi_dung_he_thong
				SET mat_khau_hash = :hash, phai_doi_mat_khau = true, doi_mat_khau_luc = :luc,
				    phien_ban = phien_ban + 1, cap_nhat_luc = :luc
				WHERE ma_nguoi_dung = :ma""")
			.param("hash", matKhauHash)
			.param("luc", OffsetDateTime.ofInstant(luc, ZoneOffset.UTC))
			.param("ma", maNguoiDung)
			.update();
	}

	/** Tăng {@code phien_ban} khi phiên bản còn khớp (AC-8, AC-9); 0 khi lệch. */
	public int tangPhienBan(String maNguoiDung, long phienBan, Instant luc) {
		return this.jdbc.sql("""
				UPDATE nguoi_dung_he_thong SET phien_ban = phien_ban + 1, cap_nhat_luc = :luc
				WHERE ma_nguoi_dung = :ma AND phien_ban = :phienBan""")
			.param("luc", OffsetDateTime.ofInstant(luc, ZoneOffset.UTC))
			.param("ma", maNguoiDung)
			.param("phienBan", phienBan)
			.update();
	}

	/**
	 * Khóa tư vấn dùng chung cho mọi transaction có thể làm giảm số Admin đang hoạt động
	 * (spec 0008, AC-11); nhả khi transaction kết thúc.
	 */
	public void khoaAdmin() {
		this.jdbc.sql("SELECT pg_advisory_xact_lock(:khoa)").param("khoa", KHOA_ADMIN).query(Object.class).list();
	}

	/** Sửa các trường của một dòng bảng con đã có. */
	public void suaVaiTro(String maNguoiDung, VaiTroGhi vaiTro) {
		BangVaiTro bang = BangVaiTro.cua(vaiTro.vaiTro());
		if (bang.truong().isEmpty()) {
			return;
		}
		List<String> gan = new ArrayList<>();
		for (TruongVaiTro truong : bang.truong()) {
			gan.add(truong.cot() + " = :" + truong.ten());
		}
		JdbcClient.StatementSpec sql = this.jdbc
			.sql("UPDATE " + bang.bang() + " SET " + String.join(", ", gan) + " WHERE ma_nguoi_dung = :maNguoiDung")
			.param("maNguoiDung", maNguoiDung);
		for (TruongVaiTro truong : bang.truong()) {
			sql = sql.param(truong.ten(), vaiTro.giaTri().get(truong));
		}
		sql.update();
	}

	/** Xóa dòng bảng con; lỗi khóa ngoại để nguyên cho service (AC-9). */
	public int xoaVaiTro(String maNguoiDung, BangVaiTro bang) {
		return this.jdbc.sql("DELETE FROM " + bang.bang() + " WHERE ma_nguoi_dung = :ma")
			.param("ma", maNguoiDung)
			.update();
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
