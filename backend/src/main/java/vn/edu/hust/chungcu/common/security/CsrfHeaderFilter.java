package vn.edu.hust.chungcu.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponseFactory;

/**
 * Lớp chống CSRF thứ hai (spec 0004, mục Chống CSRF): request ghi tới {@code /api/**}
 * phải có header {@code X-Requested-With: XMLHttpRequest}. Trang web khác không gửi được
 * header tùy chỉnh vì backend không mở CORS.
 * <p>
 * Tạo bằng {@code new} trong {@link SecurityConfig}, không khai báo là bean: Spring Boot
 * sẽ tự đăng ký thêm một lần nữa thành servlet filter ngoài chuỗi bảo mật.
 */
public final class CsrfHeaderFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Requested-With";

	public static final String EXPECTED = "XMLHttpRequest";

	public static final String MESSAGE = "Yêu cầu bị từ chối vì lý do bảo mật. Vui lòng tải lại trang và thử lại.";

	private static final Set<String> GHI = Set.of("POST", "PUT", "PATCH", "DELETE");

	private final ErrorResponseFactory errors;

	public CsrfHeaderFilter(ErrorResponseFactory errors) {
		this.errors = errors;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (canKiem(request) && !EXPECTED.equals(request.getHeader(HEADER))) {
			this.errors.write(request, response, HttpStatus.FORBIDDEN, ErrorCodes.CSRF_REJECTED, MESSAGE);
			return;
		}
		chain.doFilter(request, response);
	}

	private static boolean canKiem(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return GHI.contains(request.getMethod()) && path.startsWith("/api/");
	}

}
