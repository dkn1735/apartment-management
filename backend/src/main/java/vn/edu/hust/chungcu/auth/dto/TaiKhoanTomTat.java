package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Một dòng của danh sách tài khoản (spec 0008, AC-5). Không có số CCCD, hash hay mã bảng
 * con.
 */
public record TaiKhoanTomTat(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
				"Hoạt động", "Tạm khóa", "Ngừng hoạt động" }) String trangThaiTaiKhoan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean phaiDoiMatKhau,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<VaiTro> vaiTro, Instant dangNhapCuoiLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant taoLuc){

	@Override
	public String toString() {
		return "TaiKhoanTomTat[maNguoiDung=" + this.maNguoiDung + ", ***]";
	}

}
