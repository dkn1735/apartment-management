package vn.edu.hust.chungcu.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gắn mã {@code X-Request-Id} cho mỗi request: nhận mã từ client nếu hợp lệ (tối đa 64 ký
 * tự, chỉ chữ, số và {@code -}), ngược lại tự sinh. Mã được đưa vào MDC (key
 * {@code requestId}), trả lại trong header và trong response lỗi. Chạy trước Spring
 * Security để cả lỗi 401 và 403 cũng có mã.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";

	public static final String MDC_KEY = "requestId";

	private static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

	private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String incoming = request.getHeader(HEADER);
		String requestId = (incoming != null && VALID.matcher(incoming).matches()) ? incoming : newId();
		request.setAttribute(ATTRIBUTE, requestId);
		response.setHeader(HEADER, requestId);
		MDC.put(MDC_KEY, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(MDC_KEY);
		}
	}

	/** Mã của request hiện tại; đọc được cả trong lượt xử lý lỗi ({@code /error}). */
	public static String currentRequestId(HttpServletRequest request) {
		Object value = request.getAttribute(ATTRIBUTE);
		if (value instanceof String id) {
			return id;
		}
		String fromMdc = MDC.get(MDC_KEY);
		return fromMdc != null ? fromMdc : newId();
	}

	private static String newId() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

}
