package vn.edu.hust.chungcu.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroTaiKhoan;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/**
 * Đọc tài khoản cho màn quản lý (spec 0008, AC-5, AC-6) bằng SQL thuần. Chuỗi của người
 * dùng chỉ đi vào SQL dưới dạng tham số bind; tên bảng và cột lấy từ {@link BangVaiTro}.
 */
@Repository
public class TaiKhoanQuery {

	/**
	 * Tài khoản cư dân thuần: có dòng {@code cu_dan} và không có dòng ở sáu bảng vai trò
	 * còn lại (spec 0008, Thuật ngữ).
	 */
	static final String CU_DAN_THUAN = """
			EXISTS (SELECT 1 FROM cu_dan x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)
			AND NOT EXISTS (SELECT 1 FROM quan_tri_vien x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)
			AND NOT EXISTS (SELECT 1 FROM nhan_vien_bql x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)
			AND NOT EXISTS (SELECT 1 FROM thanh_vien_ban_quan_tri x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)
			AND NOT EXISTS (SELECT 1 FROM ke_toan x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)
			AND NOT EXISTS (SELECT 1 FROM nhan_vien_ky_thuat x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)
			AND NOT EXISTS (SELECT 1 FROM nhan_vien_bao_ve x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)""";

	private static final String COT_CHUNG = """
			n.ma_nguoi_dung, n.ho_ten, n.so_dien_thoai, n.email, n.trang_thai_tai_khoan, n.phai_doi_mat_khau,
			n.dang_nhap_cuoi_luc, n.doi_mat_khau_luc, n.tao_luc, n.cap_nhat_luc, n.phien_ban""";

	private static final String VAI_TRO_THEO_LO = vaiTroTheoLo();

	private final JdbcClient jdbc;

	public TaiKhoanQuery(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Một dòng {@code nguoi_dung_he_thong} như trong DB. */
	public record DongTaiKhoan(String maNguoiDung, String hoTen, String soDienThoai, String email,
			String trangThaiTaiKhoan, boolean phaiDoiMatKhau, Instant dangNhapCuoiLuc, Instant doiMatKhauLuc,
			Instant taoLuc, Instant capNhatLuc, long phienBan) {

		@Override
		public String toString() {
			return "DongTaiKhoan[maNguoiDung=" + this.maNguoiDung + ", ***]";
		}

	}

	/**
	 * Bộ lọc danh sách đã kiểm; trường {@code null} là không lọc. {@code qSo} là
	 * {@code q} đã chuẩn hóa như số điện thoại, chỉ gồm chữ số, hoặc {@code null} khi
	 * {@code q} không giống số điện thoại.
	 */
	public record BoLocTaiKhoan(boolean chiCuDanThuan, String q, String qSo, VaiTro vaiTro, String trangThai) {
	}

	public Optional<DongTaiKhoan> tim(String maNguoiDung) {
		return this.jdbc.sql("SELECT " + COT_CHUNG + " FROM nguoi_dung_he_thong n WHERE n.ma_nguoi_dung = :ma")
			.param("ma", maNguoiDung)
			.query((rs, rowNum) -> dong(rs))
			.optional();
	}

	/**
	 * Đọc và khóa dòng tài khoản tới hết transaction, để mọi thao tác ghi lên cùng tài
	 * khoản chạy lần lượt (spec 0008, AC-15). Phải gọi trong transaction. Dùng
	 * {@code FOR NO KEY UPDATE}, không phải {@code FOR UPDATE}: khóa ngoại của
	 * {@code phien_dang_nhap} lấy {@code FOR KEY SHARE} trên dòng này khi làm mới phiên,
	 * và {@code FOR UPDATE} sẽ chặn nó, gây deadlock với khóa họ phiên.
	 */
	public Optional<DongTaiKhoan> khoa(String maNguoiDung) {
		return this.jdbc
			.sql("SELECT " + COT_CHUNG + " FROM nguoi_dung_he_thong n WHERE n.ma_nguoi_dung = :ma FOR NO KEY UPDATE")
			.param("ma", maNguoiDung)
			.query((rs, rowNum) -> dong(rs))
			.optional();
	}

	/** Số Admin đang hoạt động, không tính {@code ngoaiTru} (spec 0008, AC-11). */
	public long demAdminHoatDongTru(String ngoaiTru) {
		return this.jdbc.sql("""
				SELECT count(*) FROM quan_tri_vien q
				JOIN nguoi_dung_he_thong n ON n.ma_nguoi_dung = q.ma_nguoi_dung
				WHERE n.trang_thai_tai_khoan = 'Hoạt động' AND n.ma_nguoi_dung <> :ma""")
			.param("ma", ngoaiTru)
			.query(Long.class)
			.single();
	}

	public long dem(BoLocTaiKhoan boLoc) {
		DieuKien dk = DieuKien.tu(boLoc);
		return this.jdbc.sql("SELECT count(*) FROM nguoi_dung_he_thong n " + dk.where())
			.params(dk.params())
			.query(Long.class)
			.single();
	}

	/** Một trang, mới tạo trước; tiêu chí phụ là mã cùng chiều (AC-5). */
	public List<DongTaiKhoan> danhSach(BoLocTaiKhoan boLoc, TrangYeuCau trang) {
		DieuKien dk = DieuKien.tu(boLoc);
		String huong = (trang.dir() == Sort.Direction.ASC) ? "ASC" : "DESC";
		String sql = "SELECT " + COT_CHUNG + " FROM nguoi_dung_he_thong n " + dk.where() + " ORDER BY n.tao_luc "
				+ huong + ", n.ma_nguoi_dung " + huong + " LIMIT :size OFFSET :offset";
		return this.jdbc.sql(sql)
			.params(dk.params())
			.param("size", trang.size())
			.param("offset", trang.offset())
			.query((rs, rowNum) -> dong(rs))
			.list();
	}

	/**
	 * Vai trò của nhiều tài khoản trong một truy vấn, mỗi danh sách theo thứ tự enum
	 * {@link VaiTro}. Tài khoản không có vai trò nào cho ra danh sách rỗng.
	 */
	public Map<String, List<VaiTroTaiKhoan>> vaiTroCua(Collection<String> maNguoiDung) {
		Map<String, List<VaiTroTaiKhoan>> ketQua = new LinkedHashMap<>();
		maNguoiDung.forEach((ma) -> ketQua.put(ma, new ArrayList<>()));
		if (maNguoiDung.isEmpty()) {
			return ketQua;
		}
		this.jdbc.sql(VAI_TRO_THEO_LO).param("ma", maNguoiDung).query((rs, rowNum) -> {
			ketQua.get(rs.getString("ma_nguoi_dung")).add(vaiTro(rs));
			return null;
		}).list();
		ketQua.values().forEach((ds) -> ds.sort(Comparator.comparing(VaiTroTaiKhoan::vaiTro)));
		return ketQua.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, (e) -> List.copyOf(e.getValue()), (a, b) -> a,
					LinkedHashMap::new));
	}

	/**
	 * Trường nào đã thuộc tài khoản khác (AC-3), theo thứ tự {@code soDienThoai},
	 * {@code email}, {@code soCccd}. Giá trị {@code null} thì bỏ qua; {@code ngoaiTru} là
	 * tài khoản đang sửa.
	 */
	public List<String> truongTrung(String soDienThoai, String email, String soCccd, String ngoaiTru) {
		List<String> trung = new ArrayList<>();
		if (soDienThoai != null && tonTai(
				"SELECT 1 FROM nguoi_dung_he_thong WHERE so_dien_thoai = :v AND ma_nguoi_dung IS DISTINCT FROM :ngoaiTru",
				soDienThoai, ngoaiTru)) {
			trung.add("soDienThoai");
		}
		if (email != null && tonTai(
				"SELECT 1 FROM nguoi_dung_he_thong WHERE email = :v AND ma_nguoi_dung IS DISTINCT FROM :ngoaiTru",
				email, ngoaiTru)) {
			trung.add("email");
		}
		if (soCccd != null
				&& tonTai("SELECT 1 FROM cu_dan WHERE so_cccd = :v AND ma_nguoi_dung IS DISTINCT FROM :ngoaiTru",
						soCccd, ngoaiTru)) {
			trung.add("soCccd");
		}
		return trung;
	}

	private boolean tonTai(String sql, String value, String ngoaiTru) {
		return !this.jdbc.sql(sql + " LIMIT 1")
			.param("v", value)
			.param("ngoaiTru", ngoaiTru)
			.query(Integer.class)
			.list()
			.isEmpty();
	}

	private static DongTaiKhoan dong(ResultSet rs) throws SQLException {
		return new DongTaiKhoan(rs.getString("ma_nguoi_dung"), rs.getString("ho_ten"), rs.getString("so_dien_thoai"),
				rs.getString("email"), rs.getString("trang_thai_tai_khoan"), rs.getBoolean("phai_doi_mat_khau"),
				instant(rs, "dang_nhap_cuoi_luc"), instant(rs, "doi_mat_khau_luc"), instant(rs, "tao_luc"),
				instant(rs, "cap_nhat_luc"), rs.getLong("phien_ban"));
	}

	private static Instant instant(ResultSet rs, String cot) throws SQLException {
		OffsetDateTime value = rs.getObject(cot, OffsetDateTime.class);
		return (value != null) ? value.toInstant() : null;
	}

	private static VaiTroTaiKhoan vaiTro(ResultSet rs) throws SQLException {
		return new VaiTroTaiKhoan(VaiTro.valueOf(rs.getString("vai_tro")), rs.getString("ma_vai_tro"),
				rs.getString("chuc_vu"), rs.getString("bo_phan"), rs.getString("nhiem_ky"), rs.getString("vai_tro_bqt"),
				rs.getString("pham_vi_phu_trach"), rs.getString("chuyen_mon_ky_thuat"), rs.getString("vi_tri_truc"),
				rs.getString("so_cccd"));
	}

	/** {@code UNION ALL} trên 7 bảng con, cột vắng là {@code NULL}. */
	private static String vaiTroTheoLo() {
		List<String> phan = new ArrayList<>();
		for (BangVaiTro bang : BangVaiTro.values()) {
			StringBuilder sb = new StringBuilder("SELECT ma_nguoi_dung, '").append(bang.vaiTro().name())
				.append("' AS vai_tro, ")
				.append(bang.cotMa())
				.append(" AS ma_vai_tro");
			for (TruongVaiTro truong : TruongVaiTro.values()) {
				String alias = alias(truong);
				sb.append(", ")
					.append(bang.truong().contains(truong) ? truong.cot() + "::text" : "NULL::text")
					.append(" AS ")
					.append(alias);
			}
			sb.append(" FROM ").append(bang.bang()).append(" WHERE ma_nguoi_dung IN (:ma)");
			phan.add(sb.toString());
		}
		return String.join("\nUNION ALL\n", phan);
	}

	private static String alias(TruongVaiTro truong) {
		return (truong == TruongVaiTro.VAI_TRO_BQT) ? "vai_tro_bqt" : truong.cot();
	}

	private record DieuKien(String where, Map<String, Object> params) {

		static DieuKien tu(BoLocTaiKhoan boLoc) {
			List<String> dieuKien = new ArrayList<>();
			Map<String, Object> params = new LinkedHashMap<>();
			if (boLoc.chiCuDanThuan()) {
				dieuKien.add("(" + CU_DAN_THUAN + ")");
			}
			if (boLoc.q() != null) {
				String mau = "%" + thoat(boLoc.q()) + "%";
				List<String> hoac = new ArrayList<>();
				hoac.add("unaccent(lower(n.ho_ten)) LIKE unaccent(lower(:qTen)) ESCAPE '\\'");
				hoac.add("n.email LIKE :qEmail ESCAPE '\\'");
				hoac.add("n.ma_nguoi_dung LIKE :qMa ESCAPE '\\'");
				params.put("qTen", mau);
				params.put("qEmail", mau.toLowerCase(Locale.ROOT));
				params.put("qMa", mau.toUpperCase(Locale.ROOT));
				if (boLoc.qSo() != null) {
					hoac.add("n.so_dien_thoai LIKE :qSo");
					params.put("qSo", "%" + boLoc.qSo() + "%");
				}
				dieuKien.add("(" + String.join(" OR ", hoac) + ")");
			}
			if (boLoc.vaiTro() != null) {
				dieuKien.add("EXISTS (SELECT 1 FROM " + BangVaiTro.cua(boLoc.vaiTro()).bang()
						+ " x WHERE x.ma_nguoi_dung = n.ma_nguoi_dung)");
			}
			if (boLoc.trangThai() != null) {
				dieuKien.add("n.trang_thai_tai_khoan = :trangThai");
				params.put("trangThai", boLoc.trangThai());
			}
			String where = dieuKien.isEmpty() ? "" : "WHERE " + String.join(" AND ", dieuKien);
			return new DieuKien(where, params);
		}

		/** Thoát {@code \}, {@code %}, {@code _} để so như chữ thường (AC-5). */
		private static String thoat(String q) {
			return q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		}

	}

}
