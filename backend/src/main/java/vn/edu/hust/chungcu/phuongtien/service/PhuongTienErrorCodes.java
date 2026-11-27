package vn.edu.hust.chungcu.phuongtien.service;

/** Mã lỗi riêng của phân hệ phương tiện (spec 0016). */
public final class PhuongTienErrorCodes {

	/** Chủ xe không cư trú hợp lệ hôm nay tại căn (AC-7, AC-10). */
	public static final String PHUONGTIEN_CHU_XE_KHONG_CU_TRU = "PHUONGTIEN_CHU_XE_KHONG_CU_TRU";

	/** Tài khoản chủ xe đã ngừng hoạt động (AC-7, AC-10). */
	public static final String PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG = "PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG";

	/** Căn đã có đủ 10 đơn chờ duyệt (AC-7). */
	public static final String PHUONGTIEN_VUOT_SO_DON_CHO_DUYET = "PHUONGTIEN_VUOT_SO_DON_CHO_DUYET";

	/** Biển số đang được một phương tiện khác giữ (AC-7, AC-8, AC-10). */
	public static final String PHUONGTIEN_TRUNG_BIEN_SO = "PHUONGTIEN_TRUNG_BIEN_SO";

	/** Cặp trạng thái ngoài ma trận (AC-10). */
	public static final String PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE = "PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE";

	/** Đơn không còn chờ duyệt, không rút được (AC-11). */
	public static final String PHUONGTIEN_DON_KHONG_CON_CHO_DUYET = "PHUONGTIEN_DON_KHONG_CON_CHO_DUYET";

	private PhuongTienErrorCodes() {
	}

}
