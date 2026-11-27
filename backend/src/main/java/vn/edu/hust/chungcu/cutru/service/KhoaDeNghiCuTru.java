package vn.edu.hust.chungcu.cutru.service;

import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;

/**
 * Tìm, kiểm phạm vi và khóa một đề nghị đang chờ xác minh (spec 0015, AC-4 đến AC-6).
 * Luôn khóa dòng {@code can_ho} trước rồi mới khóa dòng {@code ho_so_cu_tru}, cùng thứ tự
 * với spec 0014, nên cư dân và BQL thao tác đồng thời không bao giờ deadlock. Gọi trong
 * transaction của service.
 */
@Component
class KhoaDeNghiCuTru {

	/** Dòng hồ sơ đã khóa và trạng thái sử dụng của căn đọc lúc khóa. */
	record DaKhoa(HoSoCuTruDong dong, String trangThaiCan) {
	}

	private final CanHoCuTruService canHo;

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	KhoaDeNghiCuTru(CanHoCuTruService canHo, HoSoCuTruQuery query, HoSoCuTruGhi ghi) {
		this.canHo = canHo;
		this.query = query;
		this.ghi = ghi;
	}

	/** BQL: 404 khi mã sai, không có hoặc căn ngoài tòa phạm vi; 409 khi hết chờ. */
	DaKhoa choBql(PhamViTruyCap phamVi, String recordId) {
		String maHoSo = NhapLieuCuTru.maHoSo(recordId);
		String maCanHo = (maHoSo != null) ? this.query.cua(maHoSo).map(HoSoCuTruDong::maCanHo).orElse(null) : null;
		if (maCanHo == null || this.canHo.maToaNhaCua(maCanHo).filter(phamVi.maToaNha()::contains).isEmpty()) {
			throw CuTruLoi.khongThay();
		}
		return khoa(maHoSo, maCanHo, null);
	}

	/**
	 * Người gửi: mọi trường hợp không phải đề nghị của chính mình tại căn đang ở đều là
	 * 404 chung, để cư dân cùng căn không dò được đề nghị của nhau; 409 khi hết chờ.
	 */
	DaKhoa choNguoiGui(PhamViTruyCap phamVi, String recordId, String maCuDan) {
		String maHoSo = NhapLieuCuTru.maHoSo(recordId);
		HoSoCuTruDong dong = (maHoSo != null) ? this.query.cua(maHoSo).orElse(null) : null;
		if (dong == null || !phamVi.maCanHo().contains(dong.maCanHo()) || !maCuDan.equals(dong.maCuDanDeNghi())) {
			throw CuTruLoi.khongThay();
		}
		return khoa(maHoSo, dong.maCanHo(), maCuDan);
	}

	private DaKhoa khoa(String maHoSo, String maCanHo, String nguoiGui) {
		String trangThaiCan = this.canHo.khoaVaDocTrangThai(maCanHo).orElseThrow(CuTruLoi::khongThay);
		HoSoCuTruDong dong = this.ghi.khoaDong(maHoSo).orElseThrow(CuTruLoi::khongThay);
		if (nguoiGui != null && !nguoiGui.equals(dong.maCuDanDeNghi())) {
			throw CuTruLoi.khongThay();
		}
		if (!GiaTriCuTru.CHO_XAC_MINH.equals(dong.trangThaiHoSo())) {
			throw CuTruLoi.deNghiKhongConChoXacMinh();
		}
		return new DaKhoa(dong, trangThaiCan);
	}

}
