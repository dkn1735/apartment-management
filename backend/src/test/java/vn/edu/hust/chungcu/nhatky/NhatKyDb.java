package vn.edu.hust.chungcu.nhatky;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Đọc bảng {@code nhat_ky_he_thong} trong test bằng một kết nối riêng (đã commit). */
public final class NhatKyDb {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String COT = """
			SELECT ma_log, ma_nguoi_dung, loai_thao_tac, doi_tuong_tac_dong, thoi_gian_ghi,
			       chi_tiet_thay_doi::text AS chi_tiet, jsonb_typeof(chi_tiet_thay_doi) AS kieu_chi_tiet,
			       dia_chi_ip, request_id
			FROM nhat_ky_he_thong""";

	private final JdbcTemplate jdbc;

	public NhatKyDb(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Mọi dòng của một request, theo thứ tự ghi. */
	public List<Map<String, Object>> theoRequest(String requestId) {
		return this.jdbc.queryForList(COT + " WHERE request_id = ? ORDER BY ma_log", requestId);
	}

	/** Mọi dòng có người thực hiện là {@code maNguoiDung}, theo thứ tự ghi. */
	public List<Map<String, Object>> cua(String maNguoiDung) {
		return this.jdbc.queryForList(COT + " WHERE ma_nguoi_dung = ? ORDER BY ma_log", maNguoiDung);
	}

	/** Mọi cột của các dòng có {@code ma_log} lớn hơn {@code sauMaLog}, nối thành chữ. */
	public String toanBoSau(String sauMaLog) {
		List<String> dong = this.jdbc.queryForList("""
				SELECT concat_ws('|', ma_log, ma_nguoi_dung, loai_thao_tac, doi_tuong_tac_dong, thoi_gian_ghi::text,
				                 chi_tiet_thay_doi::text, dia_chi_ip, request_id)
				FROM nhat_ky_he_thong WHERE ma_log > ? ORDER BY ma_log""", String.class, sauMaLog);
		return String.join("\n", dong);
	}

	/** {@code ma_log} lớn nhất hiện có, hoặc {@code LOG00000000} khi bảng trống. */
	public String maLogCuoi() {
		return this.jdbc.queryForObject("SELECT coalesce(max(ma_log), 'LOG00000000') FROM nhat_ky_he_thong",
				String.class);
	}

	public static JsonNode chiTiet(Map<String, Object> dong) {
		Object value = dong.get("chi_tiet");
		return (value != null) ? JSON.readTree(value.toString()) : null;
	}

	public static JsonNode json(String value) {
		return JSON.readTree(value);
	}

}
