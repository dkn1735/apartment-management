package vn.edu.hust.chungcu.canho.service;

import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/** Chi tiết nhật ký của thao tác trên tòa nhà và tầng (spec 0011, AC-14). */
final class ChiTietToaNha {

	private ChiTietToaNha() {
	}

	/** {@code CANHO_THEM_TOA_NHA}. */
	record Them(int soTang) implements ChiTietNhatKy {
	}

	/** {@code CANHO_THEM_TANG}. */
	record ThemTang(String maToaNha, int soTang) implements ChiTietNhatKy {
	}

}
