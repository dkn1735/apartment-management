package vn.edu.hust.chungcu.auth.service;

import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của đăng nhập thất bại (spec 0007, AC-2, AC-3): {@code SAI_THONG_TIN}
 * hoặc {@code TAI_KHOAN_KHOA}. Không bao giờ chứa chuỗi tài khoản hay mật khẩu đã gửi.
 */
public record ChiTietDangNhapThatBai(String lyDo) implements ChiTietNhatKy {

	public static final String SAI_THONG_TIN = "SAI_THONG_TIN";

	public static final String TAI_KHOAN_KHOA = "TAI_KHOAN_KHOA";

}
