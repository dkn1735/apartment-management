package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kết quả tạo tài khoản (spec 0008, AC-1). {@code matKhauTam} chỉ có ở response này.
 */
public record TaoTaiKhoanResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaiKhoanChiTietResponse taiKhoan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "XXXXXXXXXXXX",
				description = "Mật khẩu tạm, chỉ hiện một lần") String matKhauTam) {

	/** Không bao giờ in mật khẩu tạm. */
	@Override
	public String toString() {
		return "TaoTaiKhoanResponse[taiKhoan=" + this.taiKhoan + ", matKhauTam=***]";
	}

}
