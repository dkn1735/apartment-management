package vn.edu.hust.chungcu.phuongtien.service;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_TRI;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KE_TOAN;

import java.util.Optional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;

/** Người xem thấy phương tiện theo góc nhìn nào (spec 0016, Thuật ngữ). */
enum GocNhin {

	NHAN_VIEN, CU_DAN_CAN_HO;

	/** Rỗng khi người gọi không được xem căn. */
	static Optional<GocNhin> cua(PhamViTruyCap phamVi, String maToaNha, String maCanHo) {
		boolean nhanVien = (phamVi.coVaiTro(BAN_QUAN_LY) || phamVi.coVaiTro(BAN_QUAN_TRI) || phamVi.coVaiTro(KE_TOAN))
				&& phamVi.maToaNha().contains(maToaNha);
		if (nhanVien) {
			return Optional.of(NHAN_VIEN);
		}
		boolean cuDan = phamVi.coVaiTro(CU_DAN) && phamVi.maCanHo().contains(maCanHo);
		return cuDan ? Optional.of(CU_DAN_CAN_HO) : Optional.empty();
	}

}
