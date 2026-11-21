package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body của {@code PUT /api/v1/auth/change-password} (API-AUTH-04). Quy tắc độ mạnh: 8 tới
 * 128 ký tự, có ít nhất một chữ cái và một chữ số. Trùng mật khẩu cũ và quá 72 byte UTF-8
 * (giới hạn của bcrypt) do service kiểm.
 */
public record DoiMatKhauRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128) @NotBlank(
				message = "Vui lòng nhập mật khẩu hiện tại") @Size(max = 128,
						message = "Mật khẩu tối đa 128 ký tự") String matKhauCu,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 128,
				description = "Ít nhất 8 ký tự, có cả chữ và số, khác mật khẩu hiện tại") @NotBlank(
						message = "Vui lòng nhập mật khẩu mới") @Size(min = 8, max = 128,
								message = "Mật khẩu mới phải có từ 8 đến 128 ký tự") @Pattern(
										regexp = "^(?=.*\\p{L})(?=.*\\p{Nd}).*$", flags = Pattern.Flag.DOTALL,
										message = "Mật khẩu mới phải có cả chữ và số") String matKhauMoi) {

	/** Không bao giờ in mật khẩu ra log. */
	@Override
	public String toString() {
		return "DoiMatKhauRequest[***]";
	}

}
