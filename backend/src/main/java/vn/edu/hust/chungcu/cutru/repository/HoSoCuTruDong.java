package vn.edu.hust.chungcu.cutru.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Một dòng {@code ho_so_cu_tru} đầy đủ cột (spec 0014, spec 0015 Data model sketch). Ba
 * cột đề nghị chỉ có khi dòng do cư dân gửi.
 */
public record HoSoCuTruDong(String maHoSo, String maCanHo, String maCuDan, String maNvbqlXacMinh, String vaiTroCuTru,
		String tinhTrangCuTru, LocalDate ngayBatDau, LocalDate ngayKetThuc, String trangThaiHoSo, Instant taoLuc,
		Instant capNhatLuc, String maCuDanDeNghi, String ghiChuDeNghi, String lyDoTuChoi) {

	static final String COT_TRAN = """
			ma_ho_so, ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
			ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so, tao_luc, cap_nhat_luc,
			ma_cu_dan_de_nghi, ghi_chu_de_nghi, ly_do_tu_choi
			""";

	static final String COT = "SELECT " + COT_TRAN + "FROM ho_so_cu_tru\n";

	static HoSoCuTruDong doc(ResultSet rs, int rowNum) throws SQLException {
		return new HoSoCuTruDong(rs.getString("ma_ho_so"), rs.getString("ma_can_ho"), rs.getString("ma_cu_dan"),
				rs.getString("ma_nvbql_xac_minh"), rs.getString("vai_tro_cu_tru"), rs.getString("tinh_trang_cu_tru"),
				rs.getObject("ngay_bat_dau", LocalDate.class), rs.getObject("ngay_ket_thuc", LocalDate.class),
				rs.getString("trang_thai_ho_so"), rs.getObject("tao_luc", OffsetDateTime.class).toInstant(),
				rs.getObject("cap_nhat_luc", OffsetDateTime.class).toInstant(), rs.getString("ma_cu_dan_de_nghi"),
				rs.getString("ghi_chu_de_nghi"), rs.getString("ly_do_tu_choi"));
	}

}
