package vn.edu.hust.chungcu.nhatky.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Bộ lọc của danh sách nhật ký (spec 0007, AC-12), mọi thành phần có thể {@code null}
 * (không lọc). Khoảng thời gian là nửa mở {@code [tu, den)}: {@code den} là 00:00 của
 * ngày sau {@code denNgay}, theo {@code app.timezone}.
 */
public record BoLocNhatKy(String maNguoiDung, LoaiThaoTac loaiThaoTac, Instant tu, Instant den) {

	private static final Pattern MA_NGUOI_DUNG = Pattern.compile("^ND[0-9]{8}$");

	private static final Pattern NGAY = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

	private static final String SAI_NGAY = "Ngày phải có dạng yyyy-MM-dd";

	/**
	 * Đọc tham số chuỗi của request. Thiếu hoặc rỗng coi như không gửi. Thêm một lỗi cho
	 * mỗi tham số sai vào {@code loi}, theo thứ tự {@code maNguoiDung},
	 * {@code loaiThaoTac}, {@code tuNgay}, {@code denNgay}; có lỗi thì trả {@code null}.
	 */
	public static BoLocNhatKy tu(String maNguoiDung, String loaiThaoTac, String tuNgay, String denNgay, ZoneId zone,
			List<FieldError> loi) {
		int truoc = loi.size();
		String ma = rong(maNguoiDung) ? null : maNguoiDung;
		if (ma != null && !MA_NGUOI_DUNG.matcher(ma).matches()) {
			loi.add(new FieldError("maNguoiDung", "Mã người dùng không đúng định dạng"));
		}
		LoaiThaoTac loai = rong(loaiThaoTac) ? null : loai(loaiThaoTac);
		if (!rong(loaiThaoTac) && loai == null) {
			loi.add(new FieldError("loaiThaoTac", "Loại thao tác không tồn tại"));
		}
		LocalDate tu = rong(tuNgay) ? null : ngay(tuNgay);
		if (!rong(tuNgay) && tu == null) {
			loi.add(new FieldError("tuNgay", SAI_NGAY));
		}
		LocalDate den = rong(denNgay) ? null : ngay(denNgay);
		if (!rong(denNgay) && den == null) {
			loi.add(new FieldError("denNgay", SAI_NGAY));
		}
		else if (tu != null && den != null && tu.isAfter(den)) {
			loi.add(new FieldError("denNgay", "Đến ngày phải bằng hoặc sau từ ngày"));
		}
		if (loi.size() > truoc) {
			return null;
		}
		return new BoLocNhatKy(ma, loai, (tu != null) ? tu.atStartOfDay(zone).toInstant() : null,
				(den != null) ? den.plusDays(1).atStartOfDay(zone).toInstant() : null);
	}

	/** Đúng tên một giá trị, phân biệt hoa thường; không có thì {@code null}. */
	private static LoaiThaoTac loai(String ma) {
		for (LoaiThaoTac loai : LoaiThaoTac.values()) {
			if (loai.name().equals(ma)) {
				return loai;
			}
		}
		return null;
	}

	/** {@code yyyy-MM-dd} và là ngày có thật; sai thì {@code null}. */
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

	private static boolean rong(String value) {
		return value == null || value.isEmpty();
	}

}
