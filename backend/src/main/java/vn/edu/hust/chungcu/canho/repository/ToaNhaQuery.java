package vn.edu.hust.chungcu.canho.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.canho.dto.ToaNhaResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/**
 * Danh mục và chi tiết tòa nhà kèm số căn hộ, bằng SQL thuần (spec 0006 AC-7, spec 0011
 * AC-3, AC-9).
 */
@Repository
public class ToaNhaQuery {

	/** Trường sắp xếp được phép và cột tương ứng; chuỗi của người dùng không vào SQL. */
	public static final Map<String, String> COT_SAP_XEP = Map.of("maToaNha", "t.ma_toa_nha", "tenToaNha",
			"t.ten_toa_nha", "soTang", "t.so_tang");

	private static final String DEM = "SELECT count(*) FROM toa_nha WHERE ma_toa_nha IN (:ma)";

	private static final String DANH_SACH = """
			SELECT t.ma_toa_nha, t.ten_toa_nha, t.so_tang, t.dia_chi_khu_vuc, count(c.ma_can_ho) AS so_can_ho
			FROM toa_nha t
			LEFT JOIN tang g ON g.ma_toa_nha = t.ma_toa_nha
			LEFT JOIN can_ho c ON c.ma_tang = g.ma_tang
			WHERE t.ma_toa_nha IN (:ma)
			GROUP BY t.ma_toa_nha
			ORDER BY %s %s, t.ma_toa_nha ASC
			LIMIT :size OFFSET :offset""";

	private static final String CHI_TIET = """
			SELECT t.ma_toa_nha, t.ten_toa_nha, t.so_tang, t.dia_chi_khu_vuc, count(c.ma_can_ho) AS so_can_ho
			FROM toa_nha t
			LEFT JOIN tang g ON g.ma_toa_nha = t.ma_toa_nha
			LEFT JOIN can_ho c ON c.ma_tang = g.ma_tang
			WHERE t.ma_toa_nha = :ma
			GROUP BY t.ma_toa_nha""";

	// Cùng biểu thức với uq_toa_nha_ten_lower; COLLATE để lower() của tham số theo đúng
	// luật
	// của cột, không theo collation mặc định của database
	private static final String TRUNG = """
			SELECT bool_or(ma_toa_nha = :ma) AS trung_ma,
			       bool_or(lower(ten_toa_nha) = lower(CAST(:ten AS text) COLLATE "vi-VN-x-icu")) AS trung_ten
			FROM toa_nha
			WHERE ma_toa_nha = :ma OR lower(ten_toa_nha) = lower(CAST(:ten AS text) COLLATE "vi-VN-x-icu")""";

	/** Trường nào của tòa sắp thêm đã có (spec 0011, AC-3). */
	public record Trung(boolean ma, boolean ten) {
	}

	private final JdbcClient jdbc;

	public ToaNhaQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public long dem(List<String> maToaNha) {
		return this.jdbc.sql(DEM).param("ma", maToaNha).query(Long.class).single();
	}

	/** Một tòa kèm số căn hộ, cùng cách tính với danh mục (spec 0011, AC-9). */
	public Optional<ToaNhaResponse> chiTiet(String maToaNha) {
		return this.jdbc.sql(CHI_TIET).param("ma", maToaNha).query(ToaNhaQuery::dong).optional();
	}

	/** Mã hoặc tên (so theo {@code lower()} của PostgreSQL) đã có chưa (AC-3). */
	public Trung trung(String maToaNha, String tenToaNha) {
		return this.jdbc.sql(TRUNG)
			.param("ma", maToaNha)
			.param("ten", tenToaNha)
			.query((rs, rowNum) -> new Trung(rs.getBoolean("trung_ma"), rs.getBoolean("trung_ten")))
			.single();
	}

	/** {@code trang.sortField()} phải là một khóa của {@link #COT_SAP_XEP}. */
	public List<ToaNhaResponse> danhSach(List<String> maToaNha, TrangYeuCau trang) {
		String cot = COT_SAP_XEP.get(trang.sortField());
		if (cot == null) {
			throw new IllegalArgumentException("Trường sắp xếp không hợp lệ: " + trang.sortField());
		}
		return this.jdbc.sql(DANH_SACH.formatted(cot, trang.dir().name()))
			.param("ma", maToaNha)
			.param("size", trang.size())
			.param("offset", trang.offset())
			.query(ToaNhaQuery::dong)
			.list();
	}

	private static ToaNhaResponse dong(ResultSet rs, int rowNum) throws SQLException {
		return new ToaNhaResponse(rs.getString("ma_toa_nha"), rs.getString("ten_toa_nha"), rs.getInt("so_tang"),
				rs.getString("dia_chi_khu_vuc"), rs.getLong("so_can_ho"));
	}

}
