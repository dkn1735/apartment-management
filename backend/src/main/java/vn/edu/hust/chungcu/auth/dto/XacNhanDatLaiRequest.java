package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body của {@code POST /api/v1/auth/password-reset/confirm} (spec 0010, AC-7, AC-9). Quy
 * tắc của {@code matKhauMoi} giống {@link DoiMatKhauRequest}; giới hạn 72 byte UTF-8 do
 * service kiểm, cũng trước mọi kiểm tra token. Được trùng mật khẩu hiện tại.
 */
public record XacNhanDatLaiRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				maxLength = 64) @NotBlank(message = "Thiếu token") String token,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 128,
				description = "Ít nhất 8 ký tự, có cả chữ và số") @NotBlank(
						message = "Vui lòng nhập mật khẩu mới") @Size(min = 8, max = 128,
								message = "Mật khẩu mới phải có từ 8 đến 128 ký tự") @Pattern(
										regexp = "^(?=.*\\p{L})(?=.*\\p{Nd}).*$", flags = Pattern.Flag.DOTALL,
										message = "Mật khẩu mới phải có cả chữ và số") String matKhauMoi) {

	/** Không bao giờ in token hay mật khẩu ra log. */
	@Override
	public String toString() {
		return "XacNhanDatLaiRequest[***]";
	}

}
