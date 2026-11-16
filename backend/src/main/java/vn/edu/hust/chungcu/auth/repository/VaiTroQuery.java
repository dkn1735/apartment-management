package vn.edu.hust.chungcu.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.auth.dto.ChiTietVaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroNguoiDung;

/**
 * Đọc vai trò của một người dùng từ 7 bảng vai trò bằng một câu SQL (spec 0004, mục Hồ
 * sơ). Các bảng vai trò thuộc phân hệ {@code auth} và chưa có entity ở lát này.
 */
@Repository
public class VaiTroQuery {

	private static final String SQL = """
			SELECT qtv.ma_quan_tri_vien,
			       bql.ma_nv AS bql_ma_nv, bql.chuc_vu, bql.bo_phan,
			       bqt.ma_thanh_vien, bqt.nhiem_ky, bqt.vai_tro AS bqt_vai_tro,
			       kt.ma_nv AS kt_ma_nv, kt.pham_vi_phu_trach,
			       ktv.ma_nv AS ktv_ma_nv, ktv.chuyen_mon_ky_thuat,
			       bv.ma_nv AS bv_ma_nv, bv.vi_tri_truc,
			       cd.ma_cu_dan
			FROM nguoi_dung_he_thong n
			LEFT JOIN quan_tri_vien qtv ON qtv.ma_nguoi_dung = n.ma_nguoi_dung
			LEFT JOIN nhan_vien_bql bql ON bql.ma_nguoi_dung = n.ma_nguoi_dung
			LEFT JOIN thanh_vien_ban_quan_tri bqt ON bqt.ma_nguoi_dung = n.ma_nguoi_dung
			LEFT JOIN ke_toan kt ON kt.ma_nguoi_dung = n.ma_nguoi_dung
			LEFT JOIN nhan_vien_ky_thuat ktv ON ktv.ma_nguoi_dung = n.ma_nguoi_dung
			LEFT JOIN nhan_vien_bao_ve bv ON bv.ma_nguoi_dung = n.ma_nguoi_dung
			LEFT JOIN cu_dan cd ON cd.ma_nguoi_dung = n.ma_nguoi_dung
			WHERE n.ma_nguoi_dung = :ma""";

	private final JdbcClient jdbc;

	public VaiTroQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Vai trò của người dùng; người dùng không tồn tại thì như không có vai trò nào. */
	public VaiTroNguoiDung cua(String maNguoiDung) {
		return this.jdbc.sql(SQL)
			.param("ma", maNguoiDung)
			.query((rs, rowNum) -> map(rs))
			.optional()
			.orElse(VaiTroNguoiDung.KHONG_CO);
	}

	private static VaiTroNguoiDung map(ResultSet rs) throws SQLException {
		ChiTietVaiTro chiTiet = new ChiTietVaiTro(
				(rs.getString("ma_quan_tri_vien") != null)
						? new ChiTietVaiTro.VaiTroQuanTriVien(rs.getString("ma_quan_tri_vien")) : null,
				(rs.getString("bql_ma_nv") != null) ? new ChiTietVaiTro.VaiTroBanQuanLy(rs.getString("bql_ma_nv"),
						rs.getString("chuc_vu"), rs.getString("bo_phan")) : null,
				(rs.getString("ma_thanh_vien") != null) ? new ChiTietVaiTro.VaiTroBanQuanTri(
						rs.getString("ma_thanh_vien"), rs.getString("nhiem_ky"), rs.getString("bqt_vai_tro")) : null,
				(rs.getString("kt_ma_nv") != null)
						? new ChiTietVaiTro.VaiTroKeToan(rs.getString("kt_ma_nv"), rs.getString("pham_vi_phu_trach"))
						: null,
				(rs.getString("ktv_ma_nv") != null) ? new ChiTietVaiTro.VaiTroKyThuat(rs.getString("ktv_ma_nv"),
						rs.getString("chuyen_mon_ky_thuat")) : null,
				(rs.getString("bv_ma_nv") != null)
						? new ChiTietVaiTro.VaiTroBaoVe(rs.getString("bv_ma_nv"), rs.getString("vi_tri_truc")) : null,
				(rs.getString("ma_cu_dan") != null) ? new ChiTietVaiTro.VaiTroCuDan(rs.getString("ma_cu_dan")) : null);
		return new VaiTroNguoiDung(maVaiTro(chiTiet), chiTiet);
	}

	/** Mã vai trò theo đúng thứ tự khai báo của {@link VaiTro}. */
	private static List<VaiTro> maVaiTro(ChiTietVaiTro c) {
		List<VaiTro> result = new ArrayList<>();
		addIf(result, c.quanTriVien() != null, VaiTro.QUAN_TRI_VIEN);
		addIf(result, c.banQuanLy() != null, VaiTro.BAN_QUAN_LY);
		addIf(result, c.banQuanTri() != null, VaiTro.BAN_QUAN_TRI);
		addIf(result, c.keToan() != null, VaiTro.KE_TOAN);
		addIf(result, c.kyThuat() != null, VaiTro.KY_THUAT);
		addIf(result, c.baoVe() != null, VaiTro.BAO_VE);
		addIf(result, c.cuDan() != null, VaiTro.CU_DAN);
		return List.copyOf(result);
	}

	private static void addIf(List<VaiTro> list, boolean condition, VaiTro value) {
		if (condition) {
			list.add(value);
		}
	}

}
