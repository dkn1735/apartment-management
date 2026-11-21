package vn.edu.hust.chungcu.auth.repository;

import vn.edu.hust.chungcu.auth.dto.TruongVaiTroNhap;

/**
 * Một trường riêng của vai trò (spec 0008, bảng VaiTroRequest): tên JSON, nhãn tiếng
 * Việt, cột trong bảng con và độ dài tối đa.
 */
public enum TruongVaiTro {

	CHUC_VU("chucVu", "Chức vụ", "chuc_vu", 100), BO_PHAN("boPhan", "Bộ phận", "bo_phan", 100),
	NHIEM_KY("nhiemKy", "Nhiệm kỳ", "nhiem_ky", 50),
	VAI_TRO_BQT("vaiTroBqt", "Vai trò trong ban quản trị", "vai_tro", 100),
	PHAM_VI_PHU_TRACH("phamViPhuTrach", "Phạm vi phụ trách", "pham_vi_phu_trach", 150),
	CHUYEN_MON_KY_THUAT("chuyenMonKyThuat", "Chuyên môn kỹ thuật", "chuyen_mon_ky_thuat", 150),
	VI_TRI_TRUC("viTriTruc", "Vị trí trực", "vi_tri_truc", 150), SO_CCCD("soCccd", "Số CCCD", "so_cccd", 12);

	private final String ten;

	private final String nhan;

	private final String cot;

	private final int doDaiToiDa;

	TruongVaiTro(String ten, String nhan, String cot, int doDaiToiDa) {
		this.ten = ten;
		this.nhan = nhan;
		this.cot = cot;
		this.doDaiToiDa = doDaiToiDa;
	}

	/** Tên trường trong JSON và trong {@code errors}. */
	public String ten() {
		return this.ten;
	}

	public String nhan() {
		return this.nhan;
	}

	public String cot() {
		return this.cot;
	}

	public int doDaiToiDa() {
		return this.doDaiToiDa;
	}

	/** Giá trị người dùng gửi cho trường này. */
	public String doc(TruongVaiTroNhap nhap) {
		return switch (this) {
			case CHUC_VU -> nhap.chucVu();
			case BO_PHAN -> nhap.boPhan();
			case NHIEM_KY -> nhap.nhiemKy();
			case VAI_TRO_BQT -> nhap.vaiTroBqt();
			case PHAM_VI_PHU_TRACH -> nhap.phamViPhuTrach();
			case CHUYEN_MON_KY_THUAT -> nhap.chuyenMonKyThuat();
			case VI_TRI_TRUC -> nhap.viTriTruc();
			case SO_CCCD -> nhap.soCccd();
		};
	}

}
