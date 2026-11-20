package vn.edu.hust.chungcu.nhatky.repository;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Ghi bảng {@code nhat_ky_he_thong}, chỉ thêm (spec 0007, AC-9). Không có hàm sửa hay
 * xóa; DB cũng chặn bằng trigger. {@code ma_log} do DB sinh.
 */
@Repository
public class NhatKyGhi {

	private static final String THEM = """
			INSERT INTO nhat_ky_he_thong (ma_nguoi_dung, loai_thao_tac, doi_tuong_tac_dong, thoi_gian_ghi,
			    chi_tiet_thay_doi, dia_chi_ip, request_id)
			VALUES (:maNguoiDung, :loaiThaoTac, :doiTuong, :thoiGianGhi, CAST(:chiTiet AS jsonb), :diaChiIp, :requestId)""";

	private final JdbcClient jdbc;

	public NhatKyGhi(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void them(DongNhatKy dong) {
		this.jdbc.sql(THEM)
			.param("maNguoiDung", dong.maNguoiDung(), Types.VARCHAR)
			.param("loaiThaoTac", dong.loaiThaoTac())
			.param("doiTuong", dong.doiTuongTacDong())
			// pgjdbc không bind Instant
			.param("thoiGianGhi", OffsetDateTime.ofInstant(dong.thoiGianGhi(), ZoneOffset.UTC))
			.param("chiTiet", dong.chiTietJson(), Types.VARCHAR)
			.param("diaChiIp", dong.diaChiIp(), Types.VARCHAR)
			.param("requestId", dong.requestId(), Types.VARCHAR)
			.update();
	}

	/** Một dòng đã sẵn sàng để chèn; {@code chiTietJson} là JSON object hoặc null. */
	public record DongNhatKy(String maNguoiDung, String loaiThaoTac, String doiTuongTacDong, Instant thoiGianGhi,
			String chiTietJson, String diaChiIp, String requestId) {

	}

}
