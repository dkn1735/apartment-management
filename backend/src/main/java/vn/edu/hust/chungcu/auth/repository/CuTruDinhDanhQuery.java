package vn.edu.hust.chungcu.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;

/**
 * Đọc cư dân và nhân viên BQL cho phân hệ cư trú bằng SQL thuần (spec 0014, AC-3, AC-4,
 * Value sourcing). Mỗi hàm là một câu SQL.
 */
@Repository
public class CuTruDinhDanhQuery {

	/**
	 * Từ khóa tìm cư dân đã chuẩn hóa.
	 *
	 * @param mau mẫu LIKE "chứa" đã thoát, {@code null} là không lọc
	 * @param soDienThoai chỉ chữ số, {@code null} là không so số điện thoại
	 * @param soCccd chỉ chữ số, {@code null} là không so số CCCD
	 */
	public record TuKhoa(String mau, String soDienThoai, String soCccd) {
	}

	private static final String COT = """
			SELECT c.ma_cu_dan, c.ma_nguoi_dung, n.ho_ten, n.so_dien_thoai, n.email, c.so_cccd,
			       n.trang_thai_tai_khoan
			FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung
			""";

	private final JdbcClient jdbc;

	public CuTruDinhDanhQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<String> maNvbqlCua(String maNguoiDung) {
		return this.jdbc.sql("SELECT ma_nv FROM nhan_vien_bql WHERE ma_nguoi_dung = :ma")
			.param("ma", maNguoiDung)
			.query(String.class)
			.optional();
	}

	public Optional<String> maCuDanCua(String maNguoiDung) {
		return this.jdbc.sql("SELECT ma_cu_dan FROM cu_dan WHERE ma_nguoi_dung = :ma")
			.param("ma", maNguoiDung)
			.query(String.class)
			.optional();
	}

	/**
	 * Cư dân có tài khoản {@code Hoạt động} hoặc {@code Tạm khóa}, sắp theo họ tên rồi mã
	 * cư dân (AC-3).
	 */
	public List<ThongTinCuDan> timLuaChon(TuKhoa tuKhoa, int gioiHan) {
		Map<String, Object> thamSo = new LinkedHashMap<>();
		StringBuilder sql = new StringBuilder(COT).append("WHERE n.trang_thai_tai_khoan IN ('Hoạt động', 'Tạm khóa')");
		if (tuKhoa.mau() != null) {
			sql.append(" AND ").append(khopTuKhoa(tuKhoa, thamSo));
		}
		sql.append(" ORDER BY n.ho_ten ASC, c.ma_cu_dan ASC LIMIT :gioiHan");
		thamSo.put("gioiHan", gioiHan);
		return this.jdbc.sql(sql.toString()).params(thamSo).query(CuTruDinhDanhQuery::dong).list();
	}

	/** Cư dân khớp chính xác cả hai cột, mọi trạng thái tài khoản (spec 0015, AC-3). */
	public Optional<ThongTinCuDan> theoCccdVaSdt(String soCccd, String soDienThoai) {
		return this.jdbc.sql(COT + "WHERE c.so_cccd = :cccd AND n.so_dien_thoai = :sdt")
			.param("cccd", soCccd)
			.param("sdt", soDienThoai)
			.query(CuTruDinhDanhQuery::dong)
			.optional();
	}

	/**
	 * Mã mọi cư dân khớp từ khóa có mẫu, kể cả tài khoản {@code Ngừng hoạt động}, không
	 * giới hạn số dòng (spec 0015, AC-10).
	 */
	public Set<String> maCuDanKhop(TuKhoa tuKhoa) {
		Map<String, Object> thamSo = new LinkedHashMap<>();
		String sql = "SELECT c.ma_cu_dan FROM cu_dan c JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = c.ma_nguoi_dung"
				+ " WHERE " + khopTuKhoa(tuKhoa, thamSo);
		return new LinkedHashSet<>(this.jdbc.sql(sql).params(thamSo).query(String.class).list());
	}

	/** Điều kiện {@code (... OR ...)} của từ khóa đã có mẫu; thêm tham số vào map. */
	private static String khopTuKhoa(TuKhoa tuKhoa, Map<String, Object> thamSo) {
		List<String> hoac = new ArrayList<>();
		hoac.add("unaccent(lower(n.ho_ten)) LIKE unaccent(lower(:mau)) ESCAPE '\\'");
		hoac.add("lower(n.email) LIKE lower(:mau) ESCAPE '\\'");
		hoac.add("upper(c.ma_cu_dan) LIKE upper(:mau) ESCAPE '\\'");
		thamSo.put("mau", tuKhoa.mau());
		if (tuKhoa.soDienThoai() != null) {
			hoac.add("n.so_dien_thoai LIKE :sdt");
			thamSo.put("sdt", "%" + tuKhoa.soDienThoai() + "%");
		}
		if (tuKhoa.soCccd() != null) {
			hoac.add("c.so_cccd LIKE :cccd");
			thamSo.put("cccd", "%" + tuKhoa.soCccd() + "%");
		}
		return "(" + String.join(" OR ", hoac) + ")";
	}

	public List<ThongTinCuDan> theoMa(Collection<String> maCuDan) {
		return this.jdbc.sql(COT + "WHERE c.ma_cu_dan IN (:ma)")
			.param("ma", maCuDan)
			.query(CuTruDinhDanhQuery::dong)
			.list();
	}

	/** Mã nhân viên BQL và họ tên hiện tại. */
	public Map<String, String> hoTenNvbql(Collection<String> maNv) {
		Map<String, String> ketQua = new LinkedHashMap<>();
		this.jdbc.sql("""
				SELECT b.ma_nv, n.ho_ten FROM nhan_vien_bql b
				JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = b.ma_nguoi_dung
				WHERE b.ma_nv IN (:ma)""")
			.param("ma", maNv)
			.query((rs, rowNum) -> Map.entry(rs.getString("ma_nv"), rs.getString("ho_ten")))
			.list()
			.forEach(muc -> ketQua.put(muc.getKey(), muc.getValue()));
		return ketQua;
	}

	private static ThongTinCuDan dong(ResultSet rs, int rowNum) throws SQLException {
		return new ThongTinCuDan(rs.getString("ma_cu_dan"), rs.getString("ma_nguoi_dung"), rs.getString("ho_ten"),
				rs.getString("so_dien_thoai"), rs.getString("email"), rs.getString("so_cccd"),
				rs.getString("trang_thai_tai_khoan"));
	}

}
