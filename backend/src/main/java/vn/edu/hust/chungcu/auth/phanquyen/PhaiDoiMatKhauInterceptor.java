package vn.edu.hust.chungcu.auth.phanquyen;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.edu.hust.chungcu.auth.service.AuthErrorCodes;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.security.JwtConfig;

/**
 * Chặn mọi API khi access token mang claim {@code phaiDoiMatKhau} là {@code true} (spec
 * 0008, AC-13), trừ handler {@link CongKhai} và {@link ChoPhepKhiPhaiDoiMatKhau}. Chỉ đọc
 * claim, không tra DB. Chạy trước {@link ChoPhepInterceptor}; request chưa xác thực đi
 * qua để Spring Security trả 401.
 */
public class PhaiDoiMatKhauInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(PhaiDoiMatKhauInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}
		if (handlerMethod.hasMethodAnnotation(CongKhai.class)
				|| handlerMethod.hasMethodAnnotation(ChoPhepKhiPhaiDoiMatKhau.class)) {
			return true;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			return true;
		}
		// Thiếu claim (token cấp trước tính năng này) hoặc không phải boolean thì coi như
		// false
		if (!Boolean.TRUE.equals(jwt.getClaims().get(JwtConfig.CLAIM_PHAI_DOI_MAT_KHAU))) {
			return true;
		}
		log.info("Chặn vì chưa đổi mật khẩu tạm: maNguoiDung={} method={} path={}", jwt.getSubject(),
				request.getMethod(), request.getRequestURI());
		throw new ApiException(HttpStatus.FORBIDDEN, AuthErrorCodes.AUTH_PASSWORD_CHANGE_REQUIRED,
				AuthErrorCodes.AUTH_PASSWORD_CHANGE_REQUIRED_MESSAGE);
	}

}
