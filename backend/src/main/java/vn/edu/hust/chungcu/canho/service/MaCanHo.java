package vn.edu.hust.chungcu.canho.service;

/**
 * Mã căn hộ do server ghép: {@code <maTang><số căn hai chữ số>} (spec 0012, AC-7). CHECK
 * {@code chk_can_ho_ma} của V16 giữ mọi dòng theo đúng mẫu này.
 */
public final class MaCanHo {

	private MaCanHo() {
	}

	public static String cua(String maTang, int soCan) {
		return maTang + String.format("%02d", soCan);
	}

}
