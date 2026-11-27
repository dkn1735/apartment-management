package vn.edu.hust.chungcu.cutru.service;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.canho.service.TrangThaiSuDung;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;

/**
 * Đồng bộ trạng thái sử dụng của căn theo hồ sơ cư trú (spec 0014, AC-10). Gọi sau khi đã
 * ghi {@code ho_so_cu_tru}, dưới khóa dòng căn của cùng transaction.
 */
@Component
class DongBoTrangThaiCanHo {

	private final HoSoCuTruQuery query;

	private final CanHoCuTruService canHo;

	DongBoTrangThaiCanHo(HoSoCuTruQuery query, CanHoCuTruService canHo) {
		this.query = query;
		this.canHo = canHo;
	}

	/**
	 * @param trangThaiDangKhoa trạng thái đọc lúc khóa dòng căn
	 * @return trạng thái cũ và mới khi căn đổi, {@code null} khi giữ nguyên
	 */
	ChiTietCuTru.ThayDoi<String> dongBo(String maCanHo, String trangThaiDangKhoa, LocalDate homNay) {
		String moi = trangThaiMoi(trangThaiDangKhoa, this.query.demDangCuTru(maCanHo, homNay));
		if (moi.equals(trangThaiDangKhoa)) {
			return null;
		}
		this.canHo.capNhatTrangThaiSuDung(maCanHo, moi);
		return new ChiTietCuTru.ThayDoi<>(trangThaiDangKhoa, moi);
	}

	/** Có người thì Trống hoặc Đã bàn giao thành Đang sử dụng; không ai thì ngược lại. */
	static String trangThaiMoi(String hienTai, long soNguoiDangCuTru) {
		if (soNguoiDangCuTru > 0) {
			return (TrangThaiSuDung.TRONG.equals(hienTai) || TrangThaiSuDung.DA_BAN_GIAO.equals(hienTai))
					? TrangThaiSuDung.DANG_SU_DUNG : hienTai;
		}
		return TrangThaiSuDung.DANG_SU_DUNG.equals(hienTai) ? TrangThaiSuDung.TRONG : hienTai;
	}

}
