package vn.edu.hust.chungcu.cutru.service;

import java.time.LocalDate;
import java.util.Objects;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * So giá trị cũ và mới của một lần {@code PUT} hồ sơ cư trú và dựng chi tiết nhật ký
 * (spec 0014, AC-7, AC-12). Tách khỏi {@link CapNhatHoSoCuTruService} để lớp đó gọn.
 */
final class NhatKyCapNhatCuTru {

	private NhatKyCapNhatCuTru() {
	}

	/** Cả năm trường sửa được giữ nguyên so với DB. */
	static boolean khongDoi(HoSoCuTruDong cu, HoSoCuTruGhi.GiaTri moi) {
		return cu.vaiTroCuTru().equals(moi.vaiTroCuTru()) && cu.tinhTrangCuTru().equals(moi.tinhTrangCuTru())
				&& cu.ngayBatDau().equals(moi.ngayBatDau()) && Objects.equals(cu.ngayKetThuc(), moi.ngayKetThuc())
				&& cu.trangThaiHoSo().equals(moi.trangThaiHoSo());
	}

	static LoaiThaoTac loai(HoSoCuTruGhi.GiaTri moi) {
		return GiaTriCuTru.HET_HIEU_LUC.equals(moi.trangThaiHoSo()) ? LoaiThaoTac.CUTRU_KET_THUC_HO_SO
				: LoaiThaoTac.CUTRU_CAP_NHAT_HO_SO;
	}

	/** Gọi khi hồ sơ cũ ở {@code Hợp lệ} và có ít nhất một trường đổi. */
	static ChiTietNhatKy chiTiet(HoSoCuTruDong cu, HoSoCuTruGhi.GiaTri moi, ChiTietCuTru.ThayDoi<String> doiCan) {
		ChiTietCuTru.ThayDoi<String> vaiTro = doi(cu.vaiTroCuTru(), moi.vaiTroCuTru());
		ChiTietCuTru.ThayDoi<String> tinhTrang = doi(cu.tinhTrangCuTru(), moi.tinhTrangCuTru());
		ChiTietCuTru.ThayDoi<String> batDau = doi(ngay(cu.ngayBatDau()), ngay(moi.ngayBatDau()));
		if (GiaTriCuTru.HET_HIEU_LUC.equals(moi.trangThaiHoSo())) {
			return new ChiTietCuTru.KetThuc(cu.maCanHo(), cu.maCuDan(), ngay(moi.ngayKetThuc()), false, vaiTro,
					tinhTrang, batDau, doiCan);
		}
		return new ChiTietCuTru.CapNhat(cu.maCanHo(), cu.maCuDan(), vaiTro, tinhTrang, batDau,
				doi(ngay(cu.ngayKetThuc()), ngay(moi.ngayKetThuc())), doiCan);
	}

	private static ChiTietCuTru.ThayDoi<String> doi(String cu, String moi) {
		return Objects.equals(cu, moi) ? null : new ChiTietCuTru.ThayDoi<>(cu, moi);
	}

	private static String ngay(LocalDate ngay) {
		return (ngay != null) ? ngay.toString() : null;
	}

}
