package vn.edu.hust.chungcu.cutru.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Ghi hồ sơ cư trú bằng SQL thuần (spec 0014). Mọi lệnh chạy trong transaction của
 * service, sau khi service đã khóa dòng {@code can_ho}; lỗi ràng buộc để nguyên cho
 * service đổi nghĩa.
 */
@Repository
public class HoSoCuTruGhi {

	/** Dữ liệu một hồ sơ mới hoặc giá trị mới của các trường sửa được. */
	public record GiaTri(String vaiTroCuTru, String tinhTrangCuTru, LocalDate ngayBatDau, LocalDate ngayKetThuc,
			String trangThaiHoSo) {
	}

	/** Dòng vừa được job chuyển sang {@code Hết hiệu lực} (AC-11). */
	public record DaHetHan(String maHoSo, String maCuDan, LocalDate ngayKetThuc) {
	}

	private final JdbcClient jdbc;

	public HoSoCuTruGhi(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Thêm một dòng, trả mã PostgreSQL sinh ra. */
	public String them(String maCanHo, String maCuDan, String maNvbql, GiaTri giaTri, Instant luc) {
		OffsetDateTime thoiDiem = luc.atOffset(ZoneOffset.UTC);
		return this.jdbc.sql("""
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
				    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so, tao_luc, cap_nhat_luc)
				VALUES (:maCanHo, :maCuDan, :maNv, :vaiTro, :tinhTrang, :batDau, CAST(:ketThuc AS date), :trangThai,
				    :luc, :luc)
				RETURNING ma_ho_so""")
			.param("maCanHo", maCanHo)
			.param("maCuDan", maCuDan)
			.param("maNv", maNvbql)
			.param("vaiTro", giaTri.vaiTroCuTru())
			.param("tinhTrang", giaTri.tinhTrangCuTru())
			.param("batDau", giaTri.ngayBatDau())
			.param("ketThuc", giaTri.ngayKetThuc())
			.param("trangThai", giaTri.trangThaiHoSo())
			.param("luc", thoiDiem)
			.query(String.class)
			.single();
	}

	/**
	 * Thêm một đề nghị {@code Chờ xác minh} chưa có người xác minh (spec 0015, AC-1); trả
	 * mã PostgreSQL sinh ra.
	 */
	public String themDeNghi(String maCanHo, String maCuDan, String maCuDanDeNghi, GiaTri giaTri, String ghiChu,
			Instant luc) {
		return this.jdbc.sql("""
				INSERT INTO ho_so_cu_tru (ma_can_ho, ma_cu_dan, ma_nvbql_xac_minh, vai_tro_cu_tru, tinh_trang_cu_tru,
				    ngay_bat_dau, ngay_ket_thuc, trang_thai_ho_so, tao_luc, cap_nhat_luc, ma_cu_dan_de_nghi,
				    ghi_chu_de_nghi)
				VALUES (:maCanHo, :maCuDan, NULL, :vaiTro, :tinhTrang, :batDau, CAST(:ketThuc AS date), :trangThai,
				    :luc, :luc, :deNghi, CAST(:ghiChu AS varchar))
				RETURNING ma_ho_so""")
			.param("maCanHo", maCanHo)
			.param("maCuDan", maCuDan)
			.param("vaiTro", giaTri.vaiTroCuTru())
			.param("tinhTrang", giaTri.tinhTrangCuTru())
			.param("batDau", giaTri.ngayBatDau())
			.param("ketThuc", giaTri.ngayKetThuc())
			.param("trangThai", giaTri.trangThaiHoSo())
			.param("luc", luc.atOffset(ZoneOffset.UTC))
			.param("deNghi", maCuDanDeNghi)
			.param("ghiChu", ghiChu)
			.query(String.class)
			.single();
	}

	/**
	 * Kết thúc xử lý một đề nghị đang chờ (spec 0015, AC-4 đến AC-6): duyệt, từ chối hoặc
	 * hủy. Gọi sau {@link #khoaDong} trong cùng transaction.
	 * @param maNvbql {@code null} khi người gửi tự hủy
	 * @param lyDoTuChoi chỉ có khi từ chối
	 */
	public void ketLuanDeNghi(String maHoSo, String trangThaiMoi, String maNvbql, String lyDoTuChoi, Instant luc) {
		this.jdbc.sql("""
				UPDATE ho_so_cu_tru SET trang_thai_ho_so = :trangThai, ma_nvbql_xac_minh = CAST(:maNv AS varchar),
				       ly_do_tu_choi = CAST(:lyDo AS varchar), cap_nhat_luc = :luc
				WHERE ma_ho_so = :ma""")
			.param("trangThai", trangThaiMoi)
			.param("maNv", maNvbql)
			.param("lyDo", lyDoTuChoi)
			.param("luc", luc.atOffset(ZoneOffset.UTC))
			.param("ma", maHoSo)
			.update();
	}

	/** Khóa dòng hồ sơ tới hết transaction; gọi sau khi đã khóa dòng căn (AC-7). */
	public Optional<HoSoCuTruDong> khoaDong(String maHoSo) {
		return this.jdbc.sql(HoSoCuTruDong.COT + "WHERE ma_ho_so = :ma FOR UPDATE")
			.param("ma", maHoSo)
			.query(HoSoCuTruDong::doc)
			.optional();
	}

	/** Gọi sau {@link #khoaDong} trong cùng transaction. */
	public void capNhat(String maHoSo, GiaTri giaTri, String maNvbql, Instant luc) {
		this.jdbc.sql("""
				UPDATE ho_so_cu_tru SET vai_tro_cu_tru = :vaiTro, tinh_trang_cu_tru = :tinhTrang,
				       ngay_bat_dau = :batDau, ngay_ket_thuc = CAST(:ketThuc AS date), trang_thai_ho_so = :trangThai,
				       ma_nvbql_xac_minh = :maNv, cap_nhat_luc = :luc
				WHERE ma_ho_so = :ma""")
			.param("vaiTro", giaTri.vaiTroCuTru())
			.param("tinhTrang", giaTri.tinhTrangCuTru())
			.param("batDau", giaTri.ngayBatDau())
			.param("ketThuc", giaTri.ngayKetThuc())
			.param("trangThai", giaTri.trangThaiHoSo())
			.param("maNv", maNvbql)
			.param("luc", luc.atOffset(ZoneOffset.UTC))
			.param("ma", maHoSo)
			.update();
	}

	/**
	 * Chuyển các hồ sơ hợp lệ đã qua ngày kết thúc của một căn sang {@code Hết hiệu lực},
	 * giữ nguyên người xác minh (AC-11). Trả các dòng đã đổi theo mã tăng dần.
	 */
	public List<DaHetHan> hetHan(String maCanHo, LocalDate homNay, Instant luc) {
		return this.jdbc.sql("""
				UPDATE ho_so_cu_tru SET trang_thai_ho_so = 'Hết hiệu lực', cap_nhat_luc = :luc
				WHERE ma_can_ho = :ma AND trang_thai_ho_so = 'Hợp lệ' AND ngay_ket_thuc < :homNay
				RETURNING ma_ho_so, ma_cu_dan, ngay_ket_thuc""")
			.param("luc", luc.atOffset(ZoneOffset.UTC))
			.param("ma", maCanHo)
			.param("homNay", homNay)
			.query((rs, rowNum) -> new DaHetHan(rs.getString("ma_ho_so"), rs.getString("ma_cu_dan"),
					rs.getObject("ngay_ket_thuc", LocalDate.class)))
			.list()
			.stream()
			.sorted(Comparator.comparing(DaHetHan::maHoSo))
			.toList();
	}

}
