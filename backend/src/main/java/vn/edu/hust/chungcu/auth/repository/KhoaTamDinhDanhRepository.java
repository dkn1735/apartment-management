package vn.edu.hust.chungcu.auth.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bộ đếm đăng nhập sai cho định danh không có tài khoản, bảng
 * {@code dang_nhap_sai_dinh_danh} (spec 0009, AC-5). Khóa là SHA-256 hex của định danh đã
 * chuẩn hóa; không câu nào nhận định danh gốc.
 */
@Repository
public class KhoaTamDinhDanhRepository {

	private static final String GIU_LUOT = CauGiuLuot.sql("dang_nhap_sai_dinh_danh", "dinh_danh_hash", "so_lan_sai");

	private final JdbcClient jdbc;

	public KhoaTamDinhDanhRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Chắc chắn có dòng rồi giữ lượt bằng đúng câu của tài khoản thật, nên cùng ngữ nghĩa
	 * khi chạy song song.
	 */
	public Optional<KetQuaGiuLuot> giuLuot(String dinhDanhHash, Instant now, int n, Duration thoiGian) {
		this.jdbc.sql("""
				INSERT INTO dang_nhap_sai_dinh_danh (dinh_danh_hash) VALUES (:hash)
				ON CONFLICT (dinh_danh_hash) DO NOTHING""").param("hash", dinhDanhHash).update();
		return CauGiuLuot.chay(this.jdbc, GIU_LUOT, dinhDanhHash, now, n, thoiGian);
	}

	/** Xóa dòng của các định danh vừa có chủ (AC-19). */
	public int xoa(Collection<String> dinhDanhHash) {
		if (dinhDanhHash.isEmpty()) {
			return 0;
		}
		return this.jdbc.sql("DELETE FROM dang_nhap_sai_dinh_danh WHERE dinh_danh_hash IN (:hash)")
			.param("hash", dinhDanhHash)
			.update();
	}

	/**
	 * Xóa dòng đã hết tác dụng (AC-12): lần sai cuối cũ hơn {@code moc} và không đang
	 * khóa ở {@code now}. Bộ đếm hiệu lực của chúng là 0, nên xóa không đổi hành vi.
	 */
	public int xoaHetTacDung(Instant moc, Instant now) {
		return this.jdbc.sql("""
				DELETE FROM dang_nhap_sai_dinh_danh
				WHERE lan_sai_cuoi_luc <= CAST(:moc AS timestamptz)
				  AND (khoa_tam_den IS NULL OR khoa_tam_den <= CAST(:now AS timestamptz))""")
			.param("moc", CauGiuLuot.utc(moc))
			.param("now", CauGiuLuot.utc(now))
			.update();
	}

}
