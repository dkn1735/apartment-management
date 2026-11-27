package vn.edu.hust.chungcu.cutru.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Đọc dữ liệu nhập của hồ sơ cư trú (spec 0014, AC-5, AC-8). Chuỗi chỉ nhận từ chuỗi
 * JSON, được đổi về NFC và cắt khoảng trắng hai đầu trước khi kiểm. Mỗi hàm thêm lỗi vào
 * {@code loi} và trả {@code null} khi giá trị sai.
 */
final class NhapLieuCuTru {

	static final String NGAY_KET_THUC = "ngayKetThuc";

	private static final Pattern MA_CU_DAN = Pattern.compile("^CD[0-9]{8}$");

	private static final Pattern MA_HO_SO = Pattern.compile("^HS[0-9]{8}$");

	private static final Pattern NGAY = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

	private NhapLieuCuTru() {
	}

	/**
	 * Ngày kết thúc như đã gửi.
	 *
	 * @param coMat có mặt và khác {@code null}
	 * @param giaTri {@code null} khi vắng hoặc sai định dạng
	 */
	record NgayNhap(boolean coMat, LocalDate giaTri) {

		boolean sai() {
			return this.coMat && this.giaTri == null;
		}

	}

	static boolean vang(JsonNode raw) {
		return raw == null || raw.isNull() || raw.isMissingNode();
	}

	/** Mã trên đường dẫn: cắt khoảng trắng hai đầu, viết hoa. */
	static String maTrenDuongDan(String raw) {
		return (raw != null) ? raw.strip().toUpperCase(Locale.ROOT) : "";
	}

	/** Mã hồ sơ trên đường dẫn đã chuẩn hóa; sai mẫu thì {@code null}. */
	static String maHoSo(String raw) {
		String ma = maTrenDuongDan(raw);
		return MA_HO_SO.matcher(ma).matches() ? ma : null;
	}

	/** Từ khóa tìm cư dân: NFC rồi cắt khoảng trắng; {@code null} thành chuỗi rỗng. */
	static String tuKhoa(String raw) {
		return (raw != null) ? nfc(raw) : "";
	}

	static String maCanHo(JsonNode raw, List<FieldError> loi) {
		if (vang(raw)) {
			loi.add(new FieldError("maCanHo", "Vui lòng chọn căn hộ"));
			return null;
		}
		if (!raw.isString()) {
			loi.add(new FieldError("maCanHo", "Căn hộ không hợp lệ"));
			return null;
		}
		String ma = nfc(raw.stringValue()).toUpperCase(Locale.ROOT);
		if (ma.isEmpty()) {
			loi.add(new FieldError("maCanHo", "Vui lòng chọn căn hộ"));
			return null;
		}
		return ma;
	}

	static String maCuDan(JsonNode raw, List<FieldError> loi) {
		String ma = raw != null && raw.isString() ? nfc(raw.stringValue()).toUpperCase(Locale.ROOT) : null;
		if (vang(raw) || "".equals(ma)) {
			loi.add(new FieldError("maCuDan", "Vui lòng chọn cư dân"));
			return null;
		}
		if (ma == null || !MA_CU_DAN.matcher(ma).matches()) {
			loi.add(new FieldError("maCuDan", "Mã cư dân không hợp lệ"));
			return null;
		}
		return ma;
	}

	/** Một trong các giá trị {@code hopLe}; vắng hay rỗng báo {@code cauVang}. */
	static String chon(JsonNode raw, String truong, List<String> hopLe, String cauVang, String cauSai,
			List<FieldError> loi) {
		String value = raw != null && raw.isString() ? nfc(raw.stringValue()) : null;
		if (vang(raw) || "".equals(value)) {
			loi.add(new FieldError(truong, cauVang));
			return null;
		}
		if (value == null || !hopLe.contains(value)) {
			loi.add(new FieldError(truong, cauSai));
			return null;
		}
		return value;
	}

	static LocalDate ngayBatDau(JsonNode raw, List<FieldError> loi) {
		String value = raw != null && raw.isString() ? nfc(raw.stringValue()) : null;
		if (vang(raw) || "".equals(value)) {
			loi.add(new FieldError("ngayBatDau", "Vui lòng chọn ngày bắt đầu"));
			return null;
		}
		LocalDate ngay = ngay(value);
		if (ngay == null) {
			loi.add(new FieldError("ngayBatDau", "Ngày bắt đầu không hợp lệ"));
		}
		return ngay;
	}

	/** Chỉ đọc; lỗi định dạng do {@link #loiKhiHopLe} và {@link #loiKhiKetThuc} báo. */
	static NgayNhap ngayKetThuc(JsonNode raw) {
		if (vang(raw)) {
			return new NgayNhap(false, null);
		}
		return new NgayNhap(true, raw.isString() ? ngay(nfc(raw.stringValue())) : null);
	}

	/**
	 * Lỗi ngày kết thúc khi hồ sơ (sau ghi) ở {@code Hợp lệ} (AC-5); {@code null} là hợp
	 * lệ. {@code batDau} hay {@code tinhTrang} sai thì bỏ qua các luật dựa trên chúng.
	 */
	static String loiKhiHopLe(NgayNhap ketThuc, LocalDate batDau, String tinhTrang, LocalDate homNay) {
		if (ketThuc.sai()) {
			return "Ngày kết thúc không hợp lệ";
		}
		LocalDate kt = ketThuc.giaTri();
		if (kt == null) {
			return GiaTriCuTru.TAM_TRU.equals(tinhTrang) ? "Tạm trú bắt buộc có ngày kết thúc" : null;
		}
		if (batDau == null) {
			return null;
		}
		if (kt.isBefore(batDau)) {
			return "Ngày kết thúc phải từ ngày bắt đầu trở đi";
		}
		return kt.isBefore(homNay) ? "Ngày kết thúc của hồ sơ hợp lệ không được nhỏ hơn hôm nay" : null;
	}

	/** Lỗi ngày kết thúc khi đóng hồ sơ sang {@code Hết hiệu lực} (AC-8). */
	static String loiKhiKetThuc(NgayNhap ketThuc, LocalDate batDau, LocalDate homNay) {
		if (ketThuc.sai()) {
			return "Ngày kết thúc không hợp lệ";
		}
		LocalDate kt = ketThuc.giaTri();
		if (kt == null) {
			return "Kết thúc hồ sơ bắt buộc có ngày kết thúc";
		}
		if (batDau == null) {
			return null;
		}
		if (kt.isBefore(batDau)) {
			return "Ngày kết thúc phải từ ngày bắt đầu trở đi";
		}
		if (batDau.isAfter(homNay)) {
			return kt.equals(batDau) ? null : "Hủy hồ sơ chưa tới ngày bắt đầu cần đặt ngày kết thúc bằng ngày bắt đầu";
		}
		return kt.isAfter(homNay) ? "Ngày kết thúc khi đóng hồ sơ không được lớn hơn hôm nay" : null;
	}

	static String doDaiTuKhoa(String q) {
		return (q.codePointCount(0, q.length()) > 100) ? "Từ khóa tối đa 100 ký tự" : null;
	}

	static String nfc(String raw) {
		return Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
	}

	/** Ngày thực dạng {@code YYYY-MM-DD}; sai thì {@code null}. */
	private static LocalDate ngay(String value) {
		if (!NGAY.matcher(value).matches()) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

}
