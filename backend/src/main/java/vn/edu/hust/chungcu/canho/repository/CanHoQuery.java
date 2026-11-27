package vn.edu.hust.chungcu.canho.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.dto.LanDoiTienIchResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/**
 * Tìm và đọc căn hộ kèm tầng và tòa, bằng SQL thuần (spec 0012, AC-1, AC-2, AC-4, AC-5).
 * Giá trị lọc đi vào SQL qua tham số đặt tên; cột sắp xếp lấy từ {@link #COT_SAP_XEP}.
 */
@Repository
public class CanHoQuery {

	/**
	 * Trường sắp xếp được phép và biểu thức tương ứng; trạng thái theo thứ tự nghiệp vụ.
	 */
	public static final Map<String, String> COT_SAP_XEP = Map.of("maCanHo", "c.ma_can_ho", "dienTich", "c.dien_tich",
			"trangThaiSuDung", """
					CASE c.trang_thai_su_dung WHEN 'Trống' THEN 1 WHEN 'Đang sử dụng' THEN 2 \
					WHEN 'Đang sửa chữa' THEN 3 ELSE 4 END""");

	private static final String COT = """
			SELECT c.ma_can_ho, c.ma_tang, g.so_tang, g.ma_toa_nha, t.ten_toa_nha, c.dien_tich,
			       c.trang_thai_su_dung, c.bi_tam_dung_tien_ich, c.tien_ich_ly_do, c.tien_ich_doi_luc,
			       c.tien_ich_doi_boi
			""";

	private static final String TU = """
			FROM can_ho c
			JOIN tang g ON g.ma_tang = c.ma_tang
			JOIN toa_nha t ON t.ma_toa_nha = g.ma_toa_nha
			""";

	/**
	 * Bộ lọc đã kiểm và chuẩn hóa; trường {@code null} (hoặc danh sách rỗng) là không
	 * lọc.
	 *
	 * @param maToaNha tòa được xem, không rỗng
	 * @param q mã căn chứa chuỗi này, ký tự đại diện chưa thoát
	 * @param biTamDungTienIch chỉ căn có cờ này (spec 0013, AC-11)
	 */
	public record DieuKien(List<String> maToaNha, Integer soTang, BigDecimal dienTichTu, BigDecimal dienTichDen,
			List<String> trangThaiSuDung, String q, Boolean biTamDungTienIch) {
	}

	private final JdbcClient jdbc;

	public CanHoQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public long dem(DieuKien dk) {
		Map<String, Object> thamSo = new LinkedHashMap<>();
		String sql = "SELECT count(*) " + TU + where(dk, thamSo);
		return this.jdbc.sql(sql).params(thamSo).query(Long.class).single();
	}

	/**
	 * {@code trang.sortField()} phải là một khóa của {@link #COT_SAP_XEP}; khóa phụ luôn
	 * là mã căn tăng dần.
	 */
	public List<CanHoResponse> tim(DieuKien dk, TrangYeuCau trang) {
		String cot = COT_SAP_XEP.get(trang.sortField());
		if (cot == null) {
			throw new IllegalArgumentException("Trường sắp xếp không hợp lệ: " + trang.sortField());
		}
		Map<String, Object> thamSo = new LinkedHashMap<>();
		String sql = COT + TU + where(dk, thamSo) + " ORDER BY " + cot + " " + trang.dir().name()
				+ ", c.ma_can_ho ASC LIMIT :size OFFSET :offset";
		thamSo.put("size", trang.size());
		thamSo.put("offset", trang.offset());
		return this.jdbc.sql(sql).params(thamSo).query(CanHoQuery::dong).list();
	}

	/** Một căn kèm tầng và tòa, rỗng khi không có. */
	public Optional<CanHoResponse> cua(String maCanHo) {
		return this.jdbc.sql(COT + TU + "WHERE c.ma_can_ho = :ma")
			.param("ma", maCanHo)
			.query(CanHoQuery::dong)
			.optional();
	}

	/** Tòa của một căn, để kiểm phạm vi trước khi sửa. */
	public Optional<String> maToaNhaCua(String maCanHo) {
		return this.jdbc
			.sql("SELECT g.ma_toa_nha FROM can_ho c JOIN tang g ON g.ma_tang = c.ma_tang " + "WHERE c.ma_can_ho = :ma")
			.param("ma", maCanHo)
			.query(String.class)
			.optional();
	}

	/**
	 * Mã các căn thuộc các tòa đã cho, lọc thêm số tầng khi có (spec 0015, AC-10).
	 * @param maToaNha khác rỗng
	 */
	public Set<String> maCanHoTheoToa(Collection<String> maToaNha, Integer soTang) {
		String sql = "SELECT c.ma_can_ho FROM can_ho c JOIN tang g ON g.ma_tang = c.ma_tang "
				+ "WHERE g.ma_toa_nha IN (:toa)" + ((soTang != null) ? " AND g.so_tang = :soTang" : "");
		Map<String, Object> thamSo = new LinkedHashMap<>();
		thamSo.put("toa", maToaNha);
		if (soTang != null) {
			thamSo.put("soTang", soTang);
		}
		return new LinkedHashSet<>(this.jdbc.sql(sql).params(thamSo).query(String.class).list());
	}

	public boolean tonTai(String maCanHo) {
		return this.jdbc.sql("SELECT EXISTS (SELECT 1 FROM can_ho WHERE ma_can_ho = :ma)")
			.param("ma", maCanHo)
			.query(Boolean.class)
			.single();
	}

	/** Mẫu LIKE "chứa"; dấu gạch chéo ngược, phần trăm, gạch dưới được thoát. */
	static String mau(String q) {
		return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}

	private static String where(DieuKien dk, Map<String, Object> thamSo) {
		StringBuilder sql = new StringBuilder("WHERE g.ma_toa_nha IN (:maToaNha)");
		thamSo.put("maToaNha", dk.maToaNha());
		if (dk.soTang() != null) {
			sql.append(" AND g.so_tang = :soTang");
			thamSo.put("soTang", dk.soTang());
		}
		if (dk.dienTichTu() != null) {
			sql.append(" AND c.dien_tich >= :dienTichTu");
			thamSo.put("dienTichTu", dk.dienTichTu());
		}
		if (dk.dienTichDen() != null) {
			sql.append(" AND c.dien_tich <= :dienTichDen");
			thamSo.put("dienTichDen", dk.dienTichDen());
		}
		if (dk.trangThaiSuDung() != null && !dk.trangThaiSuDung().isEmpty()) {
			sql.append(" AND c.trang_thai_su_dung IN (:trangThai)");
			thamSo.put("trangThai", dk.trangThaiSuDung());
		}
		if (dk.q() != null) {
			sql.append(" AND c.ma_can_ho LIKE :mau");
			thamSo.put("mau", mau(dk.q()));
		}
		if (dk.biTamDungTienIch() != null) {
			sql.append(" AND c.bi_tam_dung_tien_ich = :tamDung");
			thamSo.put("tamDung", dk.biTamDungTienIch());
		}
		return sql.toString();
	}

	/**
	 * Họ tên người đổi tiện ích để {@code null}; {@code NguoiDoiTienIch} điền sau (spec
	 * 0013, AC-8).
	 */
	private static CanHoResponse dong(ResultSet rs, int rowNum) throws SQLException {
		OffsetDateTime doiLuc = rs.getObject("tien_ich_doi_luc", OffsetDateTime.class);
		LanDoiTienIchResponse lanDoi = (doiLuc != null) ? new LanDoiTienIchResponse(rs.getString("tien_ich_ly_do"),
				doiLuc.toInstant(), rs.getString("tien_ich_doi_boi"), null) : null;
		return new CanHoResponse(rs.getString("ma_can_ho"), rs.getString("ma_tang"), rs.getInt("so_tang"),
				rs.getString("ma_toa_nha"), rs.getString("ten_toa_nha"), rs.getBigDecimal("dien_tich"),
				rs.getString("trang_thai_su_dung"), rs.getBoolean("bi_tam_dung_tien_ich"), lanDoi);
	}

}
