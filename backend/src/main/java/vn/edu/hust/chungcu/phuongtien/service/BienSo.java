package vn.edu.hust.chungcu.phuongtien.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Biển số dạng chuẩn (spec 0016, Thuật ngữ): NFC, viết hoa, bỏ khoảng trắng Unicode, dấu
 * chấm và gạch nối.
 */
final class BienSo {

	private static final Pattern BO = Pattern.compile("[\\s\\p{Z}.\\-]");

	private static final Pattern HOP_LE = Pattern.compile("^[A-Z0-9]{5,12}$");

	private BienSo() {
	}

	static String chuanHoa(String raw) {
		String nfc = Normalizer.normalize(raw, Normalizer.Form.NFC).toUpperCase(Locale.ROOT);
		return BO.matcher(nfc).replaceAll("");
	}

	/**
	 * @param daChuanHoa kết quả của {@link #chuanHoa}
	 */
	static boolean hopLe(String daChuanHoa) {
		return HOP_LE.matcher(daChuanHoa).matches();
	}

}
