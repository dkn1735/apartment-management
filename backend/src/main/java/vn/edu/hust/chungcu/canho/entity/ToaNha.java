package vn.edu.hust.chungcu.canho.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Tòa nhà (bảng {@code toa_nha}). Chỉ đọc ở lát này và chỉ ánh xạ cột đang dùng; UC01 làm
 * dày sau.
 */
@Entity
@Immutable
@Table(name = "toa_nha")
public class ToaNha {

	@Id
	private String maToaNha;

	protected ToaNha() {
	}

	public String getMaToaNha() {
		return this.maToaNha;
	}

}
