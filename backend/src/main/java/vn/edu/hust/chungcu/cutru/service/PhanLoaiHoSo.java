package vn.edu.hust.chungcu.cutru.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;

/**
 * Chia hồ sơ của một căn thành bốn nhóm và sắp xếp từng nhóm (spec 0014 AC-1, spec 0015
 * AC-8). Ba nhóm nhân khẩu chỉ gồm dòng {@code Hợp lệ} và {@code Hết hiệu lực}; nhóm đề
 * nghị gồm {@code Chờ xác minh}, {@code Từ chối} và {@code Đã hủy}.
 */
final class PhanLoaiHoSo {

	private PhanLoaiHoSo() {
	}

	/** Bốn nhóm đã sắp xếp. */
	record Nhom(List<HoSoCuTruDong> dangCuTru, List<HoSoCuTruDong> sapBatDau, List<HoSoCuTruDong> lichSuCuTru,
			List<HoSoCuTruDong> deNghiCuTru) {
	}

	static Nhom chia(List<HoSoCuTruDong> dong, LocalDate homNay) {
		List<HoSoCuTruDong> dang = new ArrayList<>();
		List<HoSoCuTruDong> sap = new ArrayList<>();
		List<HoSoCuTruDong> lichSu = new ArrayList<>();
		List<HoSoCuTruDong> deNghi = new ArrayList<>();
		for (HoSoCuTruDong d : dong) {
			if (GiaTriCuTru.HET_HIEU_LUC.equals(d.trangThaiHoSo())) {
				lichSu.add(d);
			}
			else if (!GiaTriCuTru.HOP_LE.equals(d.trangThaiHoSo())) {
				deNghi.add(d);
			}
			else if (d.ngayKetThuc() != null && d.ngayKetThuc().isBefore(homNay)) {
				lichSu.add(d);
			}
			else if (d.ngayBatDau().isAfter(homNay)) {
				sap.add(d);
			}
			else {
				dang.add(d);
			}
		}
		dang.sort(Comparator.comparingInt((HoSoCuTruDong d) -> thuTuVaiTro(d.vaiTroCuTru()))
			.thenComparing(HoSoCuTruDong::ngayBatDau)
			.thenComparing(HoSoCuTruDong::maHoSo));
		sap.sort(Comparator.comparing(HoSoCuTruDong::ngayBatDau).thenComparing(HoSoCuTruDong::maHoSo));
		lichSu.sort(Comparator.comparing((HoSoCuTruDong d) -> d.ngayBatDau().isAfter(homNay))
			.thenComparing(PhanLoaiHoSo::ngayMoc, Comparator.reverseOrder())
			.thenComparing(HoSoCuTruDong::maHoSo, Comparator.reverseOrder()));
		deNghi.sort(PhanLoaiHoSo::soDeNghi);
		return new Nhom(dang, sap, lichSu, deNghi);
	}

	/**
	 * Đề nghị đang chờ lên trước theo {@code tao_luc, ma_ho_so} tăng dần; đề nghị đã kết
	 * thúc sau theo {@code cap_nhat_luc, ma_ho_so} giảm dần.
	 */
	private static int soDeNghi(HoSoCuTruDong a, HoSoCuTruDong b) {
		boolean choA = GiaTriCuTru.CHO_XAC_MINH.equals(a.trangThaiHoSo());
		boolean choB = GiaTriCuTru.CHO_XAC_MINH.equals(b.trangThaiHoSo());
		if (choA != choB) {
			return choA ? -1 : 1;
		}
		if (choA) {
			return Comparator.comparing(HoSoCuTruDong::taoLuc).thenComparing(HoSoCuTruDong::maHoSo).compare(a, b);
		}
		return Comparator.comparing(HoSoCuTruDong::capNhatLuc)
			.thenComparing(HoSoCuTruDong::maHoSo)
			.reversed()
			.compare(a, b);
	}

	private static int thuTuVaiTro(String vaiTro) {
		int i = GiaTriCuTru.VAI_TRO.indexOf(vaiTro);
		return (i >= 0) ? i : GiaTriCuTru.VAI_TRO.size();
	}

	/** {@code COALESCE(ngay_ket_thuc, ngay_bat_dau)}. */
	private static LocalDate ngayMoc(HoSoCuTruDong d) {
		return (d.ngayKetThuc() != null) ? d.ngayKetThuc() : d.ngayBatDau();
	}

}
