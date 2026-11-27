package vn.edu.hust.chungcu.cutru.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;

/**
 * Dựng {@link HoSoCuTruResponse} từ dòng hồ sơ (spec 0014 AC-2, spec 0015 AC-8, Value
 * sourcing). Định danh cư dân (người ở và người đề nghị) và họ tên người xác minh gom một
 * lần cho cả danh sách. Khi ẩn dữ liệu cá nhân, server không đọc người xác minh và đặt
 * {@code null} trước khi trả.
 */
@Component
class DungHoSoCuTru {

	/** Người xem và phần dữ liệu được thấy. */
	enum CheDo {

		/** BQL của tòa: đủ mọi trường, {@code coTheHuy = false}. */
		DAY_DU,

		/** Cư dân xem nhân khẩu: ẩn dữ liệu cá nhân và cả bốn trường người đề nghị. */
		CU_DAN_NHAN_KHAU,

		/** Cư dân xem đề nghị: ẩn dữ liệu cá nhân, giữ thông tin đề nghị. */
		CU_DAN_DE_NGHI

	}

	private final CuTruDinhDanhService dinhDanh;

	DungHoSoCuTru(CuTruDinhDanhService dinhDanh) {
		this.dinhDanh = dinhDanh;
	}

	/**
	 * @param maCuDanNguoiXem mã cư dân của người gọi, chỉ dùng ở
	 * {@link CheDo#CU_DAN_DE_NGHI} để tính {@code coTheHuy}
	 */
	List<HoSoCuTruResponse> dung(List<HoSoCuTruDong> dong, CheDo cheDo, String maCuDanNguoiXem) {
		if (dong.isEmpty()) {
			return List.of();
		}
		Set<String> maCuDan = dong.stream()
			.flatMap(d -> Stream.of(d.maCuDan(), d.maCuDanDeNghi()))
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		Map<String, ThongTinCuDan> cuDan = this.dinhDanh.chiTietCuDanTheoMa(maCuDan);
		Map<String, String> nguoiXacMinh = (cheDo != CheDo.DAY_DU) ? Map.of() : this.dinhDanh.hoTenNvbqlTheoMa(
				dong.stream().map(HoSoCuTruDong::maNvbqlXacMinh).filter(Objects::nonNull).collect(Collectors.toSet()));
		return dong.stream().map(d -> dung(d, cuDan, nguoiXacMinh, cheDo, maCuDanNguoiXem)).toList();
	}

	/** Một hồ sơ, đủ mọi trường (người xem là BQL). */
	HoSoCuTruResponse dung(HoSoCuTruDong dong) {
		return dung(List.of(dong), CheDo.DAY_DU, null).get(0);
	}

	private static HoSoCuTruResponse dung(HoSoCuTruDong d, Map<String, ThongTinCuDan> cuDan,
			Map<String, String> nguoiXacMinh, CheDo cheDo, String maCuDanNguoiXem) {
		// Khóa ngoại bảo đảm luôn có dòng cu_dan; vẫn chịu được khi thiếu
		ThongTinCuDan nguoiO = cuDan.get(d.maCuDan());
		String hoTen = (nguoiO != null) ? nguoiO.hoTen() : null;
		ThongTinCuDan pii = (cheDo == CheDo.DAY_DU) ? nguoiO : null;
		String maNv = (cheDo == CheDo.DAY_DU) ? d.maNvbqlXacMinh() : null;
		boolean anDeNghi = cheDo == CheDo.CU_DAN_NHAN_KHAU;
		String maDeNghi = anDeNghi ? null : d.maCuDanDeNghi();
		ThongTinCuDan nguoiDeNghi = (maDeNghi != null) ? cuDan.get(maDeNghi) : null;
		boolean coTheHuy = cheDo == CheDo.CU_DAN_DE_NGHI && GiaTriCuTru.CHO_XAC_MINH.equals(d.trangThaiHoSo())
				&& maDeNghi != null && maDeNghi.equals(maCuDanNguoiXem);
		return new HoSoCuTruResponse(d.maHoSo(), d.maCanHo(), d.maCuDan(), (pii != null) ? pii.maNguoiDung() : null,
				hoTen, (pii != null) ? pii.soDienThoai() : null, (pii != null) ? pii.email() : null,
				(pii != null) ? pii.soCccd() : null, d.vaiTroCuTru(), d.tinhTrangCuTru(), d.ngayBatDau(),
				d.ngayKetThuc(), d.trangThaiHoSo(), maNv, (maNv != null) ? nguoiXacMinh.get(maNv) : null, d.taoLuc(),
				d.capNhatLuc(), maDeNghi, (nguoiDeNghi != null) ? nguoiDeNghi.hoTen() : null,
				anDeNghi ? null : d.ghiChuDeNghi(), anDeNghi ? null : d.lyDoTuChoi(), coTheHuy);
	}

}
