package vn.edu.hust.chungcu.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ có chủ đích. Service ném lỗi này, {@link GlobalExceptionHandler}
 * đổi thành response lỗi theo vỏ bọc chung.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String errorCode;

	private final List<ErrorResponse.FieldError> errors;

	public ApiException(HttpStatus status, String errorCode, String message) {
		this(status, errorCode, message, List.of());
	}

	public ApiException(HttpStatus status, String errorCode, String message, List<ErrorResponse.FieldError> errors) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
		this.errors = List.copyOf(errors);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, message);
	}

	public static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, message);
	}

	public static ApiException businessRule(String message) {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.BUSINESS_RULE_VIOLATION, message);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public List<ErrorResponse.FieldError> getErrors() {
		return errors;
	}

}
