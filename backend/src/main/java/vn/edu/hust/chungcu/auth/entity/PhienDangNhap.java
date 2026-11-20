package vn.edu.hust.chungcu.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Một refresh token đã cấp (bảng {@code phien_dang_nhap}); DB chỉ giữ SHA-256 của token.
 * Mọi dòng sinh ra từ một lần đăng nhập cùng {@code maHoPhien} và cùng {@code batDauLuc}
 * (spec 0005). Dòng đã thu hồi không bao giờ còn hiệu lực lại.
 */
@Entity
@Table(name = "phien_dang_nhap")
public class PhienDangNhap {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long maPhien;

	private String maNguoiDung;

	private UUID maHoPhien;

	private String tokenHash;

	private Instant batDauLuc;

	private Instant taoLuc;

	private Instant hetHanLuc;

	private Instant thuHoiLuc;

	private String lyDoThuHoi;

	private String diaChiIp;

	private String userAgent;

	protected PhienDangNhap() {
	}

	public PhienDangNhap(String maNguoiDung, UUID maHoPhien, String tokenHash, Instant batDauLuc, Instant taoLuc,
			Instant hetHanLuc, String diaChiIp, String userAgent) {
		this.maNguoiDung = maNguoiDung;
		this.maHoPhien = maHoPhien;
		this.tokenHash = tokenHash;
		this.batDauLuc = batDauLuc;
		this.taoLuc = taoLuc;
		this.hetHanLuc = hetHanLuc;
		this.diaChiIp = diaChiIp;
		this.userAgent = userAgent;
	}

	/** Thu hồi dòng này; dòng đã thu hồi thì không được thu hồi lần nữa. */
	public void thuHoi(Instant luc, String lyDo) {
		if (this.thuHoiLuc != null) {
			throw new IllegalStateException("Phiên đã bị thu hồi");
		}
		this.thuHoiLuc = luc;
		this.lyDoThuHoi = lyDo;
	}

	/** Chưa thu hồi và chưa hết hạn tại {@code now}. */
	public boolean conHieuLuc(Instant now) {
		return this.thuHoiLuc == null && this.hetHanLuc.isAfter(now);
	}

	public Long getMaPhien() {
		return this.maPhien;
	}

	public String getMaNguoiDung() {
		return this.maNguoiDung;
	}

	public UUID getMaHoPhien() {
		return this.maHoPhien;
	}

	public String getTokenHash() {
		return this.tokenHash;
	}

	public Instant getBatDauLuc() {
		return this.batDauLuc;
	}

	public Instant getTaoLuc() {
		return this.taoLuc;
	}

	public Instant getHetHanLuc() {
		return this.hetHanLuc;
	}

	public Instant getThuHoiLuc() {
		return this.thuHoiLuc;
	}

	public String getLyDoThuHoi() {
		return this.lyDoThuHoi;
	}

	public String getDiaChiIp() {
		return this.diaChiIp;
	}

	public String getUserAgent() {
		return this.userAgent;
	}

}
