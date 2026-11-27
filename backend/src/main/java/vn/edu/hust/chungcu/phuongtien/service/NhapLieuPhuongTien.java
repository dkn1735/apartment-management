package vn.edu.hust.chungcu.phuongtien.service;

import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.DANG_HOAT_DONG;
import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.DA_HUY;
import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.TAM_DUNG;
import static vn.edu.hust.chungcu.phuongtien.service.GiaTriPhuongTien.TU_CHOI;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.phuongtien.dto.DangKyPhuongTienRequest;
import vn.edu.hust.chungcu.phuongtien.dto.DoiTrangThaiPhuongTienRequest;

/**
 * Đọc dữ liệu nhập của phương tiện (spec 0016, AC-6, AC-9). Chuỗi chỉ nhận từ chuỗi JSON,
 * đổi về NFC và cắt khoảng trắng hai đầu trước khi kiểm.
 */
final class NhapLieuPhuongTien {

	private static final Pattern MA_CU_DAN = Pattern.compile("^CD[0-9]{8}$");

	private static final Pattern MA_PHUONG_TIEN = Pattern.compile("^PT[0-9]{8}$");

	private static final List<String> TRANG_THAI_DICH = List.of(DANG_HOAT_DONG, TAM_DUNG, TU_CHOI, DA_HUY);

	private NhapLieuPhuongTien() {
	}

	/** Đăng ký đã chuẩn hóa; {@code bienSo} và {@code nhanHieuMauSac} có thể null. */
	record DangKy(String maCanHo, String maCuDanSoHuu, String loaiPhuongTien, String bienSo, String nhanHieuMauSac) {
	}

	/** Yêu cầu đổi trạng thái đã chuẩn hóa; {@code lyDo} có thể null. */
	record DoiTrangThai(String trangThai, String lyDo) {
	}

	/** Mã trên đường dẫn: NFC, cắt khoảng trắng, viết hoa. */
	static String maTrenDuongDan(String raw) {
		return (raw != null) ? nfc(raw).toUpperCase(Locale.ROOT) : "";
	}

	/** Mã phương tiện đã chuẩn hóa; sai mẫu thì {@code null}. */
	static String maPhuongTien(String raw) {
		String ma = maTrenDuongDan(raw);
		return MA_PHUONG_TIEN.matcher(ma).matches() ? ma : null;
	}

	/** Thứ tự mục lỗi của AC-6; sai thì ném 400. */
	static DangKy dangKy(DangKyPhuongTienRequest request) {
		List<FieldError> loi = new ArrayList<>();
		String maCanHo = maCanHo(request.maCanHo(), loi);
		String chuXe = maChuXe(request.maCuDanSoHuu(), loi);
		String loai = chon(request.loaiPhuongTien(), "loaiPhuongTien", GiaTriPhuongTien.LOAI,
				"Vui lòng chọn loại phương tiện", "Loại phương tiện không hợp lệ", loi);
		String bienSo = bienSo(request.bienSo(), loai, loi);
		String nhanHieu = vanBan(request.nhanHieuMauSac(), "nhanHieuMauSac", 100, "Nhãn hiệu, màu sắc không hợp lệ",
				"Nhãn hiệu, màu sắc tối đa 100 ký tự", loi);
		if (!loi.isEmpty()) {
			throw PhuongTienLoi.duLieuKhongHopLe(loi);
		}
		return new DangKy(maCanHo, chuXe, loai, bienSo, nhanHieu);
	}

	/** Thứ tự mục lỗi của AC-9: trangThaiDangKy, lyDo; sai thì ném 400. */
	static DoiTrangThai doiTrangThai(DoiTrangThaiPhuongTienRequest request) {
		List<FieldError> loi = new ArrayList<>();
		String trangThai = chon(request.trangThaiDangKy(), "trangThaiDangKy", TRANG_THAI_DICH,
				"Vui lòng chọn trạng thái", "Trạng thái không hợp lệ", loi);
		int truoc = loi.size();
		String lyDo = vanBan(request.lyDo(), "lyDo", 500, "Lý do không hợp lệ", "Lý do tối đa 500 ký tự", loi);
		boolean batBuoc = TAM_DUNG.equals(trangThai) || TU_CHOI.equals(trangThai) || DA_HUY.equals(trangThai);
		if (batBuoc && lyDo == null && loi.size() == truoc) {
			loi.add(new FieldError("lyDo", "Vui lòng nhập lý do"));
		}
		if (!loi.isEmpty()) {
			throw PhuongTienLoi.duLieuKhongHopLe(loi);
		}
		return new DoiTrangThai(trangThai, lyDo);
	}

	private static String maCanHo(JsonNode raw, List<FieldError> loi) {
		String ma = chuoi(raw);
		if (vang(raw) || "".equals(ma)) {
			loi.add(new FieldError("maCanHo", "Vui lòng chọn căn hộ"));
			return null;
		}
		if (ma == null) {
			loi.add(new FieldError("maCanHo", "Căn hộ không hợp lệ"));
			return null;
		}
		return ma.toUpperCase(Locale.ROOT);
	}

	private static String maChuXe(JsonNode raw, List<FieldError> loi) {
		String value = chuoi(raw);
		if (vang(raw) || "".equals(value)) {
			loi.add(new FieldError("maCuDanSoHuu", "Vui lòng chọn chủ xe"));
			return null;
		}
		String ma = (value != null) ? value.toUpperCase(Locale.ROOT) : null;
		if (ma == null || !MA_CU_DAN.matcher(ma).matches()) {
			loi.add(new FieldError("maCuDanSoHuu", "Chủ xe không hợp lệ"));
			return null;
		}
		return ma;
	}

	private static String chon(JsonNode raw, String truong, List<String> hopLe, String cauVang, String cauSai,
			List<FieldError> loi) {
		String value = chuoi(raw);
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

	/** Chỉ xe đạp được để trống; loại sai thì biển số vẫn bắt buộc. */
	private static String bienSo(JsonNode raw, String loai, List<FieldError> loi) {
		if (!vang(raw) && !raw.isString()) {
			loi.add(new FieldError("bienSo", "Biển số không hợp lệ"));
			return null;
		}
		String bienSo = vang(raw) ? "" : BienSo.chuanHoa(raw.stringValue());
		if (bienSo.isEmpty()) {
			if (!GiaTriPhuongTien.XE_DAP.equals(loai)) {
				loi.add(new FieldError("bienSo", "Vui lòng nhập biển số"));
			}
			return null;
		}
		if (!BienSo.hopLe(bienSo)) {
			loi.add(new FieldError("bienSo", "Biển số chỉ gồm chữ và số, dài từ 5 đến 12 ký tự"));
			return null;
		}
		return bienSo;
	}

	/** Văn bản tùy chọn; chỉ gồm khoảng trắng thì {@code null}. */
	private static String vanBan(JsonNode raw, String truong, int toiDa, String cauSai, String cauDai,
			List<FieldError> loi) {
		if (vang(raw)) {
			return null;
		}
		if (!raw.isString()) {
			loi.add(new FieldError(truong, cauSai));
			return null;
		}
		String value = nfc(raw.stringValue());
		if (value.codePointCount(0, value.length()) > toiDa) {
			loi.add(new FieldError(truong, cauDai));
			return null;
		}
		return value.isEmpty() ? null : value;
	}

	private static boolean vang(JsonNode raw) {
		return raw == null || raw.isNull() || raw.isMissingNode();
	}

	private static String chuoi(JsonNode raw) {
		return (raw != null && raw.isString()) ? nfc(raw.stringValue()) : null;
	}

	private static String nfc(String raw) {
		return Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
	}

}
