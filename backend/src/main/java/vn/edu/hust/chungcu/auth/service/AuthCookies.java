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

	public AuthCookies(AppProperties properties) {
		this.secure = properties.security().cookieSecure();
		this.accessTtl = properties.security().accessTokenTtl();
	}

	/**
	 * @param refreshMaxAge {@code Max-Age} của {@code refresh_token} và {@code phien}:
	 * bằng {@code refresh-token-ttl} khi đăng nhập, bằng thời gian còn lại tới
	 * {@code het_han_luc} khi làm mới (spec 0005, AC-2)
	 */
	public HttpHeaders dat(String accessToken, String refreshToken, Duration refreshMaxAge) {
		HttpHeaders headers = new HttpHeaders();
		add(headers, ACCESS_TOKEN, accessToken, PATH_GOC, this.accessTtl);
		add(headers, REFRESH_TOKEN, refreshToken, PATH_REFRESH, refreshMaxAge);
		add(headers, PHIEN, "1", PATH_GOC, refreshMaxAge);
		return headers;
	}

	/**
	 * Chỉ đặt lại cookie {@code access_token}, giữ nguyên {@code refresh_token} và
	 * {@code phien} (spec 0008, AC-13: đổi mật khẩu cấp access token mới).
	 */
	public HttpHeaders datAccessToken(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		add(headers, ACCESS_TOKEN, accessToken, PATH_GOC, this.accessTtl);
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
