package vn.edu.hust.chungcu.auth.service;

/**
 * Giá trị {@code phien_dang_nhap.ly_do_thu_hoi}, đúng từng chữ như CHECK
 * {@code chk_phien_dang_nhap_ly_do_thu_hoi} (spec 0002). Lát này dùng bốn giá trị đầu
 * (spec 0005).
 */
public final class LyDoThuHoi {

	public static final String DA_XOAY_VONG = "Đã xoay vòng";

	public static final String DUNG_LAI_TOKEN_CU = "Dùng lại token cũ";

	public static final String DANG_XUAT = "Đăng xuất";

	public static final String TAI_KHOAN_BI_KHOA = "Tài khoản bị khóa";

	private LyDoThuHoi() {
	}

}
