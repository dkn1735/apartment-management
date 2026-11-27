package vn.edu.hust.chungcu.cutru.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Một cư dân để BQL chọn khi tạo hồ sơ cư trú (spec 0014, AC-3). */
public record CuDanLuaChonResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "CD00000001") String maCuDan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "ND00000007") String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Vũ Cư Dân") String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "0900000007") String soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "cudan@chungcu.test") String email,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "001026000007") String soCccd,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
				"Hoạt động", "Tạm khóa" }) String trangThaiTaiKhoan){
}
