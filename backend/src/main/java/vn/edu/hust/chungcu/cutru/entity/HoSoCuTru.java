package vn.edu.hust.chungcu.cutru.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

/**
 * Hồ sơ cư trú (bảng {@code ho_so_cu_tru}). Chỉ đọc ở lát này và chỉ ánh xạ cột đang
 * dùng; UC02 làm dày sau.
 */
@Entity
@Immutable
@Table(name = "ho_so_cu_tru")
public class HoSoCuTru {

	public static final String HOP_LE = "Hợp lệ";

	@Id
	private String maHoSo;

	private String maCanHo;

	private String maCuDan;

	private LocalDate ngayBatDau;

	private LocalDate ngayKetThuc;

	private String trangThaiHoSo;

	protected HoSoCuTru() {
	}

	public String getMaHoSo() {
		return this.maHoSo;
	}

	public String getMaCanHo() {
		return this.maCanHo;
	}

	public String getMaCuDan() {
		return this.maCuDan;
	}

	public LocalDate getNgayBatDau() {
		return this.ngayBatDau;
	}

	public LocalDate getNgayKetThuc() {
		return this.ngayKetThuc;
	}

	public String getTrangThaiHoSo() {
		return this.trangThaiHoSo;
	}

}
