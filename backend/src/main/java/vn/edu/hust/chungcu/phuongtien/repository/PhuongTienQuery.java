package vn.edu.hust.chungcu.phuongtien.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Đọc phương tiện bằng SQL thuần (spec 0016). Mỗi hàm là một câu SQL. */
@Repository
public class PhuongTienQuery {

	/** Điều kiện "đang giữ biển"; trùng với điều kiện của index một phần ở V20. */
	private static final String GIU_BIEN = "trang_thai_dang_ky IN ('Đang hoạt động', 'Tạm dừng')";

	/**
	 * {@code null} là không lọc; khác {@code null} chỉ giữ dòng của chủ xe hoặc người
	 * gửi.
	 */
	private static final String LIEN_QUAN = """
			(CAST(:cuDan AS varchar) IS NULL OR ma_cu_dan_so_huu = :cuDan OR ma_cu_dan_dang_ky = :cuDan)""";

	private final JdbcClient jdbc;

	public PhuongTienQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Xe đang giữ biển của căn, cũ trước (AC-2). */
	public List<PhuongTienDong> dangGui(String maCanHo) {
		return this.jdbc
			.sql(PhuongTienDong.CHON + "WHERE ma_can_ho = :canHo AND " + GIU_BIEN
					+ " ORDER BY tao_luc ASC, ma_phuong_tien ASC")
			.param("canHo", maCanHo)
			.query(PhuongTienDong::doc)
			.list();
	}

	/**
	 * Mọi đơn chờ duyệt của căn, cũ trước (AC-2).
	 * @param chiCuaCuDan {@code null} là mọi đơn
	 */
	public List<PhuongTienDong> choDuyet(String maCanHo, String chiCuaCuDan) {
		return this.jdbc
			.sql(PhuongTienDong.CHON + "WHERE ma_can_ho = :canHo AND trang_thai_dang_ky = 'Chờ duyệt' AND " + LIEN_QUAN
					+ " ORDER BY tao_luc ASC, ma_phuong_tien ASC")
			.param("canHo", maCanHo)
			.param("cuDan", chiCuaCuDan)
			.query(PhuongTienDong::doc)
			.list();
	}

	/**
	 * Các dòng từ chối và đã hủy mới nhất; giới hạn áp sau khi lọc theo cư dân (AC-2).
	 */
	public List<PhuongTienDong> lichSu(String maCanHo, String chiCuaCuDan, int toiDa) {
		return this.jdbc
			.sql(PhuongTienDong.CHON + "WHERE ma_can_ho = :canHo AND trang_thai_dang_ky IN ('Từ chối', 'Đã hủy') AND "
					+ LIEN_QUAN + " ORDER BY cap_nhat_luc DESC, ma_phuong_tien DESC LIMIT :toiDa")
			.param("canHo", maCanHo)
			.param("cuDan", chiCuaCuDan)
			.param("toiDa", toiDa)
			.query(PhuongTienDong::doc)
			.list();
	}

	public Optional<PhuongTienDong> cua(String maPhuongTien) {
		return this.jdbc.sql(PhuongTienDong.CHON + "WHERE ma_phuong_tien = :ma")
			.param("ma", maPhuongTien)
			.query(PhuongTienDong::doc)
			.optional();
	}

	/** Khóa dòng tới hết transaction; gọi sau khi đã khóa dòng {@code can_ho}. */
	public Optional<PhuongTienDong> khoaDong(String maPhuongTien) {
		return this.jdbc.sql(PhuongTienDong.CHON + "WHERE ma_phuong_tien = :ma FOR UPDATE")
			.param("ma", maPhuongTien)
			.query(PhuongTienDong::doc)
			.optional();
	}

	public int demChoDuyet(String maCanHo) {
		return this.jdbc
			.sql("SELECT count(*) FROM phuong_tien WHERE ma_can_ho = :canHo AND trang_thai_dang_ky = 'Chờ duyệt'")
			.param("canHo", maCanHo)
			.query(Integer.class)
			.single();
	}

	/** Xe đang giữ biển {@code bienSo} ở bất kỳ căn nào; tối đa một dòng. */
	public Optional<PhuongTienDong> dangGiuBien(String bienSo) {
		return this.jdbc.sql(PhuongTienDong.CHON + "WHERE bien_so = :bienSo AND " + GIU_BIEN)
			.param("bienSo", bienSo)
			.query(PhuongTienDong::doc)
			.optional();
	}

}
