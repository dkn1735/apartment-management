package vn.edu.hust.chungcu.auth.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;

public interface NguoiDungHeThongRepository extends JpaRepository<NguoiDungHeThong, String> {

	Optional<NguoiDungHeThong> findBySoDienThoai(String soDienThoai);

	Optional<NguoiDungHeThong> findByEmail(String email);

	/**
	 * Mã và họ tên của các người dùng trong {@code ma}, một truy vấn (spec 0007, AC-13).
	 */
	@Query("select n.maNguoiDung as maNguoiDung, n.hoTen as hoTen from NguoiDungHeThong n where n.maNguoiDung in :ma")
	List<HoTenNguoiDung> timHoTen(Collection<String> ma);

	interface HoTenNguoiDung {

		String getMaNguoiDung();

		String getHoTen();

	}

	/**
	 * Ghi lúc đăng nhập mà không tăng {@code phien_ban}, để hai lần đăng nhập cùng lúc
	 * không gây lỗi khóa lạc quan.
	 */
	@Modifying
	@Query("update NguoiDungHeThong n set n.dangNhapCuoiLuc = :luc, n.capNhatLuc = :luc where n.maNguoiDung = :ma")
	int ghiDangNhapCuoi(String ma, Instant luc);

	/**
	 * Nâng hash có điều kiện: mật khẩu đã đổi giữa chừng thì không ghi gì, để không ghi
	 * đè mật khẩu mới.
	 */
	@Modifying
	@Query("update NguoiDungHeThong n set n.matKhauHash = :hashMoi where n.maNguoiDung = :ma and n.matKhauHash = :hashCu")
	int nangCapMatKhauHash(String ma, String hashCu, String hashMoi);

	/**
	 * Đổi mật khẩu có điều kiện (API-AUTH-04): hash trong DB phải còn là hash đã dùng để
	 * kiểm mật khẩu cũ. Tăng {@code phien_ban} như mọi lần sửa dữ liệu của người dùng, và
	 * tắt cờ bắt đổi mật khẩu tạm (spec 0008, AC-13).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update NguoiDungHeThong n set n.matKhauHash = :hashMoi, n.capNhatLuc = :luc, n.phienBan = n.phienBan + 1, "
			+ "n.phaiDoiMatKhau = false " + "where n.maNguoiDung = :ma and n.matKhauHash = :hashCu")
	int doiMatKhauHash(String ma, String hashCu, String hashMoi, Instant luc);

}
