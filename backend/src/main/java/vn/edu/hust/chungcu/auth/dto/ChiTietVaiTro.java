package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Chi tiết riêng của từng vai trò (spec 0004, mục Mã vai trò). Mỗi trường là {@code null}
 * khi người dùng không có vai trò đó. Không bao giờ chứa {@code soCccd}.
 */
public record ChiTietVaiTro(VaiTroQuanTriVien quanTriVien, VaiTroBanQuanLy banQuanLy, VaiTroBanQuanTri banQuanTri,
		VaiTroKeToan keToan, VaiTroKyThuat kyThuat, VaiTroBaoVe baoVe, VaiTroCuDan cuDan) {

	public record VaiTroQuanTriVien(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maQuanTriVien) {
	}

	public record VaiTroBanQuanLy(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNv,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String chucVu,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String boPhan) {
	}

	public record VaiTroBanQuanTri(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maThanhVien,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nhiemKy,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String vaiTro) {
	}

	public record VaiTroKeToan(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNv,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phamViPhuTrach) {
	}

	public record VaiTroKyThuat(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNv,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String chuyenMonKyThuat) {
	}

	public record VaiTroBaoVe(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNv,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String viTriTruc) {
	}

	public record VaiTroCuDan(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maCuDan) {
	}

}
