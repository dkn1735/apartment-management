package vn.edu.hust.chungcu.canho.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của thao tác trên căn hộ (spec 0012, AC-14). Mọi diện tích đã qua
 * {@code setScale(2)} để JSON luôn có đúng 2 chữ số thập phân.
 */
final class ChiTietCanHo {

	private ChiTietCanHo() {
	}

	/** {@code CANHO_THEM_CAN_HO}. */
	record Them(String maTang, BigDecimal dienTich, String trangThaiSuDung) implements ChiTietNhatKy {
	}

	/**
	 * {@code CANHO_SUA_CAN_HO}: chỉ trường đổi; trường không đổi là {@code null} và không
	 * ra JSON.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Sua(ThayDoi<BigDecimal> dienTich, ThayDoi<String> trangThaiSuDung) implements ChiTietNhatKy {
	}

	/**
	 * {@code CANHO_TAM_DUNG_TIEN_ICH} và {@code CANHO_MO_KHOA_TIEN_ICH}: lý do đã cắt
	 * (spec 0013, AC-7).
	 */
	record TienIch(String lyDo) implements ChiTietNhatKy {
	}

	/** Giá trị trước và sau một lần sửa. */
	record ThayDoi<T>(T cu, T moi) {
	}

}
