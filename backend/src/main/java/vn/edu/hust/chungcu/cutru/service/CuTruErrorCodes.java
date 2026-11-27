package vn.edu.hust.chungcu.cutru.service;

/** Mã lỗi riêng của phân hệ cư trú (spec 0014). Câu thông báo trùng với frontend. */
public final class CuTruErrorCodes {

	/** Tài khoản của cư dân đã ngừng hoạt động (AC-6). */
	public static final String CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG = "CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG";

	/** Cùng cư dân đã có hồ sơ hợp lệ giao khoảng ngày tại căn (AC-6, AC-8). */
	public static final String CUTRU_TRUNG_KHOANG_NGAY_CU_DAN = "CUTRU_TRUNG_KHOANG_NGAY_CU_DAN";

	/** Căn đã có chủ căn hộ hợp lệ giao khoảng ngày (AC-6, AC-8). */
	public static final String CUTRU_DA_CO_CHU_CAN_HO = "CUTRU_DA_CO_CHU_CAN_HO";

	/** Hồ sơ đã hết hiệu lực, không sửa được (AC-8). */
	public static final String CUTRU_HO_SO_DA_HET_HIEU_LUC = "CUTRU_HO_SO_DA_HET_HIEU_LUC";

	/** Hồ sơ đang chờ xác minh, không sửa ở đây (AC-8). */
	public static final String CUTRU_HO_SO_DANG_CHO_XAC_MINH = "CUTRU_HO_SO_DANG_CHO_XAC_MINH";

	/** Đề nghị đã bị từ chối hoặc đã hủy, không sửa được (spec 0015, AC-7). */
	public static final String CUTRU_DE_NGHI_DA_KET_THUC = "CUTRU_DE_NGHI_DA_KET_THUC";

	/** Không có tài khoản cư dân khớp cả CCCD và số điện thoại (spec 0015, AC-3). */
	public static final String CUTRU_KHONG_KHOP_CU_DAN = "CUTRU_KHONG_KHOP_CU_DAN";

	/** Cặp căn và cư dân đã có đề nghị đang chờ (spec 0015, AC-3, AC-11). */
	public static final String CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH = "CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH";

	/** Căn đã có đủ 10 đề nghị đang chờ (spec 0015, AC-3). */
	public static final String CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH = "CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH";

	/** Đề nghị không còn ở trạng thái chờ xác minh (spec 0015, AC-4 đến AC-6). */
	public static final String CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH = "CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH";

	/** Đề nghị đã qua ngày kết thúc, không duyệt được (spec 0015, AC-4). */
	public static final String CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC = "CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC";

	private CuTruErrorCodes() {
	}

}
