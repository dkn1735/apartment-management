package vn.edu.hust.chungcu.common.error;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thay trang lỗi mặc định của Spring Boot ({@code /error}) để cả những lỗi xảy ra
 * ngoài controller (trong filter, trong servlet container) vẫn theo vỏ bọc chung.
 */
@Hidden
@RestController
public class JsonErrorController implements ErrorController {

	private final ErrorResponseFactory errors;

	public JsonErrorController(ErrorResponseFactory errors) {
		this.errors = errors;
	}

	@RequestMapping("${server.error.path:/error}")
	ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
		HttpStatus status = resolveStatus(request);
		return switch (status) {
			case NOT_FOUND -> errors.entity(request, status, ErrorCodes.NOT_FOUND, "Không tìm thấy tài nguyên",
					List.of());
			case UNAUTHORIZED -> errors.entity(request, status, ErrorCodes.UNAUTHENTICATED,
					"Bạn cần đăng nhập để tiếp tục", List.of());
			case FORBIDDEN -> errors.entity(request, status, ErrorCodes.FORBIDDEN,
					"Bạn không có quyền thực hiện thao tác này", List.of());
			case METHOD_NOT_ALLOWED -> errors.entity(request, status, ErrorCodes.METHOD_NOT_ALLOWED,
					"Phương thức HTTP không được hỗ trợ", List.of());
			case CONTENT_TOO_LARGE -> errors.entity(request, status, ErrorCodes.PAYLOAD_TOO_LARGE,
					"Tệp vượt quá dung lượng cho phép", List.of());
			case BAD_REQUEST -> errors.entity(request, status, ErrorCodes.MALFORMED_REQUEST,
					"Yêu cầu không đúng định dạng", List.of());
			default -> errors.entity(request, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
					"Hệ thống gặp lỗi, vui lòng thử lại sau", List.of());
		};
	}

	private static HttpStatus resolveStatus(HttpServletRequest request) {
		Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		if (code instanceof Integer value) {
			HttpStatus status = HttpStatus.resolve(value);
			if (status != null) {
				return status;
			}
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

}
