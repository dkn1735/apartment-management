package vn.edu.hust.chungcu.auth.phanquyen;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.edu.hust.chungcu.auth.dto.VaiTro;

/**
 * Kiểm {@link ChoPhep} trước khi Spring MVC đọc tham số và body (spec 0006, AC-2). Vai
 * trò chỉ lấy từ authority {@code ROLE_<mã>} của access token, không đọc DB. Exception
 * ném ở đây đi qua {@code GlobalExceptionHandler} như lỗi của controller.
 */
public class ChoPhepInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ChoPhepInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}
		ChoPhep choPhep = handlerMethod.getMethodAnnotation(ChoPhep.class);
		if (choPhep == null) {
			return true;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Set<VaiTro> vaiTro = VaiTroTrongToken.cua(authentication);
		if (Arrays.stream(choPhep.value()).anyMatch(vaiTro::contains)) {
			return true;
		}
		// Không ghi query string, token hay cookie (AC-4); requestId có sẵn trong MDC
		log.warn("Từ chối quyền maNguoiDung={} vaiTro={} method={} path={} canCo={}",
				(authentication != null) ? authentication.getName() : null, noi(vaiTro), request.getMethod(),
				request.getRequestURI(), noi(Arrays.stream(choPhep.value()).sorted().distinct().toList()));
		throw new AccessDeniedException("FORBIDDEN");
	}

	private static String noi(Collection<VaiTro> vaiTro) {
		return vaiTro.stream().map(Enum::name).collect(Collectors.joining(","));
	}

}
