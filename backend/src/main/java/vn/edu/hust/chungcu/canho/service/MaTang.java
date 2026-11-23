package vn.edu.hust.chungcu.canho.service;

/**
 * Mã tầng do server ghép: {@code <maToaNha>-<số tầng hai chữ số>} (spec 0011, AC-6). SQL
 * {@code generate_series} của {@code ToaNhaGhi} dùng cùng mẫu; CHECK {@code chk_tang_ma}
 * giữ hai nơi không lệch nhau.
 */
public final class MaTang {

	private MaTang() {
	}

	public static String cua(String maToaNha, int soTang) {
		return maToaNha + "-" + String.format("%02d", soTang);
	}

}
