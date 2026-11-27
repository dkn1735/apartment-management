package vn.edu.hust.chungcu.phuongtien.service;

import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.CHO_DUYET;
import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.DANG_HOAT_DONG;
import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.DA_HUY;
import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.TAM_DUNG;
import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.TU_CHOI;

import java.util.Map;
import java.util.Set;

/**
 * Chuyển trạng thái mà BQL được làm qua {@code PUT .../status} (spec 0016, AC-10).
 * {@code Từ chối} và {@code Đã hủy} là trạng thái cuối.
 */
final class MaTranTrangThai {

	private static final Map<String, Set<String>> DUOC_SANG = Map.of(CHO_DUYET, Set.of(DANG_HOAT_DONG, TU_CHOI),
			DANG_HOAT_DONG, Set.of(TAM_DUNG, DA_HUY), TAM_DUNG, Set.of(DANG_HOAT_DONG, DA_HUY));

	private MaTranTrangThai() {
	}

	static boolean chuyenDuoc(String tu, String sang) {
		return DUOC_SANG.getOrDefault(tu, Set.of()).contains(sang);
	}

}
