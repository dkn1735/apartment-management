package vn.edu.hust.chungcu.canho.repository;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Ghi tòa nhà và tầng bằng SQL thuần (spec 0011). Không dùng JPA: entity {@code ToaNha}
 * chỉ đọc, và {@code save()} với khóa do người dùng đặt sẽ chạy {@code merge} thay vì báo
 * trùng. Lỗi ràng buộc để nguyên cho service đổi nghĩa; mọi lệnh chạy trong transaction
 * của service.
 */
@Repository
public class ToaNhaGhi {

	private final JdbcClient jdbc;

	public ToaNhaGhi(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Thêm một dòng {@code toa_nha}; trùng khóa thì nổ ngay tại lệnh này (AC-3). */
	public void themToaNha(String maToaNha, String tenToaNha, int soTang, String diaChiKhuVuc) {
		this.jdbc.sql("""
				INSERT INTO toa_nha (ma_toa_nha, ten_toa_nha, so_tang, dia_chi_khu_vuc)
				VALUES (:ma, :ten, :soTang, :diaChi)""")
			.param("ma", maToaNha)
			.param("ten", tenToaNha)
			.param("soTang", soTang)
			.param("diaChi", diaChiKhuVuc)
			.update();
	}

	/**
	 * Thêm tầng 1 đến {@code n} của tòa trong một lệnh, mã theo mẫu của {@code MaTang}.
	 */
	public void themTangTu1Den(String maToaNha, int n) {
		this.jdbc.sql("""
				INSERT INTO tang (ma_tang, ma_toa_nha, so_tang)
				SELECT :ma || '-' || lpad(g::text, 2, '0'), :ma, g
				FROM generate_series(1, :n) g""").param("ma", maToaNha).param("n", n).update();
	}

	/** Khóa dòng tòa tới hết transaction và đọc số tầng hiện có (AC-4). */
	public Optional<Integer> soTangKhoaDong(String maToaNha) {
		return this.jdbc.sql("SELECT so_tang FROM toa_nha WHERE ma_toa_nha = :ma FOR UPDATE")
			.param("ma", maToaNha)
			.query(Integer.class)
			.optional();
	}

	/**
	 * Thêm một tầng và đặt số tầng của tòa bằng tầng đó; gọi sau {@link #soTangKhoaDong}.
	 */
	public void themTang(String maToaNha, int soTang, String maTang) {
		this.jdbc.sql("INSERT INTO tang (ma_tang, ma_toa_nha, so_tang) VALUES (:maTang, :ma, :soTang)")
			.param("maTang", maTang)
			.param("ma", maToaNha)
			.param("soTang", soTang)
			.update();
		this.jdbc.sql("UPDATE toa_nha SET so_tang = :soTang WHERE ma_toa_nha = :ma")
			.param("soTang", soTang)
			.param("ma", maToaNha)
			.update();
	}

}
