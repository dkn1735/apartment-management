package vn.edu.hust.chungcu.phuongtien.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.auth.service.TrangThaiTaiKhoan;
import vn.edu.hust.chungcu.cutru.service.CuTruService;

/**
 * Điều kiện để một xe được đăng ký hoặc vào Đang hoạt động (spec 0016, bước 5 và 6 của
 * AC-7): chủ xe đang cư trú hợp lệ tại căn và tài khoản chưa ngừng hoạt động.
 */
@Component
class KiemChuXe {

	private final CuTruService cuTru;

	private final CuTruDinhDanhService dinhDanh;

	KiemChuXe(CuTruService cuTru, CuTruDinhDanhService dinhDanh) {
		this.cuTru = cuTru;
		this.dinhDanh = dinhDanh;
	}

	/**
	 * @param ganVaoTruong lỗi cư trú gắn trên trường chủ xe
	 */
	void kiem(String maCanHo, String maCuDan, LocalDate homNay, boolean ganVaoTruong) {
		if (!this.cuTru.dangCuTruHopLe(maCanHo, maCuDan, homNay)) {
			throw PhuongTienLoi.chuXeKhongCuTru(ganVaoTruong);
		}
		ThongTinCuDan chuXe = this.dinhDanh.chiTietCuDanTheoMa(List.of(maCuDan)).get(maCuDan);
		if (chuXe == null || TrangThaiTaiKhoan.NGUNG_HOAT_DONG.equals(chuXe.trangThaiTaiKhoan())) {
			throw PhuongTienLoi.taiKhoanNgungHoatDong();
		}
	}

}
