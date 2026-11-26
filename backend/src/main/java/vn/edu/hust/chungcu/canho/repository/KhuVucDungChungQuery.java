package vn.edu.hust.chungcu.canho.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.canho.dto.KhuVucDungChungResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/**
 * Danh mục khu vực dùng chung theo tòa và từ khóa, bằng SQL thuần (spec 0011, AC-11). Từ
 * khóa tìm chứa trong tên, không phân biệt hoa thường; ký tự đại diện của người dùng được
 * thoát.
 */
@Repository
public class KhuVucDungChungQuery {

	/** Trường sắp xếp được phép và cột tương ứng; chuỗi của người dùng không vào SQL. */
	public static final Map<String, String> COT_SAP_XEP = Map.of("tenKhuVuc", "k.ten_khu_vuc", "maKhuVuc",
			"k.ma_khu_vuc", "tenToaNha", "t.ten_toa_nha");

	private static final String TU = """
			FROM khu_vuc_dung_chung k
			JOIN toa_nha t ON t.ma_toa_nha = k.ma_toa_nha
			WHERE k.ma_toa_nha IN (:ma)""";

	private static final String LOC_TEN = " AND k.ten_khu_vuc ILIKE :mau";

	private static final String DANH_SACH = """
			SELECT k.ma_khu_vuc, k.ten_khu_vuc, k.vi_tri, k.ma_toa_nha, t.ten_toa_nha
			%s
			ORDER BY %s %s, k.ma_khu_vuc ASC
			LIMIT :size OFFSET :offset""";

	private final JdbcClient jdbc;

	public KhuVucDungChungQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** {@code q} là từ khóa đã chuẩn hóa, {@code null} khi không lọc. */
	public long dem(List<String> maToaNha, String q) {
		return thamSo(this.jdbc.sql("SELECT count(*) " + tu(q)), maToaNha, q).query(Long.class).single();
	}

	/** {@code trang.sortField()} phải là một khóa của {@link #COT_SAP_XEP}. */
	public List<KhuVucDungChungResponse> danhSach(List<String> maToaNha, String q, TrangYeuCau trang) {
		String cot = COT_SAP_XEP.get(trang.sortField());
		if (cot == null) {
			throw new IllegalArgumentException("Trường sắp xếp không hợp lệ: " + trang.sortField());
		}
		String sql = DANH_SACH.formatted(tu(q), cot, trang.dir().name());
		return thamSo(this.jdbc.sql(sql), maToaNha, q).param("size", trang.size())
			.param("offset", trang.offset())
			.query((rs, rowNum) -> new KhuVucDungChungResponse(rs.getString("ma_khu_vuc"), rs.getString("ten_khu_vuc"),
					rs.getString("vi_tri"), rs.getString("ma_toa_nha"), rs.getString("ten_toa_nha")))
			.list();
	}

	/** Mẫu ILIKE "chứa"; dấu gạch chéo ngược, phần trăm, gạch dưới được thoát. */
	static String mau(String q) {
		return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}

	private static String tu(String q) {
		return (q != null) ? TU + LOC_TEN : TU;
	}

	private static StatementSpec thamSo(StatementSpec sql, List<String> maToaNha, String q) {
		StatementSpec ketQua = sql.param("ma", maToaNha);
		return (q != null) ? ketQua.param("mau", mau(q)) : ketQua;
	}

}
