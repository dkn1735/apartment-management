package vn.edu.hust.chungcu.cutru.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.common.error.LoiCoSoDuLieu;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;

/**
 * Luật không chồng ngày của hồ sơ hợp lệ (spec 0014, AC-6 bước 4 và 5, AC-8, AC-9). Kiểm
 * trước khi ghi cho trường hợp thường gặp; hai request lọt qua thì ràng buộc loại trừ của
 * {@code V18} chặn và {@link #dich} đổi về cùng mã 409.
 */
@Component
class KiemChongNgay {

	static final String CU_DAN = "ex_hsct_cu_dan_khong_chong_ngay";

	static final String CHU_CAN_HO = "ex_hsct_mot_chu_can_ho";

	private final HoSoCuTruQuery query;

	KiemChongNgay(HoSoCuTruQuery query) {
		this.query = query;
	}

	/**
	 * Chỉ xét khi giá trị mới ở {@code Hợp lệ}.
	 * @param truMaHoSo hồ sơ đang sửa, {@code null} khi thêm mới
	 */
	void kiem(String maCanHo, String maCuDan, HoSoCuTruGhi.GiaTri moi, String truMaHoSo) {
		if (!GiaTriCuTru.HOP_LE.equals(moi.trangThaiHoSo())) {
			return;
		}
		if (this.query.coChongNgayCuDan(maCanHo, maCuDan, moi.ngayBatDau(), moi.ngayKetThuc(), truMaHoSo)) {
			throw CuTruLoi.trungKhoangNgayCuDan();
		}
		if (GiaTriCuTru.CHU_CAN_HO.equals(moi.vaiTroCuTru())
				&& this.query.coChongNgayChu(maCanHo, moi.ngayBatDau(), moi.ngayKetThuc(), truMaHoSo)) {
			throw CuTruLoi.daCoChuCanHo();
		}
	}

	/**
	 * Lỗi của hai ràng buộc loại trừ thành 409; lỗi khác trả lại nguyên. Transaction đã
	 * bị PostgreSQL hủy nên nơi gọi chỉ được ném lỗi, không chạy thêm SQL.
	 */
	static RuntimeException dich(DataIntegrityViolationException ex) {
		if (LoiCoSoDuLieu.sqlState(ex).filter(LoiCoSoDuLieu.LOAI_TRU_GIST::equals).isEmpty()) {
			return ex;
		}
		String rangBuoc = LoiCoSoDuLieu.tenRangBuoc(ex).orElse("");
		return switch (rangBuoc) {
			case CU_DAN -> CuTruLoi.trungKhoangNgayCuDan();
			case CHU_CAN_HO -> CuTruLoi.daCoChuCanHo();
			default -> ex;
		};
	}

}
