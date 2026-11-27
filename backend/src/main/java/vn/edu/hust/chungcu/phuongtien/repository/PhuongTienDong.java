package vn.edu.hust.chungcu.phuongtien.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;

/** Một dòng {@code phuong_tien} đầy đủ cột (spec 0016, Data model sketch). */
public record PhuongTienDong(String maPhuongTien, String maCanHo, String maCuDanSoHuu, String loaiPhuongTien,
		String bienSo, String nhanHieuMauSac, String trangThaiDangKy, String maCuDanDangKy, String maNvbqlCapNhat,
		String lyDoTrangThai, Instant taoLuc, Instant capNhatLuc) {

	static final String CHON = """
			SELECT ma_phuong_tien, ma_can_ho, ma_cu_dan_so_huu, loai_phuong_tien, bien_so, nhan_hieu_mau_sac,
			       trang_thai_dang_ky, ma_cu_dan_dang_ky, ma_nvbql_cap_nhat, ly_do_trang_thai, tao_luc, cap_nhat_luc
			FROM phuong_tien
			""";

	static PhuongTienDong doc(ResultSet rs, int rowNum) throws SQLException {
		return new PhuongTienDong(rs.getString("ma_phuong_tien"), rs.getString("ma_can_ho"),
				rs.getString("ma_cu_dan_so_huu"), rs.getString("loai_phuong_tien"), rs.getString("bien_so"),
				rs.getString("nhan_hieu_mau_sac"), rs.getString("trang_thai_dang_ky"),
				rs.getString("ma_cu_dan_dang_ky"), rs.getString("ma_nvbql_cap_nhat"), rs.getString("ly_do_trang_thai"),
				rs.getObject("tao_luc", OffsetDateTime.class).toInstant(),
				rs.getObject("cap_nhat_luc", OffsetDateTime.class).toInstant());
	}

}
