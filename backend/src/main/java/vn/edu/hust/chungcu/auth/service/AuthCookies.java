package vn.edu.hust.chungcu.auth.service;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.security.CookieBearerTokenResolver;

/**
 * Đặt và xóa ba cookie xác thực (spec 0004, mục Cookie). Mọi cookie {@code HttpOnly},
 * {@code SameSite=Strict}, {@code Secure} theo {@code app.security.cookie-secure}.
 */
@Component
public class AuthCookies {

	public static final String ACCESS_TOKEN = CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE;

	public static final String REFRESH_TOKEN = "refresh_token";

	/** Dấu có phiên cho {@code proxy.ts}; không chứa bí mật. */
	public static final String PHIEN = "phien";

	static final String PATH_GOC = "/";

	static final String PATH_REFRESH = "/api/v1/auth";

	private final boolean secure;

	private final Duration accessTtl;

	private final Duration refreshTtl;

	public AuthCookies(AppProperties properties) {
		this.secure = properties.security().cookieSecure();
		this.accessTtl = properties.security().accessTokenTtl();
		this.refreshTtl = properties.security().refreshTokenTtl();
	}

	public HttpHeaders dat(String accessToken, String refreshToken) {
		HttpHeaders headers = new HttpHeaders();
		add(headers, ACCESS_TOKEN, accessToken, PATH_GOC, this.accessTtl);
		add(headers, REFRESH_TOKEN, refreshToken, PATH_REFRESH, this.refreshTtl);
		add(headers, PHIEN, "1", PATH_GOC, this.refreshTtl);
		return headers;
	}

	public HttpHeaders xoa() {
		HttpHeaders headers = new HttpHeaders();
		add(headers, ACCESS_TOKEN, "", PATH_GOC, Duration.ZERO);
		add(headers, REFRESH_TOKEN, "", PATH_REFRESH, Duration.ZERO);
		add(headers, PHIEN, "", PATH_GOC, Duration.ZERO);
		return headers;
	}

	private void add(HttpHeaders headers, String name, String value, String path, Duration maxAge) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(this.secure)
			.sameSite("Strict")
			.path(path)
			.maxAge(maxAge)
			.build();
		headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
	}

}
