package vn.edu.hust.chungcu.canho.service;

import java.util.List;

/**
 * Bốn trạng thái sử dụng của căn hộ theo CHECK {@code chk_canho_trangthai} (V2), theo thứ
 * tự dùng khi sắp xếp (spec 0012, Thuật ngữ).
 */
public final class TrangThaiSuDung {

	public static final String TRONG = "Trống";

	public static final List<String> TAT_CA = List.of(TRONG, "Đang sử dụng", "Đang sửa chữa", "Đã bàn giao");

	private TrangThaiSuDung() {
	}

	/** {@code value} đã đổi về NFC. */
	static boolean hopLe(String value) {
		return TAT_CA.contains(value);
	}

}
