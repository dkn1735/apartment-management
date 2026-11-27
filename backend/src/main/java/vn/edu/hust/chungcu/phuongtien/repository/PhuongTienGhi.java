package vn.edu.hust.chungcu.phuongtien.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Ghi phương tiện bằng SQL thuần (spec 0016). Mọi lệnh chạy trong transaction của
 * service; lỗi ràng buộc để nguyên cho service đổi nghĩa.
 */
@Repository
public class PhuongTienGhi {

	/** Giá trị của một phương tiện mới. */
	public record Moi(String maCanHo, String maCuDanSoHuu, String loaiPhuongTien, String bienSo, String nhanHieuMauSac,
			String trangThaiDangKy, String maCuDanDangKy, String maNvbqlCapNhat) {
	}

	private final JdbcClient jdbc;

	public PhuongTienGhi(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Thêm một dòng, trả mã PostgreSQL sinh ra. */
	public String them(Moi moi, Instant luc) {
		return this.jdbc.sql("""
				INSERT INTO phuong_tien (ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, bien_so, nhan_hieu_mau_sac,
				    trang_thai_dang_ky, ma_cu_dan_dang_ky, ma_nvbql_cap_nhat, tao_luc, cap_nhat_luc)
				VALUES (:canHo, :chuXe, :loai, CAST(:bienSo AS varchar), CAST(:nhanHieu AS varchar), :trangThai,
				    CAST(:dangKy AS varchar), CAST(:maNv AS varchar), :luc, :luc)
				RETURNING ma_phuong_tien""")
			.param("canHo", moi.maCanHo())
			.param("chuXe", moi.maCuDanSoHuu())
			.param("loai", moi.loaiPhuongTien())
			.param("bienSo", moi.bienSo())
			.param("nhanHieu", moi.nhanHieuMauSac())
			.param("trangThai", moi.trangThaiDangKy())
			.param("dangKy", moi.maCuDanDangKy())
			.param("maNv", moi.maNvbqlCapNhat())
			.param("luc", luc.atOffset(ZoneOffset.UTC))
			.query(String.class)
			.single();
	}

	/**
	 * Đổi trạng thái đăng ký; gọi sau khi đã khóa dòng.
	 * @param maNvbql {@code null} khi người gửi tự rút đơn
	 */
	public void doiTrangThai(String maPhuongTien, String trangThai, String lyDo, String maNvbql, Instant luc) {
		this.jdbc.sql("""
				UPDATE phuong_tien SET trang_thai_dang_ky = :trangThai, ly_do_trang_thai = CAST(:lyDo AS varchar),
				       ma_nvbql_cap_nhat = CAST(:maNv AS varchar), cap_nhat_luc = :luc
				WHERE ma_phuong_tien = :ma""")
			.param("trangThai", trangThai)
			.param("lyDo", lyDo)
			.param("maNv", maNvbql)
			.param("luc", luc.atOffset(ZoneOffset.UTC))
			.param("ma", maPhuongTien)
			.update();
	}

}
