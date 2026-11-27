package vn.edu.hust.chungcu.cutru.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Đọc hồ sơ cư trú bằng SQL thuần (spec 0014). Các hàm kiểm chồng ngày dùng cùng biểu
 * thức {@code daterange(..., '[]')} với ràng buộc loại trừ của {@code V18}, nên kết luận
 * của service và của PostgreSQL luôn khớp nhau.
 */
@Repository
public class HoSoCuTruQuery {

	private static final String HOP_LE = "Hợp lệ";

	private static final String GIAO_NGAY = """
			daterange(ngay_bat_dau, ngay_ket_thuc, '[]')
			    && daterange(CAST(:batDau AS date), CAST(:ketThuc AS date), '[]')
			AND (CAST(:truMa AS varchar) IS NULL OR ma_ho_so <> :truMa)""";

	private final JdbcClient jdbc;

	public HoSoCuTruQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Mọi hồ sơ của căn, kể cả đề nghị (spec 0015, AC-8); service tự chia nhóm và sắp
	 * xếp.
	 */
	public List<HoSoCuTruDong> theoCanHo(String maCanHo) {
		return this.jdbc.sql(HoSoCuTruDong.COT + "WHERE ma_can_ho = :ma")
			.param("ma", maCanHo)
			.query(HoSoCuTruDong::doc)
			.list();
	}

	/** Cặp căn và cư dân đã có đề nghị đang chờ xác minh (spec 0015, AC-3 bước 4). */
	public boolean coDeNghiChoXacMinh(String maCanHo, String maCuDan) {
		return this.jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM ho_so_cu_tru
				WHERE ma_can_ho = :ma AND ma_cu_dan = :cuDan AND trang_thai_ho_so = 'Chờ xác minh')""")
			.param("ma", maCanHo)
			.param("cuDan", maCuDan)
			.query(Boolean.class)
			.single();
	}

	/** Số đề nghị đang chờ xác minh của căn (spec 0015, AC-3 bước 5). */
	public long demDeNghiChoXacMinh(String maCanHo) {
		return this.jdbc
			.sql("SELECT count(*) FROM ho_so_cu_tru WHERE ma_can_ho = :ma AND trang_thai_ho_so = 'Chờ xác minh'")
			.param("ma", maCanHo)
			.query(Long.class)
			.single();
	}

	public Optional<HoSoCuTruDong> cua(String maHoSo) {
		return this.jdbc.sql(HoSoCuTruDong.COT + "WHERE ma_ho_so = :ma")
			.param("ma", maHoSo)
			.query(HoSoCuTruDong::doc)
			.optional();
	}

	/** Số hồ sơ đang cư trú vào {@code ngay} của căn (Thuật ngữ). */
	public long demDangCuTru(String maCanHo, LocalDate ngay) {
		return this.jdbc.sql("""
				SELECT count(*) FROM ho_so_cu_tru
				WHERE ma_can_ho = :ma AND trang_thai_ho_so = 'Hợp lệ' AND ngay_bat_dau <= :ngay
				  AND (ngay_ket_thuc IS NULL OR ngay_ket_thuc >= :ngay)""")
			.param("ma", maCanHo)
			.param("ngay", ngay)
			.query(Long.class)
			.single();
	}

	/**
	 * Cùng cư dân đã có hồ sơ hợp lệ khác giao khoảng ngày tại căn (AC-6 bước 4).
	 * @param truMaHoSo hồ sơ đang sửa, {@code null} khi thêm mới
	 */
	public boolean coChongNgayCuDan(String maCanHo, String maCuDan, LocalDate batDau, LocalDate ketThuc,
			String truMaHoSo) {
		return this.jdbc
			.sql("SELECT EXISTS (SELECT 1 FROM ho_so_cu_tru WHERE ma_can_ho = :ma AND ma_cu_dan = :cuDan "
					+ "AND trang_thai_ho_so = '" + HOP_LE + "' AND " + GIAO_NGAY + ")")
			.param("ma", maCanHo)
			.param("cuDan", maCuDan)
			.param("batDau", batDau)
			.param("ketThuc", ketThuc)
			.param("truMa", truMaHoSo)
			.query(Boolean.class)
			.single();
	}

	/** Căn đã có chủ căn hộ hợp lệ khác giao khoảng ngày (AC-6 bước 5). */
	public boolean coChongNgayChu(String maCanHo, LocalDate batDau, LocalDate ketThuc, String truMaHoSo) {
		return this.jdbc
			.sql("SELECT EXISTS (SELECT 1 FROM ho_so_cu_tru WHERE ma_can_ho = :ma " + "AND trang_thai_ho_so = '"
					+ HOP_LE + "' AND vai_tro_cu_tru = 'Chủ căn hộ' AND " + GIAO_NGAY + ")")
			.param("ma", maCanHo)
			.param("batDau", batDau)
			.param("ketThuc", ketThuc)
			.param("truMa", truMaHoSo)
			.query(Boolean.class)
			.single();
	}

	/** Căn có hồ sơ hợp lệ đã qua ngày kết thúc, tăng dần (AC-11). */
	public List<String> maCanHoCoHoSoQuaHan(LocalDate homNay) {
		return this.jdbc.sql("""
				SELECT DISTINCT ma_can_ho FROM ho_so_cu_tru
				WHERE trang_thai_ho_so = 'Hợp lệ' AND ngay_ket_thuc < :homNay
				ORDER BY ma_can_ho""").param("homNay", homNay).query(String.class).list();
	}

}
