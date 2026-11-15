package vn.edu.hust.chungcu.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Vỏ bọc response lỗi dùng chung. Luôn có {@code errors} (rỗng khi lỗi không
 * theo từng trường) và {@code requestId}; không có {@code data}.
 */
public record ErrorResponse(
		boolean success,
		int code,
		String message,
		String errorCode,
		List<FieldError> errors,
		String requestId,
		Instant timestamp) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(int status, String errorCode, String message, List<FieldError> errors,
			String requestId, Instant timestamp) {
		return new ErrorResponse(false, status, message, errorCode, errors == null ? List.of() : errors,
				requestId, timestamp);
	}

}
