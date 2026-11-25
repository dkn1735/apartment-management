package vn.edu.hust.chungcu.canho.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/** Lỗi dùng chung của các service tòa nhà, tầng và căn hộ (spec 0011, spec 0012). */
final class CanHoLoi {

	static final String TRUNG_MESSAGE = "Tòa nhà đã tồn tại";

	static final String SAI_THU_TU_MESSAGE = "Tầng mới phải nối tiếp tầng cao nhất của tòa";

	static final String TOI_DA_TANG = "Tòa đã đạt tối đa 99 tầng";

	private CanHoLoi() {
	}

	static ApiException duLieuKhongHopLe(List<FieldError> loi) {
		return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ", loi);
	}

	/**
	 * 409 với một mục cho mỗi trường trùng, theo thứ tự {@code maToaNha},
	 * {@code tenToaNha}.
	 */
	static ApiException trung(boolean trungMa, boolean trungTen) {
		List<FieldError> loi = new ArrayList<>();
		if (trungMa) {
			loi.add(new FieldError("maToaNha", "Mã tòa nhà đã được dùng"));
		}
		if (trungTen) {
			loi.add(new FieldError("tenToaNha", "Tên tòa nhà đã được dùng"));
		}
		return new ApiException(HttpStatus.CONFLICT, CanHoErrorCodes.CANHO_TOA_NHA_DA_TON_TAI, TRUNG_MESSAGE, loi);
	}

	/** 409 {@code CANHO_CAN_HO_DA_TON_TAI}, một mục ở ô số căn (spec 0012, AC-9). */
	static ApiException trungCanHo(int soCan, String maCanHo) {
		return new ApiException(HttpStatus.CONFLICT, CanHoErrorCodes.CANHO_CAN_HO_DA_TON_TAI, "Căn hộ đã tồn tại",
				List.of(new FieldError("soCan", "Tầng này đã có căn số " + soCan + " (" + maCanHo + ")")));
	}

	/** 422 {@code CANHO_TANG_SAI_THU_TU} với câu của mục {@code soTang}. */
	static ApiException saiThuTu(String cauSoTang) {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, CanHoErrorCodes.CANHO_TANG_SAI_THU_TU,
				SAI_THU_TU_MESSAGE, List.of(new FieldError("soTang", cauSoTang)));
	}

}
