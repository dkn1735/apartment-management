package vn.edu.hust.chungcu.nhatky.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;
import vn.edu.hust.chungcu.nhatky.dto.BoLocNhatKy;

/**
 * Đọc bảng {@code nhat_ky_he_thong} cho danh sách (spec 0007, AC-11, AC-12), bằng SQL
 * thuần. Chuỗi của người dùng chỉ đi vào SQL dưới dạng tham số bind.
 */
@Repository
public class NhatKyQuery {

	/** Trường sắp xếp được phép (AC-11). */
	public static final Set<String> TRUONG_SAP_XEP = Set.of("thoiGianGhi", "loaiThaoTac");

	private static final String DANH_SACH = """
			SELECT ma_log, ma_nguoi_dung, loai_thao_tac, doi_tuong_tac_dong, chi_tiet_thay_doi::text AS chi_tiet,
			       dia_chi_ip, request_id, thoi_gian_ghi
			FROM nhat_ky_he_thong
			%s
			ORDER BY %s
			LIMIT :size OFFSET :offset""";

	private final JdbcClient jdbc;

	public NhatKyQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public long dem(BoLocNhatKy boLoc) {
		DieuKien dk = DieuKien.tu(boLoc);
		return this.jdbc.sql("SELECT count(*) FROM nhat_ky_he_thong " + dk.where())
			.params(dk.params())
			.query(Long.class)
			.single();
	}

	/** {@code trang.sortField()} phải thuộc {@link #TRUONG_SAP_XEP}. */
	public List<DongDoc> danhSach(BoLocNhatKy boLoc, TrangYeuCau trang) {
		DieuKien dk = DieuKien.tu(boLoc);
		return this.jdbc.sql(DANH_SACH.formatted(dk.where(), sapXep(trang)))
			.params(dk.params())
			.param("size", trang.size())
			.param("offset", trang.offset())
			.query((rs, rowNum) -> new DongDoc(rs.getString("ma_log"), rs.getString("ma_nguoi_dung"),
					rs.getString("loai_thao_tac"), rs.getString("doi_tuong_tac_dong"), rs.getString("chi_tiet"),
					rs.getString("dia_chi_ip"), rs.getString("request_id"),
					rs.getObject("thoi_gian_ghi", OffsetDateTime.class).toInstant()))
			.list();
	}

	/**
	 * Tiêu chí phụ luôn là thời gian giảm dần rồi mã giảm dần; riêng
	 * {@code thoiGianGhi,asc} thì mã tăng dần. {@code loaiThaoTac} sắp theo mã đã lưu
	 * (collation {@code C}), không theo nhãn.
	 */
	private static String sapXep(TrangYeuCau trang) {
		String huong = (trang.dir() == Sort.Direction.ASC) ? "ASC" : "DESC";
		return switch (trang.sortField()) {
			case "thoiGianGhi" -> "thoi_gian_ghi " + huong + ", ma_log " + huong;
			case "loaiThaoTac" -> "loai_thao_tac COLLATE \"C\" " + huong + ", thoi_gian_ghi DESC, ma_log DESC";
			default -> throw new IllegalArgumentException("Trường sắp xếp không hợp lệ: " + trang.sortField());
		};
	}

	/** Một dòng như trong DB; {@code chiTietJson} là JSON object dạng chữ hoặc null. */
	public record DongDoc(String maLog, String maNguoiDung, String loaiThaoTac, String doiTuongTacDong,
			String chiTietJson, String diaChiIp, String requestId, Instant thoiGianGhi) {

	}

	private record DieuKien(String where, Map<String, Object> params) {

		static DieuKien tu(BoLocNhatKy boLoc) {
			List<String> dieuKien = new ArrayList<>();
			Map<String, Object> params = new LinkedHashMap<>();
			if (boLoc.maNguoiDung() != null) {
				dieuKien.add("ma_nguoi_dung = :maNguoiDung");
				params.put("maNguoiDung", boLoc.maNguoiDung());
			}
			if (boLoc.loaiThaoTac() != null) {
				dieuKien.add("loai_thao_tac = :loaiThaoTac");
				params.put("loaiThaoTac", boLoc.loaiThaoTac().name());
			}
			// pgjdbc không bind Instant
			if (boLoc.tu() != null) {
				dieuKien.add("thoi_gian_ghi >= :tu");
				params.put("tu", OffsetDateTime.ofInstant(boLoc.tu(), ZoneOffset.UTC));
			}
			if (boLoc.den() != null) {
				dieuKien.add("thoi_gian_ghi < :den");
				params.put("den", OffsetDateTime.ofInstant(boLoc.den(), ZoneOffset.UTC));
			}
			String where = dieuKien.isEmpty() ? "" : "WHERE " + String.join(" AND ", dieuKien);
			return new DieuKien(where, params);
		}

	}

}
