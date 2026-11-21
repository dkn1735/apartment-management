package vn.edu.hust.chungcu.auth.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.repository.BangVaiTro;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Lỗi dùng chung của các service quản lý tài khoản (spec 0008). Câu thông báo trùng với
 * frontend.
 */
final class TaiKhoanLoi {

	static final String TRUNG_MESSAGE = "Thông tin đã được dùng cho tài khoản khác";

	static final String LECH_PHIEN_BAN = "Tài khoản vừa được thay đổi ở nơi khác, vui lòng tải lại và thử lại.";

	/** Câu cho từng trường trùng, theo thứ tự của AC-3. */
	private static final Map<String, String> CAU_TRUNG = Map.of("soDienThoai",
			"Số điện thoại đã được dùng cho tài khoản khác", "email", "Email đã được dùng cho tài khoản khác", "soCccd",
			"Số CCCD đã được dùng cho cư dân khác");

	/** Tên ràng buộc UNIQUE của V1 và trường tương ứng. */
	private static final Map<String, String> RANG_BUOC_TRUNG = Map.of("nguoi_dung_he_thong_so_dien_thoai_key",
			"soDienThoai", "nguoi_dung_he_thong_email_key", "email", "cu_dan_so_cccd_key", "soCccd");

	private TaiKhoanLoi() {
	}

	static ApiException duLieuKhongHopLe(List<FieldError> loi) {
		return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ", loi);
	}

	/** 409 {@code AUTH_IDENTITY_TAKEN}; {@code truongTrung} đã theo đúng thứ tự. */
	static ApiException trung(List<String> truongTrung) {
		return new ApiException(HttpStatus.CONFLICT, AuthErrorCodes.AUTH_IDENTITY_TAKEN, TRUNG_MESSAGE,
				truongTrung.stream().map((t) -> new FieldError(t, CAU_TRUNG.get(t))).toList());
	}

	static ApiException lechPhienBan() {
		return new ApiException(HttpStatus.CONFLICT, ErrorCodes.CONCURRENT_MODIFICATION, LECH_PHIEN_BAN);
	}

	/**
	 * Đổi lỗi UNIQUE khi chạy đua thành 409 có nghĩa (AC-3): trường trùng thành
	 * {@code AUTH_IDENTITY_TAKEN}, hai request cùng thêm một vai trò thành
	 * {@code CONCURRENT_MODIFICATION}. Ràng buộc lạ thì ném lại nguyên lỗi.
	 */
	static RuntimeException tuRangBuoc(DataIntegrityViolationException ex) {
		Optional<String> ten = LoiCoSoDuLieu.tenRangBuoc(ex);
		if (ten.isPresent()) {
			String truong = RANG_BUOC_TRUNG.get(ten.get());
			if (truong != null) {
				return trung(List.of(truong));
			}
			for (BangVaiTro bang : BangVaiTro.values()) {
				if (bang.rangBuocMotNguoiDung().equals(ten.get())) {
					return lechPhienBan();
				}
			}
		}
		return ex;
	}

}
