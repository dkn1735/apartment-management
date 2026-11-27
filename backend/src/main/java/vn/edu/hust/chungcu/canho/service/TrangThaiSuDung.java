package vn.edu.hust.chungcu.canho.service;

import java.util.List;

/**
 * Bốn trạng thái sử dụng của căn hộ theo CHECK {@code chk_canho_trangthai} (V2), theo thứ
 * tự dùng khi sắp xếp (spec 0012, Thuật ngữ).
 */
public final class TrangThaiSuDung {

	public static final String TRONG = "Trống";

	public static final String DANG_SU_DUNG = "Đang sử dụng";

	public static final String DANG_SUA_CHUA = "Đang sửa chữa";

	public static final String DA_BAN_GIAO = "Đã bàn giao";

	public static final List<String> TAT_CA = List.of(TRONG, DANG_SU_DUNG, DANG_SUA_CHUA, DA_BAN_GIAO);

	private TrangThaiSuDung() {
	}

	/** {@code value} đã đổi về NFC. */
	static boolean hopLe(String value) {
		return TAT_CA.contains(value);
	}

}
