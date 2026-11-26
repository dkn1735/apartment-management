package vn.edu.hust.chungcu.canho.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.service.NguoiDungTraCuuService;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.dto.LanDoiTienIchResponse;

/**
 * Điền hoặc ẩn người đổi tiện ích trong response căn hộ (spec 0013, AC-8, AC-9). Họ tên
 * là họ tên hiện tại, lấy qua service công khai của {@code auth}.
 */
@Component
public class NguoiDoiTienIch {

	private final NguoiDungTraCuuService nguoiDung;

	public NguoiDoiTienIch(NguoiDungTraCuuService nguoiDung) {
		this.nguoiDung = nguoiDung;
	}

	/**
	 * Một truy vấn họ tên cho cả danh sách; không có mã người đổi nào thì không truy vấn.
	 * Mã không có trong map thì họ tên {@code null}.
	 */
	public List<CanHoResponse> ganHoTen(List<CanHoResponse> canHo) {
		Set<String> ma = canHo.stream()
			.map(CanHoResponse::lanDoiTienIch)
			.filter(Objects::nonNull)
			.map(LanDoiTienIchResponse::maNguoiDoi)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		if (ma.isEmpty()) {
			return canHo;
		}
		Map<String, String> hoTen = this.nguoiDung.hoTenTheoMa(ma);
		return canHo.stream().map(c -> ganHoTen(c, hoTen)).toList();
	}

	public CanHoResponse ganHoTen(CanHoResponse canHo) {
		return ganHoTen(List.of(canHo)).get(0);
	}

	/** Bản không có mã và họ tên người đổi, cho cư dân (AC-9). */
	public CanHoResponse anNguoiDoi(CanHoResponse canHo) {
		LanDoiTienIchResponse lanDoi = canHo.lanDoiTienIch();
		return (lanDoi == null) ? canHo : canHo.voiLanDoi(lanDoi.anNguoiDoi());
	}

	private static CanHoResponse ganHoTen(CanHoResponse canHo, Map<String, String> hoTen) {
		LanDoiTienIchResponse lanDoi = canHo.lanDoiTienIch();
		if (lanDoi == null || lanDoi.maNguoiDoi() == null) {
			return canHo;
		}
		return canHo.voiLanDoi(lanDoi.voiHoTen(hoTen.get(lanDoi.maNguoiDoi())));
	}

}
