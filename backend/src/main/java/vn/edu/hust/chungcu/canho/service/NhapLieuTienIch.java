package vn.edu.hust.chungcu.canho.service;

import java.text.Normalizer;
import java.util.List;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Đọc dữ liệu nhập của tạm dừng tiện ích (spec 0013, AC-3, AC-11). Tách khỏi
 * {@link NhapLieuCanHo} để lớp đó không quá dài. Mỗi hàm thêm lỗi vào {@code loi} và trả
 * {@code null} khi giá trị sai.
 */
final class NhapLieuTienIch {

	static final int LY_DO_TOI_DA = 500;

	static final String CO_SAI = "Vui lòng chọn tạm dừng hoặc mở khóa";

	static final String LY_DO_VANG = "Vui lòng nhập lý do";

	static final String LY_DO_DAI = "Lý do tối đa 500 ký tự";

	static final String BO_LOC_SAI = "Giá trị lọc tiện ích không hợp lệ";

	private NhapLieuTienIch() {
	}

	/** Chỉ nhận boolean JSON; chuỗi {@code "true"} hay số {@code 1} là sai. */
	static Boolean coTamDung(JsonNode raw, List<FieldError> loi) {
		if (raw != null && raw.isBoolean()) {
			return raw.booleanValue();
		}
		loi.add(new FieldError("biTamDungTienIch", CO_SAI));
		return null;
	}

	/**
	 * Chuỗi JSON, đổi về NFC rồi cắt khoảng trắng hai đầu; không rỗng và tối đa 500 code
	 * point. Trả chuỗi đã cắt.
	 */
	static String lyDo(JsonNode raw, List<FieldError> loi) {
		if (raw == null || !raw.isString()) {
			loi.add(new FieldError("lyDo", LY_DO_VANG));
			return null;
		}
		String gon = Normalizer.normalize(raw.stringValue(), Normalizer.Form.NFC).strip();
		if (gon.isEmpty()) {
			loi.add(new FieldError("lyDo", LY_DO_VANG));
			return null;
		}
		if (NhapLieu.doDai(gon) > LY_DO_TOI_DA) {
			loi.add(new FieldError("lyDo", LY_DO_DAI));
			return null;
		}
		return gon;
	}

	/**
	 * Tham số lọc trên query: {@code true} hoặc {@code false} sau khi cắt, rỗng là không
	 * lọc; giá trị khác (kể cả {@code TRUE}, {@code 1}) là sai.
	 */
	static Boolean boLoc(String raw, List<FieldError> loi) {
		String gon = NhapLieuCanHo.gon(raw);
		if (gon == null) {
			return null;
		}
		return switch (gon) {
			case "true" -> Boolean.TRUE;
			case "false" -> Boolean.FALSE;
			default -> {
				loi.add(new FieldError("biTamDungTienIch", BO_LOC_SAI));
				yield null;
			}
		};
	}

}
