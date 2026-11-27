package vn.edu.hust.chungcu.cutru.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/** Lỗi dùng chung của các service hồ sơ cư trú (spec 0014). */
final class CuTruLoi {

	private CuTruLoi() {
	}

	static ApiException duLieuKhongHopLe(List<FieldError> loi) {
		return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ", loi);
	}

	/** Cùng câu với 403 của {@code @ChoPhep}: người gọi đã bị gỡ dòng nhân viên BQL. */
	static ApiException khongCoQuyen() {
		return new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
				"Bạn không có quyền thực hiện thao tác này");
	}

	static ApiException khongThay() {
		return ApiException.notFound(PhamViTruyCap.KHONG_TIM_THAY);
	}

	static ApiException khongThayCuDan() {
		return ApiException.notFound("Không tìm thấy cư dân");
	}

	static ApiException taiKhoanNgungHoatDong() {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, CuTruErrorCodes.CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG,
				"Tài khoản cư dân đã ngừng hoạt động, không thể ghi nhận hoặc duyệt hồ sơ cư trú");
	}

	static ApiException trungKhoangNgayCuDan() {
		return new ApiException(HttpStatus.CONFLICT, CuTruErrorCodes.CUTRU_TRUNG_KHOANG_NGAY_CU_DAN,
				"Cư dân đã có hồ sơ cư trú hợp lệ giao khoảng ngày tại căn hộ này");
	}

	static ApiException daCoChuCanHo() {
		return new ApiException(HttpStatus.CONFLICT, CuTruErrorCodes.CUTRU_DA_CO_CHU_CAN_HO,
				"Căn hộ đã có chủ căn hộ hợp lệ trong khoảng ngày này");
	}

	static ApiException daHetHieuLuc() {
		return new ApiException(HttpStatus.CONFLICT, CuTruErrorCodes.CUTRU_HO_SO_DA_HET_HIEU_LUC,
				"Hồ sơ cư trú đã hết hiệu lực, không thể chỉnh sửa");
	}

	static ApiException deNghiDaKetThuc() {
		return new ApiException(HttpStatus.CONFLICT, CuTruErrorCodes.CUTRU_DE_NGHI_DA_KET_THUC,
				"Đề nghị cư trú đã kết thúc xử lý, không thể chỉnh sửa");
	}

	static ApiException khongKhopCuDan() {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, CuTruErrorCodes.CUTRU_KHONG_KHOP_CU_DAN,
				"Không tìm thấy tài khoản cư dân khớp đồng thời số CCCD và số điện thoại đã nhập");
	}

	static ApiException daCoDeNghiChoXacMinh() {
		return new ApiException(HttpStatus.CONFLICT, CuTruErrorCodes.CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH,
				"Cư dân này đã có đề nghị cư trú đang chờ xác minh tại căn hộ");
	}

	static ApiException vuotSoDeNghiChoXacMinh() {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, CuTruErrorCodes.CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH,
				"Căn hộ đang có tối đa 10 đề nghị chờ xác minh, vui lòng chờ Ban quản lý xử lý hoặc hủy đề nghị cũ");
	}

	static ApiException deNghiKhongConChoXacMinh() {
		return new ApiException(HttpStatus.CONFLICT, CuTruErrorCodes.CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH,
				"Đề nghị cư trú không còn ở trạng thái chờ xác minh");
	}

	static ApiException deNghiDaQuaNgayKetThuc() {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, CuTruErrorCodes.CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC,
				"Đề nghị cư trú đã qua ngày kết thúc, vui lòng từ chối để cư dân gửi lại");
	}

	static ApiException dangChoXacMinh() {
		return new ApiException(HttpStatus.CONFLICT, CuTruErrorCodes.CUTRU_HO_SO_DANG_CHO_XAC_MINH,
				"Hồ sơ đang chờ xác minh, chưa thể chỉnh sửa tại đây");
	}

}
