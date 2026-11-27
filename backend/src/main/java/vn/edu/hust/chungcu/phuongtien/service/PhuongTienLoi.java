package vn.edu.hust.chungcu.phuongtien.service;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.common.error.LoiCoSoDuLieu;

/** Lỗi dùng chung của các service phương tiện (spec 0016). */
final class PhuongTienLoi {

	/** Index một phần chặn hai xe cùng giữ một biển (V20). */
	static final String UQ_BIEN_SO = "uq_phuong_tien_bien_so_hieu_luc";

	static final String CAU_TRUNG_BIEN = "Biển số này đang được đăng ký cho một phương tiện khác";

	private static final String CAU_CHU_XE = "Chủ xe không đang cư trú hợp lệ tại căn hộ này";

	private PhuongTienLoi() {
	}

	static ApiException duLieuKhongHopLe(List<FieldError> loi) {
		return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ", loi);
	}

	static ApiException khongCoQuyen() {
		return new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
				"Bạn không có quyền thực hiện thao tác này");
	}

	static ApiException khongThay() {
		return ApiException.notFound(PhamViTruyCap.KHONG_TIM_THAY);
	}

	/**
	 * @param ganVaoTruong lỗi gắn trên trường chủ xe (chỉ khi đăng ký)
	 */
	static ApiException chuXeKhongCuTru(boolean ganVaoTruong) {
		List<FieldError> loi = ganVaoTruong ? List.of(new FieldError("maCuDanSoHuu", CAU_CHU_XE)) : List.of();
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, PhuongTienErrorCodes.PHUONGTIEN_CHU_XE_KHONG_CU_TRU,
				CAU_CHU_XE, loi);
	}

	static ApiException taiKhoanNgungHoatDong() {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT,
				PhuongTienErrorCodes.PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG,
				"Tài khoản của chủ xe đã ngừng hoạt động, không thể đăng ký hoặc kích hoạt phương tiện");
	}

	static ApiException vuotSoDonChoDuyet() {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, PhuongTienErrorCodes.PHUONGTIEN_VUOT_SO_DON_CHO_DUYET,
				"Căn hộ đang có tối đa 10 đơn đăng ký xe chờ duyệt, vui lòng chờ Ban quản lý xử lý hoặc rút đơn cũ");
	}

	/**
	 * @param cauTruongBienSo câu của mục lỗi {@code bienSo}
	 */
	static ApiException trungBienSo(String cauTruongBienSo) {
		return new ApiException(HttpStatus.CONFLICT, PhuongTienErrorCodes.PHUONGTIEN_TRUNG_BIEN_SO, CAU_TRUNG_BIEN,
				List.of(new FieldError("bienSo", cauTruongBienSo)));
	}

	static ApiException chuyenTrangThaiKhongHopLe() {
		return new ApiException(HttpStatus.CONFLICT, PhuongTienErrorCodes.PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE,
				"Không thể chuyển phương tiện từ trạng thái hiện tại sang trạng thái đã chọn");
	}

	static ApiException donKhongConChoDuyet() {
		return new ApiException(HttpStatus.CONFLICT, PhuongTienErrorCodes.PHUONGTIEN_DON_KHONG_CON_CHO_DUYET,
				"Đơn đăng ký phương tiện không còn ở trạng thái chờ duyệt");
	}

	/**
	 * Hai request chạy đua cùng lọt bước kiểm biển thì index chặn lúc ghi (AC-8). Gọi sau
	 * khi transaction đã rollback; câu mục lỗi luôn là câu chung.
	 */
	static RuntimeException dich(DataIntegrityViolationException ex) {
		boolean trungKhoa = LoiCoSoDuLieu.sqlState(ex).filter(LoiCoSoDuLieu.TRUNG_KHOA::equals).isPresent();
		if (trungKhoa && LoiCoSoDuLieu.tenRangBuoc(ex).filter(UQ_BIEN_SO::equals).isPresent()) {
			return trungBienSo(CAU_TRUNG_BIEN);
		}
		return ex;
	}

}
