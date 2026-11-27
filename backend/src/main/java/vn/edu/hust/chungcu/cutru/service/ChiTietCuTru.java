package vn.edu.hust.chungcu.cutru.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/**
 * Chi tiết nhật ký của thao tác trên hồ sơ cư trú (spec 0014, AC-12). Chỉ mã định danh và
 * trường nghiệp vụ cư trú, không bao giờ có họ tên, số điện thoại, email hay số CCCD.
 * Ngày ghi dạng chuỗi {@code YYYY-MM-DD}.
 */
final class ChiTietCuTru {

	private ChiTietCuTru() {
	}

	/**
	 * {@code CUTRU_THEM_HO_SO}; {@code trangThaiCanHo} chỉ có khi căn tự đổi trạng thái.
	 */
	record Them(String maCanHo, String maCuDan, String vaiTroCuTru, String tinhTrangCuTru, String ngayBatDau,
			String ngayKetThuc,
			@JsonInclude(JsonInclude.Include.NON_NULL) ThayDoi<String> trangThaiCanHo) implements ChiTietNhatKy {
	}

	/** {@code CUTRU_CAP_NHAT_HO_SO}: chỉ trường đổi. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record CapNhat(String maCanHo, String maCuDan, ThayDoi<String> vaiTroCuTru, ThayDoi<String> tinhTrangCuTru,
			ThayDoi<String> ngayBatDau, ThayDoi<String> ngayKetThuc,
			ThayDoi<String> trangThaiCanHo) implements ChiTietNhatKy {
	}

	/**
	 * {@code CUTRU_KET_THUC_HO_SO}: {@code ngayKetThuc} là ngày kết thúc cuối cùng; ba
	 * trường sửa kèm chỉ có khi BQL vừa sửa vừa kết thúc trong một lần {@code PUT}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record KetThuc(String maCanHo, String maCuDan, String ngayKetThuc, boolean tuDong, ThayDoi<String> vaiTroCuTru,
			ThayDoi<String> tinhTrangCuTru, ThayDoi<String> ngayBatDau,
			ThayDoi<String> trangThaiCanHo) implements ChiTietNhatKy {
	}

	/**
	 * {@code CUTRU_GUI_DE_NGHI}: chỉ cờ có ghi chú, không chép văn bản tự do (spec 0015).
	 */
	record GuiDeNghi(String maCanHo, String maCuDan, String maCuDanDeNghi, String vaiTroCuTru, String tinhTrangCuTru,
			String ngayBatDau, String ngayKetThuc, boolean coGhiChu) implements ChiTietNhatKy {
	}

	/** {@code CUTRU_DUYET_DE_NGHI}; {@code trangThaiCanHo} chỉ có khi căn tự đổi. */
	record DuyetDeNghi(String maCanHo, String maCuDan, String maCuDanDeNghi, String vaiTroCuTru, String tinhTrangCuTru,
			String ngayBatDau, String ngayKetThuc,
			@JsonInclude(JsonInclude.Include.NON_NULL) ThayDoi<String> trangThaiCanHo) implements ChiTietNhatKy {
	}

	/** {@code CUTRU_TU_CHOI_DE_NGHI}. */
	record TuChoiDeNghi(String maCanHo, String maCuDan, String maCuDanDeNghi,
			String lyDoTuChoi) implements ChiTietNhatKy {
	}

	/** {@code CUTRU_HUY_DE_NGHI}. */
	record HuyDeNghi(String maCanHo, String maCuDan, String maCuDanDeNghi) implements ChiTietNhatKy {
	}

	/** Giá trị trước và sau. */
	record ThayDoi<T>(T cu, T moi) {
	}

}
