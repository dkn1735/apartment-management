package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body của {@code POST /api/v1/auth/login}. */
public record DangNhapRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150,
		description = "Số điện thoại hoặc email") @NotBlank(message = "Vui lòng nhập số điện thoại hoặc email") @Size(
				max = 150, message = "Số điện thoại hoặc email tối đa 150 ký tự") String taiKhoan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128) @NotBlank(
				message = "Vui lòng nhập mật khẩu") @Size(max = 128,
						message = "Mật khẩu tối đa 128 ký tự") String matKhau) {

	/** Không bao giờ in tài khoản hay mật khẩu ra log (AC-16). */
	@Override
	public String toString() {
		return "DangNhapRequest[***]";
	}

}
