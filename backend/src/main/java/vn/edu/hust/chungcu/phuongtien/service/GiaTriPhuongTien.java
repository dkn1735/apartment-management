package vn.edu.hust.chungcu.phuongtien.service;

import java.util.List;

/** Giá trị cố định của phương tiện, trùng với CHECK ở {@code V20} (spec 0016). */
final class GiaTriPhuongTien {

	static final String CHO_DUYET = "Chờ duyệt";

	static final String DANG_HOAT_DONG = "Đang hoạt động";

	static final String TAM_DUNG = "Tạm dừng";

	static final String TU_CHOI = "Từ chối";

	static final String DA_HUY = "Đã hủy";

	static final String XE_DAP = "Xe đạp";

	static final List<String> LOAI = List.of("Ô tô", "Xe máy", "Xe máy điện", XE_DAP);

	private GiaTriPhuongTien() {
	}

}
