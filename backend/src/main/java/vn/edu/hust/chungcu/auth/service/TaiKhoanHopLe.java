package vn.edu.hust.chungcu.auth.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import vn.edu.hust.chungcu.auth.dto.TruongVaiTroNhap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroRequest;
import vn.edu.hust.chungcu.auth.repository.BangVaiTro;
import vn.edu.hust.chungcu.auth.repository.TruongVaiTro;
import vn.edu.hust.chungcu.auth.repository.VaiTroGhi;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Chuẩn hóa rồi kiểm dữ liệu tài khoản (spec 0008, AC-1, AC-4). Không ném lỗi: thêm lỗi
 * vào danh sách theo đúng thứ tự trường để service gộp thành một response 400. Kiểm rỗng
 * và độ dài sau chuẩn hóa.
 */
final class TaiKhoanHopLe {

	static final int HO_TEN_TOI_DA = 100;

	static final int EMAIL_TOI_DA = 150;

	private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");

	private static final Pattern KHOANG_TRANG = Pattern.compile("\\s+");

	private static final Pattern CCCD = Pattern.compile("^[0-9]{12}$");

	private TaiKhoanHopLe() {
	}

	/**
	 * Họ tên, số điện thoại, email đã chuẩn hóa; trường sai thì giá trị là {@code null}.
	 */
	record ThongTinChung(String hoTen, String soDienThoai, String email) {

		@Override
		public String toString() {
			return "ThongTinChung[***]";
		}

	}

	static ThongTinChung thongTinChung(String hoTenRaw, String soDienThoaiRaw, String emailRaw, List<FieldError> loi) {
		String hoTen = chuanHoaHoTen(hoTenRaw);
		if (hoTen.isEmpty()) {
			loi.add(new FieldError("hoTen", "Vui lòng nhập họ tên"));
			hoTen = null;
		}
		else if (hoTen.length() > HO_TEN_TOI_DA) {
			loi.add(new FieldError("hoTen", "Họ tên tối đa 100 ký tự"));
			hoTen = null;
		}
		String soDienThoai = null;
		if (soDienThoaiRaw == null || soDienThoaiRaw.isBlank()) {
			loi.add(new FieldError("soDienThoai", "Vui lòng nhập số điện thoại"));
		}
		else {
			soDienThoai = SoDienThoaiChuanHoa.chuanHoa(soDienThoaiRaw).orElse(null);
			if (soDienThoai == null) {
				loi.add(new FieldError("soDienThoai", "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0"));
			}
		}
		String email = (emailRaw != null) ? emailRaw.strip().toLowerCase(Locale.ROOT) : "";
		if (email.isEmpty()) {
			loi.add(new FieldError("email", "Vui lòng nhập email"));
			email = null;
		}
		else if (email.length() > EMAIL_TOI_DA || !EMAIL.matcher(email).matches()) {
			loi.add(new FieldError("email", "Email không hợp lệ"));
			email = null;
		}
		return new ThongTinChung(hoTen, soDienThoai, email);
	}

	/**
	 * Danh sách vai trò của body tạo; tên trường lỗi có tiền tố {@code vaiTro[i]}. Có lỗi
	 * thì kết quả không dùng được.
	 */
	static List<VaiTroGhi> danhSachVaiTro(List<VaiTroRequest> ds, List<FieldError> loi) {
		List<VaiTroGhi> ketQua = new ArrayList<>();
		if (ds == null || ds.isEmpty()) {
			loi.add(new FieldError("vaiTro", "Vui lòng chọn ít nhất một vai trò"));
			return ketQua;
		}
		Set<VaiTro> daCo = EnumSet.noneOf(VaiTro.class);
		for (int i = 0; i < ds.size(); i++) {
			String tienTo = "vaiTro[" + i + "]";
			VaiTroRequest phanTu = ds.get(i);
			if (phanTu == null) {
				loi.add(new FieldError(tienTo, "Vai trò không tồn tại"));
				continue;
			}
			VaiTro vaiTro = docVaiTro(phanTu.vaiTro());
			if (vaiTro == null) {
				loi.add(new FieldError(tienTo + ".vaiTro", "Vai trò không tồn tại"));
				continue;
			}
			if (!daCo.add(vaiTro)) {
				loi.add(new FieldError(tienTo + ".vaiTro", "Mỗi vai trò chỉ chọn một lần"));
				continue;
			}
			ketQua.add(new VaiTroGhi(vaiTro, truongVaiTro(vaiTro, phanTu, tienTo + ".", loi)));
		}
		return ketQua;
	}

	/**
	 * Trường riêng của một vai trò: bắt buộc đủ trường của vai trò, cấm trường của vai
	 * trò khác. {@code tienTo} là {@code "vaiTro[i]."} ở body tạo, chuỗi rỗng ở body sửa.
	 */
	static Map<TruongVaiTro, String> truongVaiTro(VaiTro vaiTro, TruongVaiTroNhap nhap, String tienTo,
			List<FieldError> loi) {
		List<TruongVaiTro> canCo = BangVaiTro.cua(vaiTro).truong();
		Map<TruongVaiTro, String> giaTri = new EnumMap<>(TruongVaiTro.class);
		for (TruongVaiTro truong : TruongVaiTro.values()) {
			String raw = truong.doc(nhap);
			if (!canCo.contains(truong)) {
				if (raw != null) {
					loi.add(new FieldError(tienTo + truong.ten(), "Trường này không dùng cho vai trò đã chọn"));
				}
				continue;
			}
			String value = chuanHoaChu(raw);
			if (value.isEmpty()) {
				loi.add(new FieldError(tienTo + truong.ten(), "Vui lòng nhập " + chuThuongDau(truong.nhan())));
			}
			else if (truong == TruongVaiTro.SO_CCCD && !CCCD.matcher(value).matches()) {
				loi.add(new FieldError(tienTo + truong.ten(), "Số CCCD phải gồm 12 chữ số"));
			}
			else if (value.length() > truong.doDaiToiDa()) {
				loi.add(new FieldError(tienTo + truong.ten(),
						truong.nhan() + " tối đa " + truong.doDaiToiDa() + " ký tự"));
			}
			else {
				giaTri.put(truong, value);
			}
		}
		return giaTri;
	}

	static final String THIEU_PHIEN_BAN = "Thiếu phiên bản dữ liệu, vui lòng tải lại";

	/** {@code phienBan} bắt buộc, là số nguyên không âm (AC-4). */
	static void kiemPhienBan(Long phienBan, List<FieldError> loi) {
		if (phienBan == null || phienBan < 0) {
			loi.add(new FieldError("phienBan", THIEU_PHIEN_BAN));
		}
	}

	/** {@code phienBan} trên query string; sai thì thêm lỗi và trả {@code null}. */
	static Long docPhienBan(String raw, List<FieldError> loi) {
		if (raw != null && raw.matches("[0-9]{1,18}")) {
			return Long.parseLong(raw);
		}
		loi.add(new FieldError("phienBan", THIEU_PHIEN_BAN));
		return null;
	}

	/** Mã vai trò hợp lệ, hoặc {@code null}. */
	static VaiTro docVaiTro(String ma) {
		if (ma == null) {
			return null;
		}
		for (VaiTro v : VaiTro.values()) {
			if (v.name().equals(ma)) {
				return v;
			}
		}
		return null;
	}

	static String chuanHoaHoTen(String raw) {
		if (raw == null) {
			return "";
		}
		return Normalizer.normalize(KHOANG_TRANG.matcher(raw.strip()).replaceAll(" "), Normalizer.Form.NFC);
	}

	static String chuanHoaChu(String raw) {
		return (raw != null) ? Normalizer.normalize(raw.strip(), Normalizer.Form.NFC) : "";
	}

	private static String chuThuongDau(String nhan) {
		return Character.toLowerCase(nhan.charAt(0)) + nhan.substring(1);
	}

}
