package vn.edu.hust.chungcu.canho.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.canho.dto.TangResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/** Danh sách tầng của một tòa kèm số căn hộ, bằng SQL thuần (spec 0011, AC-8). */
@Repository
public class TangQuery {

	/** Trường sắp xếp được phép và cột tương ứng; chuỗi của người dùng không vào SQL. */
	public static final Map<String, String> COT_SAP_XEP = Map.of("soTang", "g.so_tang", "maTang", "g.ma_tang");

	private static final String DEM = "SELECT count(*) FROM tang WHERE ma_toa_nha = :ma";

	private static final String DANH_SACH = """
			SELECT g.ma_tang, g.so_tang, count(c.ma_can_ho) AS so_can_ho
			FROM tang g
			LEFT JOIN can_ho c ON c.ma_tang = g.ma_tang
			WHERE g.ma_toa_nha = :ma
			GROUP BY g.ma_tang
			ORDER BY %s %s, g.ma_tang ASC
			LIMIT :size OFFSET :offset""";

	private final JdbcClient jdbc;

	public TangQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Tòa của một tầng, rỗng khi tầng không có (spec 0012, AC-8). */
	public Optional<String> maToaNhaCua(String maTang) {
		return this.jdbc.sql("SELECT ma_toa_nha FROM tang WHERE ma_tang = :ma")
			.param("ma", maTang)
			.query(String.class)
			.optional();
	}

	public long dem(String maToaNha) {
		return this.jdbc.sql(DEM).param("ma", maToaNha).query(Long.class).single();
	}

	/** {@code trang.sortField()} phải là một khóa của {@link #COT_SAP_XEP}. */
	public List<TangResponse> danhSach(String maToaNha, TrangYeuCau trang) {
		String cot = COT_SAP_XEP.get(trang.sortField());
		if (cot == null) {
			throw new IllegalArgumentException("Trường sắp xếp không hợp lệ: " + trang.sortField());
		}
		return this.jdbc.sql(DANH_SACH.formatted(cot, trang.dir().name()))
			.param("ma", maToaNha)
			.param("size", trang.size())
			.param("offset", trang.offset())
			.query((rs, rowNum) -> new TangResponse(rs.getString("ma_tang"), rs.getInt("so_tang"),
					rs.getLong("so_can_ho")))
			.list();
	}

}
