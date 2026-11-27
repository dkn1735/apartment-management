package vn.edu.hust.chungcu.cutru.service;

import java.util.List;

/**
 * Giá trị NFC của vai trò, tình trạng và trạng thái hồ sơ cư trú, đúng từng chữ như CHECK
 * ở {@code V2} (spec 0014, Thuật ngữ).
 */
final class GiaTriCuTru {

	static final String CHU_CAN_HO = "Chủ căn hộ";

	static final String THANH_VIEN_GIA_DINH = "Thành viên gia đình";

	static final String KHACH_THUE = "Khách thuê";

	/** Theo thứ tự hiển thị của {@code dangCuTru} (AC-1). */
	static final List<String> VAI_TRO = List.of(CHU_CAN_HO, THANH_VIEN_GIA_DINH, KHACH_THUE);

	static final String THUONG_TRU = "Thường trú";

	static final String TAM_TRU = "Tạm trú";

	static final List<String> TINH_TRANG = List.of(THUONG_TRU, TAM_TRU);

	static final String HOP_LE = "Hợp lệ";

	static final String HET_HIEU_LUC = "Hết hiệu lực";

	static final String CHO_XAC_MINH = "Chờ xác minh";

	static final String TU_CHOI = "Từ chối";

	static final String DA_HUY = "Đã hủy";

	/** Năm trạng thái hợp lệ (spec 0015, Thuật ngữ). */
	static final List<String> TRANG_THAI = List.of(CHO_XAC_MINH, HOP_LE, HET_HIEU_LUC, TU_CHOI, DA_HUY);

	/** Vai trò cư dân được tự đề nghị (spec 0015, Thuật ngữ). */
	static final List<String> VAI_TRO_DE_NGHI = List.of(THANH_VIEN_GIA_DINH, KHACH_THUE);

	/** Trần đề nghị đang chờ xác minh của một căn (spec 0015, AC-3 bước 5). */
	static final int DE_NGHI_CHO_TOI_DA = 10;

	/** Trạng thái ghi được qua {@code PUT} ở lát này. */
	static final List<String> TRANG_THAI_GHI = List.of(HOP_LE, HET_HIEU_LUC);

	private GiaTriCuTru() {
	}

}
