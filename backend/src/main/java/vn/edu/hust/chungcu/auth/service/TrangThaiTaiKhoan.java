package vn.edu.hust.chungcu.auth.service;

import java.util.List;

/**
 * Giá trị {@code trang_thai_tai_khoan}, đúng từng chữ như CHECK
 * {@code chk_trangthaitaikhoan} (spec 0002).
 */
public final class TrangThaiTaiKhoan {

	public static final String HOAT_DONG = "Hoạt động";

	public static final String TAM_KHOA = "Tạm khóa";

	public static final String NGUNG_HOAT_DONG = "Ngừng hoạt động";

	public static final List<String> TAT_CA = List.of(HOAT_DONG, TAM_KHOA, NGUNG_HOAT_DONG);

	private TrangThaiTaiKhoan() {
	}

}
