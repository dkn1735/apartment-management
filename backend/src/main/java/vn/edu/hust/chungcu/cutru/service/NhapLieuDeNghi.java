package vn.edu.hust.chungcu.cutru.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.GuiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.TuChoiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;

/**
 * Đọc dữ liệu nhập của đề nghị cư trú (spec 0015, AC-2, AC-5). Chuỗi chỉ nhận từ chuỗi
 * JSON, được đổi về NFC và cắt khoảng trắng hai đầu trước khi kiểm.
 */
final class NhapLieuDeNghi {

	static final int VAN_BAN_TOI_DA = 500;

	private static final Pattern CCCD = Pattern.compile("^[0-9]{12}$");

	private static final Pattern SDT = Pattern.compile("^0[0-9]{9}$");

	private static final Pattern BO_CCCD = Pattern.compile("[\\s\\-]");

	private static final Pattern BO_SDT = Pattern.compile("[\\s.\\-]");

	private NhapLieuDeNghi() {
	}

	/**
	 * Đề nghị đã chuẩn hóa và hợp lệ.
	 *
	 * @param soCccd chỉ có khi {@code laChinhToi = false}
	 * @param soDienThoai chỉ có khi {@code laChinhToi = false}
	 * @param ghiChu {@code null} khi vắng hoặc chỉ gồm khoảng trắng
	 */
	record DeNghiMoi(String maCanHo, boolean laChinhToi, String soCccd, String soDienThoai, HoSoCuTruGhi.GiaTri giaTri,
			String ghiChu) {
	}

	/**
	 * Thứ tự mục lỗi của AC-2: maCanHo, laChinhToi, soCccd, soDienThoai, vaiTroCuTru,
	 * tinhTrangCuTru, ngayBatDau, ngayKetThuc, ghiChuDeNghi.
	 */
	static DeNghiMoi kiem(GuiDeNghiCuTruRequest request, LocalDate homNay) {
		List<FieldError> loi = new ArrayList<>();
		String maCanHo = NhapLieuCuTru.maCanHo(request.maCanHo(), loi);
		JsonNode chinhToi = request.laChinhToi();
		Boolean laChinhToi = (chinhToi != null && chinhToi.isBoolean()) ? chinhToi.booleanValue() : null;
		if (laChinhToi == null) {
			loi.add(new FieldError("laChinhToi", "Vui lòng chọn đối tượng cư trú"));
		}
		String cccd = null;
		String sdt = null;
		if (Boolean.FALSE.equals(laChinhToi)) {
			cccd = soCccd(request.soCccd(), loi);
			sdt = soDienThoai(request.soDienThoai(), loi);
		}
		String vaiTro = NhapLieuCuTru.chon(request.vaiTroCuTru(), "vaiTroCuTru", GiaTriCuTru.VAI_TRO_DE_NGHI,
				"Vui lòng chọn vai trò cư trú", "Cư dân chỉ được đề nghị vai trò Thành viên gia đình hoặc Khách thuê",
				loi);
		String tinhTrang = NhapLieuCuTru.chon(request.tinhTrangCuTru(), "tinhTrangCuTru", GiaTriCuTru.TINH_TRANG,
				"Vui lòng chọn tình trạng cư trú", "Tình trạng cư trú không hợp lệ", loi);
		LocalDate batDau = NhapLieuCuTru.ngayBatDau(request.ngayBatDau(), loi);
		if (batDau != null && batDau.isBefore(homNay)) {
			loi.add(new FieldError("ngayBatDau", "Ngày bắt đầu của đề nghị phải từ hôm nay trở đi"));
		}
		NhapLieuCuTru.NgayNhap ketThuc = NhapLieuCuTru.ngayKetThuc(request.ngayKetThuc());
		String loiKetThuc = loiNgayKetThuc(ketThuc, batDau, tinhTrang);
		if (loiKetThuc != null) {
			loi.add(new FieldError(NhapLieuCuTru.NGAY_KET_THUC, loiKetThuc));
		}
		String ghiChu = vanBanTuyChon(request.ghiChuDeNghi(), loi);
		if (!loi.isEmpty()) {
			throw CuTruLoi.duLieuKhongHopLe(loi);
		}
		return new DeNghiMoi(maCanHo, laChinhToi, cccd, sdt,
				new HoSoCuTruGhi.GiaTri(vaiTro, tinhTrang, batDau, ketThuc.giaTri(), GiaTriCuTru.CHO_XAC_MINH), ghiChu);
	}

	/** Lý do từ chối bắt buộc (AC-5); sai thì ném 400 một mục {@code lyDoTuChoi}. */
	static String lyDoTuChoi(TuChoiDeNghiCuTruRequest request) {
		JsonNode raw = (request != null) ? request.lyDoTuChoi() : null;
		String loi;
		String value = null;
		if (NhapLieuCuTru.vang(raw)) {
			loi = "Vui lòng nhập lý do từ chối";
		}
		else if (!raw.isString()) {
			loi = "Lý do từ chối không hợp lệ";
		}
		else {
			value = NhapLieuCuTru.nfc(raw.stringValue());
			loi = value.isEmpty() ? "Vui lòng nhập lý do từ chối"
					: (doDai(value) > VAN_BAN_TOI_DA) ? "Lý do từ chối tối đa 500 ký tự" : null;
		}
		if (loi != null) {
			throw CuTruLoi.duLieuKhongHopLe(List.of(new FieldError("lyDoTuChoi", loi)));
		}
		return value;
	}

	private static String soCccd(JsonNode raw, List<FieldError> loi) {
		String value = (raw != null && raw.isString())
				? BO_CCCD.matcher(NhapLieuCuTru.nfc(raw.stringValue())).replaceAll("") : null;
		if (NhapLieuCuTru.vang(raw) || "".equals(value)) {
			loi.add(new FieldError("soCccd", "Vui lòng nhập số CCCD"));
			return null;
		}
		if (value == null || !CCCD.matcher(value).matches()) {
			loi.add(new FieldError("soCccd", "Số CCCD phải gồm đúng 12 chữ số"));
			return null;
		}
		return value;
	}

	private static String soDienThoai(JsonNode raw, List<FieldError> loi) {
		String value = (raw != null && raw.isString()) ? chuanHoaSdt(NhapLieuCuTru.nfc(raw.stringValue())) : null;
		if (NhapLieuCuTru.vang(raw) || "".equals(value)) {
			loi.add(new FieldError("soDienThoai", "Vui lòng nhập số điện thoại"));
			return null;
		}
		if (value == null || !SDT.matcher(value).matches()) {
			loi.add(new FieldError("soDienThoai", "Số điện thoại không hợp lệ"));
			return null;
		}
		return value;
	}

	/**
	 * Bỏ khoảng trắng, dấu chấm, gạch nối; đổi đầu {@code +84} hoặc {@code 84} thành
	 * {@code 0} khi phía sau còn chữ số.
	 */
	static String chuanHoaSdt(String raw) {
		String sdt = BO_SDT.matcher(raw).replaceAll("");
		if (sdt.startsWith("+84") && conChuSo(sdt, 3)) {
			return "0" + sdt.substring(3);
		}
		if (sdt.startsWith("84") && conChuSo(sdt, 2)) {
			return "0" + sdt.substring(2);
		}
		return sdt;
	}

	/**
	 * Lỗi ngày kết thúc của đề nghị; {@code null} là hợp lệ. Không so với hôm nay, vì
	 * ngày bắt đầu đã bị chặn từ hôm nay trở đi.
	 */
	private static String loiNgayKetThuc(NhapLieuCuTru.NgayNhap ketThuc, LocalDate batDau, String tinhTrang) {
		if (ketThuc.sai()) {
			return "Ngày kết thúc không hợp lệ";
		}
		LocalDate kt = ketThuc.giaTri();
		if (kt == null) {
			return GiaTriCuTru.TAM_TRU.equals(tinhTrang) ? "Tạm trú bắt buộc có ngày kết thúc" : null;
		}
		return (batDau != null && kt.isBefore(batDau)) ? "Ngày kết thúc phải từ ngày bắt đầu trở đi" : null;
	}

	private static String vanBanTuyChon(JsonNode raw, List<FieldError> loi) {
		if (NhapLieuCuTru.vang(raw)) {
			return null;
		}
		if (!raw.isString()) {
			loi.add(new FieldError("ghiChuDeNghi", "Ghi chú đề nghị không hợp lệ"));
			return null;
		}
		String value = NhapLieuCuTru.nfc(raw.stringValue());
		if (doDai(value) > VAN_BAN_TOI_DA) {
			loi.add(new FieldError("ghiChuDeNghi", "Ghi chú đề nghị tối đa 500 ký tự"));
			return null;
		}
		return value.isEmpty() ? null : value;
	}

	private static boolean conChuSo(String sdt, int tu) {
		return sdt.length() > tu && Character.isDigit(sdt.charAt(tu));
	}

	private static int doDai(String value) {
		return value.codePointCount(0, value.length());
	}

}
