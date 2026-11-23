package vn.edu.hust.chungcu.auth.service;

/**
 * Kết quả nội bộ của một yêu cầu đặt lại mật khẩu (spec 0010). Chỉ dùng để ghi log; phản
 * hồi cho người dùng luôn giống nhau (AC-2).
 */
enum KetQuaYeuCauDatLai {

	DA_TAO, KHONG_CO_TAI_KHOAN, KHONG_HOAT_DONG, GIOI_HAN, GIOI_HAN_CHUNG

}
