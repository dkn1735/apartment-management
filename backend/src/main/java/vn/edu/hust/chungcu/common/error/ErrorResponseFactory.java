package vn.edu.hust.chungcu.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.common.web.RequestIdFilter;

/**
 * Dựng response lỗi theo vỏ bọc chung, dùng chung cho exception handler,
 * bộ xử lý lỗi của Spring Security và trang lỗi {@code /error}.
 */
@Component
public class ErrorResponseFactory {

	private final Clock clock;

	private final JsonMapper jsonMapper;

	public ErrorResponseFactory(Clock clock, JsonMapper jsonMapper) {
		this.clock = clock;
		this.jsonMapper = jsonMapper;
	}

	public ErrorResponse build(HttpServletRequest request, HttpStatus status, String errorCode, String message,
			List<ErrorResponse.FieldError> errors) {
		return ErrorResponse.of(status.value(), errorCode, message, errors, RequestIdFilter.currentRequestId(request),
				Instant.now(clock));
	}

	public ResponseEntity<ErrorResponse> entity(HttpServletRequest request, HttpStatus status, String errorCode,
			String message, List<ErrorResponse.FieldError> errors) {
		return ResponseEntity.status(status)
			.contentType(MediaType.APPLICATION_JSON)
			.body(build(request, status, errorCode, message, errors));
	}

	/** Ghi thẳng ra response, cho các chỗ nằm ngoài Spring MVC (filter của Spring Security). */
	public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String errorCode,
			String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), build(request, status, errorCode, message, List.of()));
	}

}
