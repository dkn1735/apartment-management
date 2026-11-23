package vn.edu.hust.chungcu.auth.repository;

import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.BO_PHAN;
import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.CHUC_VU;
import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.CHUYEN_MON_KY_THUAT;
import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.NHIEM_KY;
import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.PHAM_VI_PHU_TRACH;
import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.SO_CCCD;
import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.VAI_TRO_BQT;
import static vn.edu.hust.chungcu.auth.repository.TruongVaiTro.VI_TRI_TRUC;

import java.util.List;
import vn.edu.hust.chungcu.auth.dto.VaiTro;

/**
 * Bảng con của từng vai trò (spec 0002, mô hình TPT; spec 0008, bảng VaiTroRequest). Tên
 * bảng và cột ở đây là hằng, không bao giờ lấy từ dữ liệu người dùng.
 */
public enum BangVaiTro {

	QUAN_TRI_VIEN(VaiTro.QUAN_TRI_VIEN, "quan_tri_vien", "ma_quan_tri_vien", List.of()),
	BAN_QUAN_LY(VaiTro.BAN_QUAN_LY, "nhan_vien_bql", "ma_nv", List.of(CHUC_VU, BO_PHAN)),
	BAN_QUAN_TRI(VaiTro.BAN_QUAN_TRI, "thanh_vien_ban_quan_tri", "ma_thanh_vien", List.of(NHIEM_KY, VAI_TRO_BQT)),
	KE_TOAN(VaiTro.KE_TOAN, "ke_toan", "ma_nv", List.of(PHAM_VI_PHU_TRACH)),
	KY_THUAT(VaiTro.KY_THUAT, "nhan_vien_ky_thuat", "ma_nv", List.of(CHUYEN_MON_KY_THUAT)),
	BAO_VE(VaiTro.BAO_VE, "nhan_vien_bao_ve", "ma_nv", List.of(VI_TRI_TRUC)),
	CU_DAN(VaiTro.CU_DAN, "cu_dan", "ma_cu_dan", List.of(SO_CCCD));

	private final VaiTro vaiTro;

	private final String bang;

	private final String cotMa;

	private final List<TruongVaiTro> truong;

	BangVaiTro(VaiTro vaiTro, String bang, String cotMa, List<TruongVaiTro> truong) {
		this.vaiTro = vaiTro;
		this.bang = bang;
		this.cotMa = cotMa;
		this.truong = truong;
	}

	public static BangVaiTro cua(VaiTro vaiTro) {
		return valueOf(vaiTro.name());
	}

	public VaiTro vaiTro() {
		return this.vaiTro;
	}

	public String bang() {
		return this.bang;
	}

	/** Cột khóa chính, giá trị là {@code maVaiTro} ở response. */
	public String cotMa() {
		return this.cotMa;
	}

	/** Các trường bắt buộc của vai trò, theo thứ tự hiển thị lỗi. */
	public List<TruongVaiTro> truong() {
		return this.truong;
	}

	/** Tên ràng buộc UNIQUE trên {@code ma_nguoi_dung} của bảng con (mặc định của V1). */
	public String rangBuocMotNguoiDung() {
		return this.bang + "_ma_nguoi_dung_key";
	}

}
