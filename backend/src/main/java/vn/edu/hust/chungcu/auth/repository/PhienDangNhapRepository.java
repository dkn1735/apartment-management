package vn.edu.hust.chungcu.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import vn.edu.hust.chungcu.auth.entity.PhienDangNhap;

public interface PhienDangNhapRepository extends JpaRepository<PhienDangNhap, Long> {

	/**
	 * Họ phiên của token, đọc không khóa và không nạp entity, chỉ để biết khóa họ nào.
	 * Đọc dòng thật sau khi đã có khóa bằng {@link #findByTokenHash}.
	 */
	@Query("select p.maHoPhien from PhienDangNhap p where p.tokenHash = :hash")
	Optional<UUID> timHoPhien(String hash);

	/** Như {@link #timHoPhien}, kèm chủ họ phiên để ghi nhật ký (spec 0007, AC-4). */
	@Query("select new vn.edu.hust.chungcu.auth.repository.HoPhienCuaToken(p.maHoPhien, p.maNguoiDung) "
			+ "from PhienDangNhap p where p.tokenHash = :hash")
	Optional<HoPhienCuaToken> timHoPhienVaNguoiDung(String hash);

	Optional<PhienDangNhap> findByTokenHash(String hash);

	/**
	 * Khóa tư vấn của PostgreSQL theo họ phiên, tự nhả khi transaction kết thúc. Mọi thao
	 * tác ghi lên một họ lấy khóa này trước (spec 0005, Key invariants). Chờ tối đa 3
	 * giây; quá giờ thì lỗi đi lên thành 500.
	 */
	default void khoaHo(UUID maHoPhien) {
		datLockTimeout();
		khoaHoTheoKhoa(maHoPhien.toString());
	}

	@Query(value = "SELECT set_config('lock_timeout', '3s', true)", nativeQuery = true)
	String datLockTimeout();

	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(CAST(:ma AS text), 0))", nativeQuery = true)
	Integer khoaHoTheoKhoa(String ma);

	/** Thu hồi mọi dòng chưa thu hồi của họ; trả số dòng đã thu hồi. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update PhienDangNhap p set p.thuHoiLuc = :luc, p.lyDoThuHoi = :lyDo "
			+ "where p.maHoPhien = :maHoPhien and p.thuHoiLuc is null")
	int thuHoiHo(UUID maHoPhien, Instant luc, String lyDo);

	/**
	 * Dòng còn hiệu lực (chưa thu hồi, chưa hết hạn) của họ; rỗng nghĩa là họ đã chết.
	 * Index duy nhất một phần bảo đảm tối đa một dòng.
	 */
	@Query("select p from PhienDangNhap p "
			+ "where p.maHoPhien = :maHoPhien and p.thuHoiLuc is null and p.hetHanLuc > :now")
	Optional<PhienDangNhap> timConHieuLuc(UUID maHoPhien, Instant now);

	/**
	 * Các họ phiên còn dòng hiệu lực của một người dùng, sắp theo mã họ để mọi nơi khóa
	 * nhiều họ cùng một thứ tự.
	 */
	@Query("select distinct p.maHoPhien from PhienDangNhap p where p.maNguoiDung = :maNguoiDung "
			+ "and p.thuHoiLuc is null and p.hetHanLuc > :now order by p.maHoPhien")
	List<UUID> hoConHieuLucCua(String maNguoiDung, Instant now);

	/** Xóa dòng đã hết hạn hoặc đã thu hồi trước {@code moc}; trả số dòng đã xóa. */
	@Modifying
	@Query("delete from PhienDangNhap p where p.hetHanLuc < :moc or p.thuHoiLuc < :moc")
	int xoaCu(Instant moc);

}
