package vn.edu.hust.chungcu.cutru.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.hust.chungcu.cutru.entity.HoSoCuTru;

public interface HoSoCuTruRepository extends JpaRepository<HoSoCuTru, String> {

	/**
	 * Căn hộ của các hồ sơ ở trạng thái {@code trangThai} còn hiệu lực vào {@code ngay}.
	 */
	@Query("""
			select distinct h.maCanHo from HoSoCuTru h
			where h.maCuDan = :maCuDan and h.trangThaiHoSo = :trangThai and h.ngayBatDau <= :ngay
			and (h.ngayKetThuc is null or h.ngayKetThuc >= :ngay)
			order by h.maCanHo""")
	List<String> findMaCanHoConHieuLuc(String maCuDan, String trangThai, LocalDate ngay);

	/**
	 * Căn có ít nhất một hồ sơ ở trạng thái {@code trangThai} còn hiệu lực vào
	 * {@code ngay}.
	 */
	@Query("""
			select count(h) > 0 from HoSoCuTru h
			where h.maCanHo = :maCanHo and h.trangThaiHoSo = :trangThai and h.ngayBatDau <= :ngay
			and (h.ngayKetThuc is null or h.ngayKetThuc >= :ngay)""")
	boolean coHoSoConHieuLuc(String maCanHo, String trangThai, LocalDate ngay);

	/** Cư dân của các hồ sơ ở trạng thái {@code trangThai} còn hiệu lực tại căn. */
	@Query("""
			select distinct h.maCuDan from HoSoCuTru h
			where h.maCanHo = :maCanHo and h.trangThaiHoSo = :trangThai and h.ngayBatDau <= :ngay
			and (h.ngayKetThuc is null or h.ngayKetThuc >= :ngay)""")
	List<String> findMaCuDanConHieuLuc(String maCanHo, String trangThai, LocalDate ngay);

}
