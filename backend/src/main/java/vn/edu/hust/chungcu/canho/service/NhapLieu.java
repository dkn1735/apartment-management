package vn.edu.hust.chungcu.canho.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Chuẩn hóa và đọc dữ liệu nhập của phân hệ căn hộ (spec 0011, Thuật ngữ, AC-2). Kiểm
 * rỗng và độ dài sau chuẩn hóa; độ dài đếm theo code point.
 */
final class NhapLieu {

	static final int SO_TANG_TOI_DA = 99;

	static final String SO_TANG_SAI = "Số tầng phải là số nguyên từ 1 đến 99";

	private static final Pattern KHOANG_TRANG = Pattern.compile("\\s+");

	private NhapLieu() {
	}

	/**
	 * Cắt khoảng trắng hai đầu, gộp khoảng trắng liên tiếp, đổi về NFC; {@code null}
	 * thành chuỗi rỗng.
	 */
	static String chuanHoaChu(String raw) {
		if (raw == null) {
			return "";
		}
		return Normalizer.normalize(KHOANG_TRANG.matcher(raw.strip()).replaceAll(" "), Normalizer.Form.NFC);
	}

	/** Mã tòa: cắt khoảng trắng hai đầu rồi đổi chữ hoa. */
	static String chuanHoaMaToaNha(String raw) {
		return (raw != null) ? raw.strip().toUpperCase(Locale.ROOT) : "";
	}

	static int doDai(String s) {
		return s.codePointCount(0, s.length());
	}

	/**
	 * Số tầng là số nguyên JSON từ 1 đến 99; sai (kể cả {@code 3.0}, {@code "3"},
	 * {@code null}) thì thêm lỗi mục {@code soTang} và trả {@code null}.
	 */
	static Integer soTang(JsonNode raw, List<FieldError> loi) {
		if (raw != null && raw.isIntegralNumber() && raw.canConvertToInt()) {
			int value = raw.intValue();
			if (value >= 1 && value <= SO_TANG_TOI_DA) {
				return value;
			}
		}
		loi.add(new FieldError("soTang", SO_TANG_SAI));
		return null;
	}

}
