package vn.edu.hust.chungcu.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Nơi duy nhất đổi exception thành response lỗi theo vỏ bọc chung (spec 0001).
 * Không trả stack trace hay câu SQL ra ngoài.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final ErrorResponseFactory errors;

	public GlobalExceptionHandler(ErrorResponseFactory errors) {
		this.errors = errors;
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
		return errors.entity(request, ex.getStatus(), ex.getErrorCode(), ex.getMessage(), ex.getErrors());
	}

	@ExceptionHandler(BindException.class)
	ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest request) {
		// MethodArgumentNotValidException (@Valid trên @RequestBody) là lớp con của BindException
		List<ErrorResponse.FieldError> fields = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
			.toList();
		return validationFailed(request, fields);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
			HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> new ErrorResponse.FieldError(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())))
			.toList();
		return validationFailed(request, fields);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = ex.getConstraintViolations()
			.stream()
			.map(v -> new ErrorResponse.FieldError(lastNode(v.getPropertyPath().toString()), v.getMessage()))
			.toList();
		return validationFailed(request, fields);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		return validationFailed(request,
				List.of(new ErrorResponse.FieldError(ex.getParameterName(), "Thiếu tham số bắt buộc")));
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	ResponseEntity<ErrorResponse> handleMalformed(Exception ex, HttpServletRequest request) {
		return errors.entity(request, HttpStatus.BAD_REQUEST, ErrorCodes.MALFORMED_REQUEST,
				"Yêu cầu không đúng định dạng", List.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		return errors.entity(request, HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.METHOD_NOT_ALLOWED,
				"Phương thức HTTP không được hỗ trợ", List.of());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request) {
		return errors.entity(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.UNSUPPORTED_MEDIA_TYPE,
				"Kiểu nội dung không được hỗ trợ", List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		return errors.entity(request, HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Không tìm thấy tài nguyên",
				List.of());
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return errors.entity(request, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED,
				"Bạn cần đăng nhập để tiếp tục", List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return errors.entity(request, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
				"Bạn không có quyền thực hiện thao tác này", List.of());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex,
			HttpServletRequest request) {
		return errors.entity(request, HttpStatus.CONFLICT, ErrorCodes.CONCURRENT_MODIFICATION,
				"Dữ liệu vừa được người khác thay đổi, vui lòng tải lại", List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("Database từ chối thao tác: {}", ex.getMostSpecificCause().getMessage());
		return errors.entity(request, HttpStatus.CONFLICT, ErrorCodes.CONFLICT,
				"Dữ liệu xung đột với dữ liệu đã có", List.of());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
		return errors.entity(request, HttpStatus.CONTENT_TOO_LARGE, ErrorCodes.PAYLOAD_TOO_LARGE,
				"Tệp vượt quá dung lượng cho phép", List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Lỗi không lường trước", ex);
		return errors.entity(request, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
				"Hệ thống gặp lỗi, vui lòng thử lại sau", List.of());
	}

	private ResponseEntity<ErrorResponse> validationFailed(HttpServletRequest request,
			List<ErrorResponse.FieldError> fields) {
		return errors.entity(request, HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ",
				fields);
	}

	private static String lastNode(String path) {
		int dot = path.lastIndexOf('.');
		return dot < 0 ? path : path.substring(dot + 1);
	}

}
