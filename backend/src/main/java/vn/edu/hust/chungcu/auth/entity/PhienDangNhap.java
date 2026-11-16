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
 * Thu hồi và xoay vòng thuộc tính năng 6.
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

	private Instant taoLuc;

	private Instant hetHanLuc;

	private Instant thuHoiLuc;

	private String lyDoThuHoi;

	private String diaChiIp;

	private String userAgent;

	protected PhienDangNhap() {
	}

	public PhienDangNhap(String maNguoiDung, UUID maHoPhien, String tokenHash, Instant taoLuc, Instant hetHanLuc,
			String diaChiIp, String userAgent) {
		this.maNguoiDung = maNguoiDung;
		this.maHoPhien = maHoPhien;
		this.tokenHash = tokenHash;
		this.taoLuc = taoLuc;
		this.hetHanLuc = hetHanLuc;
		this.diaChiIp = diaChiIp;
		this.userAgent = userAgent;
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
