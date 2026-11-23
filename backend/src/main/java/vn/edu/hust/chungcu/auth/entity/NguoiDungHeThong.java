package vn.edu.hust.chungcu.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Tài khoản đăng nhập (bảng {@code nguoi_dung_he_thong}). Lát này chỉ đọc; các lần ghi
 * của đăng nhập đi qua câu UPDATE riêng trong repository để không tăng {@code phien_ban}.
 * Chỉ ánh xạ cột đang dùng.
 */
@Entity
@Table(name = "nguoi_dung_he_thong")
@DynamicUpdate
public class NguoiDungHeThong {

	public static final String HOAT_DONG = "Hoạt động";

	@Id
	private String maNguoiDung;

	private String hoTen;

	private String soDienThoai;

	private String email;

	private String matKhauHash;

	private String trangThaiTaiKhoan;

	private Instant dangNhapCuoiLuc;

	/** Đang dùng mật khẩu tạm, phải đổi ở lần đăng nhập tới (spec 0008, AC-13). */
	private boolean phaiDoiMatKhau;

	private Instant capNhatLuc;

	@Version
	private long phienBan;

	protected NguoiDungHeThong() {
	}

	public String getMaNguoiDung() {
		return this.maNguoiDung;
	}

	public String getHoTen() {
		return this.hoTen;
	}

	public String getSoDienThoai() {
		return this.soDienThoai;
	}

	public String getEmail() {
		return this.email;
	}

	public String getMatKhauHash() {
		return this.matKhauHash;
	}

	public String getTrangThaiTaiKhoan() {
		return this.trangThaiTaiKhoan;
	}

	public boolean dangHoatDong() {
		return HOAT_DONG.equals(this.trangThaiTaiKhoan);
	}

	public boolean isPhaiDoiMatKhau() {
		return this.phaiDoiMatKhau;
	}

	public Instant getDangNhapCuoiLuc() {
		return this.dangNhapCuoiLuc;
	}

	public Instant getCapNhatLuc() {
		return this.capNhatLuc;
	}

	public long getPhienBan() {
		return this.phienBan;
	}

}
