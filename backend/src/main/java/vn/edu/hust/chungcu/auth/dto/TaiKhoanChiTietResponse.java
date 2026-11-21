package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết một tài khoản (spec 0008, AC-6). Không bao giờ có {@code matKhauHash}.
 */
public record TaiKhoanChiTietResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "ND00000001") String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
				"Hoạt động", "Tạm khóa", "Ngừng hoạt động" }) String trangThaiTaiKhoan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean phaiDoiMatKhau, Instant dangNhapCuoiLuc,
		Instant doiMatKhauLuc, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant taoLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant capNhatLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long phienBan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<VaiTroTaiKhoan> vaiTro){

	@Override
	public String toString() {
		return "TaiKhoanChiTietResponse[maNguoiDung=" + this.maNguoiDung + ", ***]";
	}

}
