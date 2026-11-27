package vn.edu.hust.chungcu.phuongtien.service;

import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienDong;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienQuery;

/**
 * Chặn biển số đang được giữ (spec 0016, bước 8 của AC-7). Chỉ BQL có phạm vi tòa của căn
 * đang giữ biển mới được biết mã xe và mã căn.
 */
@Component
class KiemTrungBien {

	private final PhuongTienQuery query;

	private final CanHoCuTruService canHo;

	KiemTrungBien(PhuongTienQuery query, CanHoCuTruService canHo) {
		this.query = query;
		this.canHo = canHo;
	}

	/** {@code bienSo} null (xe đạp không biển) thì không kiểm. */
	void kiem(String bienSo, PhamViTruyCap phamVi, boolean luongBql) {
		if (bienSo == null) {
			return;
		}
		this.query.dangGiuBien(bienSo).ifPresent(dangGiu -> {
			throw PhuongTienLoi.trungBienSo(cau(dangGiu, phamVi, luongBql));
		});
	}

	private String cau(PhuongTienDong dangGiu, PhamViTruyCap phamVi, boolean luongBql) {
		boolean duocBiet = luongBql
				&& this.canHo.maToaNhaCua(dangGiu.maCanHo()).filter(phamVi.maToaNha()::contains).isPresent();
		return duocBiet ? "Biển số đang được giữ bởi phương tiện %s của căn hộ %s".formatted(dangGiu.maPhuongTien(),
				dangGiu.maCanHo()) : PhuongTienLoi.CAU_TRUNG_BIEN;
	}

}
