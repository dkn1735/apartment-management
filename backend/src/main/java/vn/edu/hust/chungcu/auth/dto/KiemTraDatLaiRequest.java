package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Body của {@code POST /api/v1/auth/password-reset/check} (spec 0010, AC-6). Token đi
 * trong body, không bao giờ trong URL (AC-15). Token dài hay sai dạng không bị bean
 * validation chặn mà trả {@code AUTH_RESET_TOKEN_INVALID} như mọi token hỏng khác.
 */
public record KiemTraDatLaiRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		maxLength = 64) @NotBlank(message = "Thiếu token") String token) {

	/** Không in token ra log. */
	@Override
	public String toString() {
		return "KiemTraDatLaiRequest[***]";
	}

}
