package vn.edu.hust.chungcu.common.error;

/**
 * Bộ mã lỗi chung (spec 0001). Mỗi phân hệ thêm mã riêng có tiền tố phân hệ,
 * ví dụ {@code AUTH_INVALID_CREDENTIALS}.
 */
public final class ErrorCodes {

	public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
	public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
	public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
	public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
	public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
	public static final String FORBIDDEN = "FORBIDDEN";
	public static final String NOT_FOUND = "NOT_FOUND";
	public static final String CONFLICT = "CONFLICT";
	public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
	public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
	public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
	public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

	private ErrorCodes() {
	}

}
