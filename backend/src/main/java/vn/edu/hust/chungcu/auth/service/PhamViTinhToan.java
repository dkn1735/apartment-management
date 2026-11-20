package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.dto.PhamVi;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.canho.service.ToaNhaService;
import vn.edu.hust.chungcu.cutru.service.CuTruService;

/**
 * Quy tắc duy nhất tính tòa nhà và căn hộ của một người (spec 0006, AC-6), dùng chung cho
 * trang hồ sơ (vai trò từ DB) và {@link PhamViTruyCapService} (vai trò từ token). Khi có
 * bảng gán nhân viên vào tòa, chỉ sửa lớp này.
 */
@Component
public class PhamViTinhToan {

	/** Mọi nhân viên hiện phụ trách mọi tòa; Admin và cư dân không có phạm vi tòa. */
	static final Set<VaiTro> NHAN_VIEN = EnumSet.of(VaiTro.BAN_QUAN_LY, VaiTro.BAN_QUAN_TRI, VaiTro.KE_TOAN,
			VaiTro.KY_THUAT, VaiTro.BAO_VE);

	private final ToaNhaService toaNhaService;

	private final CuTruService cuTruService;

	private final Clock clock;

	public PhamViTinhToan(ToaNhaService toaNhaService, CuTruService cuTruService, Clock clock) {
		this.toaNhaService = toaNhaService;
		this.cuTruService = cuTruService;
		this.clock = clock;
	}

	/**
	 * @param maCuDanHoacNull mã cư dân khi DB còn dòng {@code cu_dan} của người đó
	 */
	public PhamVi tinh(Set<VaiTro> vaiTro, String maCuDanHoacNull) {
		List<String> maToaNha = vaiTro.stream().anyMatch(NHAN_VIEN::contains) ? this.toaNhaService.danhSachMaToaNha()
				: List.of();
		List<String> maCanHo = (vaiTro.contains(VaiTro.CU_DAN) && maCuDanHoacNull != null)
				// Clock mang múi giờ app.timezone, nên đây là ngày theo giờ Việt Nam
				? this.cuTruService.maCanHoDangCuTru(maCuDanHoacNull, LocalDate.now(this.clock)) : List.of();
		return new PhamVi(maToaNha, maCanHo);
	}

}
