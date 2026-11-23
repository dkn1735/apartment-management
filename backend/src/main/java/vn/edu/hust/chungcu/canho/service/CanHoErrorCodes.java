package vn.edu.hust.chungcu.canho.service;

/** Mã lỗi riêng của phân hệ căn hộ (spec 0011). Câu thông báo trùng với frontend. */
public final class CanHoErrorCodes {

	/** Mã hoặc tên tòa nhà đã có (AC-3). */
	public static final String CANHO_TOA_NHA_DA_TON_TAI = "CANHO_TOA_NHA_DA_TON_TAI";

	/** Tầng mới không nối tiếp tầng cao nhất, hoặc tòa đã đủ 99 tầng (AC-5). */
	public static final String CANHO_TANG_SAI_THU_TU = "CANHO_TANG_SAI_THU_TU";

	private CanHoErrorCodes() {
	}

}
