package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body của {@code PUT /api/v1/accounts/{maNguoiDung}/roles/{vaiTro}} (spec 0008, AC-8).
 * Không có trường {@code vaiTro}: mã vai trò chỉ lấy từ path. Với {@code QUAN_TRI_VIEN}
 * body chỉ có {@code phienBan}.
 */
public record VaiTroCapNhatRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0",
				description = "phienBan nhận ở chi tiết tài khoản") Long phienBan,
		@Schema(description = "BAN_QUAN_LY", maxLength = 100) String chucVu,
		@Schema(description = "BAN_QUAN_LY", maxLength = 100) String boPhan,
		@Schema(description = "BAN_QUAN_TRI", maxLength = 50) String nhiemKy,
		@Schema(description = "BAN_QUAN_TRI, lưu vào cột vai_tro", maxLength = 100) String vaiTroBqt,
		@Schema(description = "KE_TOAN", maxLength = 150) String phamViPhuTrach,
		@Schema(description = "KY_THUAT", maxLength = 150) String chuyenMonKyThuat,
		@Schema(description = "BAO_VE", maxLength = 150) String viTriTruc,
		@Schema(description = "CU_DAN, 12 chữ số", pattern = "^[0-9]{12}$") String soCccd) implements TruongVaiTroNhap {

	@Override
	public String toString() {
		return "VaiTroCapNhatRequest[phienBan=" + this.phienBan + ", ***]";
	}

}
