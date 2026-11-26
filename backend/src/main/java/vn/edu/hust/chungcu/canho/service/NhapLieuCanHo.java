package vn.edu.hust.chungcu.canho.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Đọc dữ liệu nhập của căn hộ (spec 0012, Thuật ngữ, AC-2, AC-8). Tách khỏi
 * {@link NhapLieu} để lớp đó không quá dài. Mỗi hàm thêm lỗi vào {@code loi} và trả
 * {@code null} khi giá trị sai.
 */
final class NhapLieuCanHo {

	static final BigDecimal DIEN_TICH_TOI_DA = new BigDecimal("1000");

	static final String DIEN_TICH_SAI = "Diện tích phải lớn hơn 0, tối đa 1000 m² và tối đa 2 chữ số thập phân";

	static final String DIEN_TICH_LOC_SAI = "Diện tích lọc phải từ 0 đến 1000, tối đa 2 chữ số thập phân";

	static final String SO_CAN_SAI = "Số căn phải là số nguyên từ 1 đến 99";

	static final String TRANG_THAI_SAI = "Trạng thái sử dụng không hợp lệ";

	static final int SO_CAN_TOI_DA = 99;

	/** Dạng chữ nhận trên query: không dấu phẩy, dấu {@code +}, số mũ hay {@code .5}. */
	private static final Pattern DIEN_TICH_LOC = Pattern.compile("^[0-9]{1,4}(\\.[0-9]{1,2})?$");

	private NhapLieuCanHo() {
	}

	/** Trường vắng hoặc {@code null} trong JSON. */
	static boolean vang(JsonNode raw) {
		return raw == null || raw.isNull() || raw.isMissingNode();
	}

	/**
	 * Diện tích là số JSON lớn hơn 0, tối đa 1000, tối đa 2 chữ số thập phân sau khi bỏ
	 * số 0 thừa; trả giá trị đã {@code setScale(2)}.
	 */
	static BigDecimal dienTich(JsonNode raw, List<FieldError> loi) {
		BigDecimal value = soThapPhan(raw);
		if (value != null && value.signum() > 0 && value.compareTo(DIEN_TICH_TOI_DA) <= 0
				&& value.stripTrailingZeros().scale() <= 2) {
			return value.setScale(2);
		}
		loi.add(new FieldError("dienTich", DIEN_TICH_SAI));
		return null;
	}

	/**
	 * Diện tích lọc trên query, từ 0 đến 1000; rỗng sau khi cắt khoảng trắng thì không
	 * lọc và không lỗi.
	 */
	static BigDecimal dienTichLoc(String raw, String truong, List<FieldError> loi) {
		String gon = gon(raw);
		if (gon == null) {
			return null;
		}
		if (DIEN_TICH_LOC.matcher(gon).matches()) {
			BigDecimal value = new BigDecimal(gon);
			if (value.compareTo(DIEN_TICH_TOI_DA) <= 0) {
				return value;
			}
		}
		loi.add(new FieldError(truong, DIEN_TICH_LOC_SAI));
		return null;
	}

	/**
	 * Số căn là số nguyên JSON từ 1 đến 99 (kể cả {@code 1.0} hay {@code "5"} là sai).
	 */
	static Integer soCan(JsonNode raw, List<FieldError> loi) {
		if (raw != null && raw.isIntegralNumber() && raw.canConvertToInt()) {
			int value = raw.intValue();
			if (value >= 1 && value <= SO_CAN_TOI_DA) {
				return value;
			}
		}
		loi.add(new FieldError("soCan", SO_CAN_SAI));
		return null;
	}

	/**
	 * Trạng thái trong body: chỉ nhận chuỗi JSON, đổi về NFC rồi so bốn giá trị. Gọi khi
	 * trường đã có mặt và khác {@code null}.
	 */
	static String trangThai(JsonNode raw, List<FieldError> loi) {
		if (raw.isString()) {
			String value = Normalizer.normalize(raw.stringValue(), Normalizer.Form.NFC);
			if (TrangThaiSuDung.hopLe(value)) {
				return value;
			}
		}
		loi.add(new FieldError("trangThaiSuDung", TRANG_THAI_SAI));
		return null;
	}

	/** Cắt khoảng trắng hai đầu; rỗng hoặc {@code null} thì trả {@code null}. */
	static String gon(String raw) {
		if (raw == null) {
			return null;
		}
		String gon = raw.strip();
		return gon.isEmpty() ? null : gon;
	}

	private static BigDecimal soThapPhan(JsonNode raw) {
		if (raw == null || !raw.isNumber()) {
			return null;
		}
		try {
			return raw.decimalValue();
		}
		catch (RuntimeException ex) {
			// Số quá lớn để đọc thành BigDecimal (vô cực): coi như sai
			return null;
		}
	}

}
