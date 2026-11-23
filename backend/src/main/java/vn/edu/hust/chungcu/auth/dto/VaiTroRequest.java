package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Một vai trò trong body tạo tài khoản (spec 0008, AC-1). {@code vaiTro} là mã enum
 * {@link VaiTro}, nhận dạng chuỗi để mã sai thành lỗi theo trường thay vì 400 do Jackson.
 */
public record VaiTroRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "BAN_QUAN_LY", allowableValues = {
				"QUAN_TRI_VIEN", "BAN_QUAN_LY", "BAN_QUAN_TRI", "KE_TOAN", "KY_THUAT", "BAO_VE",
				"CU_DAN" }) String vaiTro,
		@Schema(description = "BAN_QUAN_LY", maxLength = 100) String chucVu,
		@Schema(description = "BAN_QUAN_LY", maxLength = 100) String boPhan,
		@Schema(description = "BAN_QUAN_TRI", maxLength = 50) String nhiemKy,
		@Schema(description = "BAN_QUAN_TRI, lưu vào cột vai_tro", maxLength = 100) String vaiTroBqt,
		@Schema(description = "KE_TOAN", maxLength = 150) String phamViPhuTrach,
		@Schema(description = "KY_THUAT", maxLength = 150) String chuyenMonKyThuat,
		@Schema(description = "BAO_VE", maxLength = 150) String viTriTruc,
		@Schema(description = "CU_DAN, 12 chữ số", pattern = "^[0-9]{12}$") String soCccd) implements TruongVaiTroNhap{

	/** Không in số CCCD ra log. */
	@Override
	public String toString() {
		return "VaiTroRequest[vaiTro=" + this.vaiTro + ", ***]";
	}

}
