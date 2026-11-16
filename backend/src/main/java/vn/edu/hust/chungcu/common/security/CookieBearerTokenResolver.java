package vn.edu.hust.chungcu.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Chỉ đọc access token từ cookie {@value #ACCESS_TOKEN_COOKIE}, bỏ qua header
 * {@code Authorization} (spec 0004, AC-7). Trả {@code null} cho mọi đường công khai, để
 * cookie hết hạn hay hỏng không chặn được đăng nhập.
 */
public final class CookieBearerTokenResolver implements BearerTokenResolver {

	public static final String ACCESS_TOKEN_COOKIE = "access_token";

	private final RequestMatcher skip;

	public CookieBearerTokenResolver(RequestMatcher skip) {
		this.skip = skip;
	}

	@Override
	public String resolve(HttpServletRequest request) {
		if (this.skip.matches(request)) {
			return null;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
				String value = cookie.getValue();
				return (value == null || value.isBlank()) ? null : value;
			}
		}
		return null;
	}

}
