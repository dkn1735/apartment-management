package vn.edu.hust.chungcu.common.api;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Vỏ bọc response thành công dùng chung cho mọi API. Controller tự bọc rõ ràng
 * bằng {@link #ok} hoặc {@link #created}; không bọc ngầm bằng ResponseBodyAdvice.
 * Response 204 không có body, trả {@code ResponseEntity.noContent().build()}.
 */
public record ApiResponse<T>(boolean success, int code, String message, T data, Instant timestamp) {

	public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
		return of(HttpStatus.OK, data, message);
	}

	public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
		return of(HttpStatus.CREATED, data, message);
	}

	private static <T> ResponseEntity<ApiResponse<T>> of(HttpStatus status, T data, String message) {
		// Mốc thời gian của response chỉ là siêu dữ liệu, không phải quy tắc nghiệp vụ
		ApiResponse<T> body = new ApiResponse<>(true, status.value(), message, data, Instant.now());
		return ResponseEntity.status(status).body(body);
	}

}
