package vn.edu.hust.chungcu.cutru.repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/**
 * Tra cứu hồ sơ cư trú toàn tòa có phân trang bằng SQL thuần (spec 0015, AC-9, AC-10).
 * Chỉ đọc bảng {@code ho_so_cu_tru}: tập căn hộ và tập cư dân khớp từ khóa do service lấy
 * sẵn qua phân hệ {@code canho} và {@code auth}.
 */
@Repository
public class HoSoCuTruTraCuuQuery {

	/** Trường sắp xếp được phép và cột tương ứng. */
	public static final Map<String, String> COT_SAP_XEP = Map.of("taoLuc", "tao_luc", "capNhatLuc", "cap_nhat_luc",
			"ngayBatDau", "ngay_bat_dau", "ngayKetThuc", "ngay_ket_thuc", "maCanHo", "ma_can_ho");

	/**
	 * Điều kiện đã chuẩn hóa; {@code null} hoặc rỗng là không lọc, trừ {@code maCanHo}.
	 *
	 * @param maCanHo khác rỗng
	 * @param maCuDanKhop chỉ dùng khi có {@code mauQ}; rỗng thì bỏ nhánh này
	 * @param mauQ mẫu LIKE "chứa" đã thoát
	 */
	public record DieuKien(Collection<String> maCanHo, List<String> trangThaiHoSo, String vaiTroCuTru,
			String tinhTrangCuTru, Collection<String> maCuDanKhop, String mauQ) {
	}

	private final JdbcClient jdbc;

	public HoSoCuTruTraCuuQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public long dem(DieuKien dk) {
		Map<String, Object> thamSo = new LinkedHashMap<>();
		String sql = "SELECT count(*) FROM ho_so_cu_tru " + where(dk, thamSo);
		return this.jdbc.sql(sql).params(thamSo).query(Long.class).single();
	}

	/**
	 * Một trang, khóa phụ {@code ma_ho_so} cùng chiều để phân trang ổn định; ngày kết
	 * thúc trống đứng cuối khi tăng dần, đứng đầu khi giảm dần.
	 */
	public List<HoSoCuTruDong> tim(DieuKien dk, TrangYeuCau trang) {
		Map<String, Object> thamSo = new LinkedHashMap<>();
		String chieu = (trang.dir() == Sort.Direction.ASC) ? "ASC" : "DESC";
		String nulls = "ngayKetThuc".equals(trang.sortField())
				? ((trang.dir() == Sort.Direction.ASC) ? " NULLS LAST" : " NULLS FIRST") : "";
		String sql = HoSoCuTruDong.COT + where(dk, thamSo) + " ORDER BY " + COT_SAP_XEP.get(trang.sortField()) + " "
				+ chieu + nulls + ", ma_ho_so " + chieu + " LIMIT :limit OFFSET :offset";
		thamSo.put("limit", trang.size());
		thamSo.put("offset", trang.offset());
		return this.jdbc.sql(sql).params(thamSo).query(HoSoCuTruDong::doc).list();
	}

	private static String where(DieuKien dk, Map<String, Object> thamSo) {
		StringBuilder sql = new StringBuilder("WHERE ma_can_ho IN (:maCanHo)");
		thamSo.put("maCanHo", dk.maCanHo());
		if (dk.trangThaiHoSo() != null && !dk.trangThaiHoSo().isEmpty()) {
			sql.append(" AND trang_thai_ho_so IN (:trangThai)");
			thamSo.put("trangThai", dk.trangThaiHoSo());
		}
		if (dk.vaiTroCuTru() != null) {
			sql.append(" AND vai_tro_cu_tru = :vaiTro");
			thamSo.put("vaiTro", dk.vaiTroCuTru());
		}
		if (dk.tinhTrangCuTru() != null) {
			sql.append(" AND tinh_trang_cu_tru = :tinhTrang");
			thamSo.put("tinhTrang", dk.tinhTrangCuTru());
		}
		if (dk.mauQ() != null) {
			sql.append(" AND (");
			if (dk.maCuDanKhop() != null && !dk.maCuDanKhop().isEmpty()) {
				sql.append("ma_cu_dan IN (:cuDan) OR ");
				thamSo.put("cuDan", dk.maCuDanKhop());
			}
			sql.append(
					"upper(ma_can_ho) LIKE upper(:mau) ESCAPE '\\' OR upper(ma_ho_so) LIKE upper(:mau) ESCAPE '\\')");
			thamSo.put("mau", dk.mauQ());
		}
		return sql.toString();
	}

}
