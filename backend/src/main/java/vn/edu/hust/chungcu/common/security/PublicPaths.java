package vn.edu.hust.chungcu.common.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Danh sách đường công khai dùng chung cho {@code authorizeHttpRequests} và
 * {@link CookieBearerTokenResolver} (spec 0004, mục Token), để cookie cũ hay hỏng không
 * bao giờ làm hỏng một đường công khai. Làm mới và đăng xuất nhận diện phiên bằng cookie
 * {@code refresh_token}, nên access token cũ hay hỏng không được chặn chúng (spec 0005,
 * AC-7).
 */
public final class PublicPaths {

	private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

	/** Luôn công khai. */
	public static final RequestMatcher ALWAYS = new OrRequestMatcher(
			PATHS.matcher(HttpMethod.POST, "/api/v1/auth/login"),
			PATHS.matcher(HttpMethod.POST, "/api/v1/auth/refresh-token"),
			PATHS.matcher(HttpMethod.POST, "/api/v1/auth/logout"), PATHS.matcher("/error"),
			PATHS.matcher("/actuator/health"), PATHS.matcher("/actuator/health/**"));

	/** Swagger: chỉ công khai khi bật {@code springdoc.api-docs.enabled} (dev). */
	public static final RequestMatcher API_DOCS = new OrRequestMatcher(PATHS.matcher("/swagger-ui.html"),
			PATHS.matcher("/swagger-ui/**"), PATHS.matcher("/v3/api-docs"), PATHS.matcher("/v3/api-docs/**"));

	/** Mọi đường không bao giờ đọc access token. */
	public static final RequestMatcher NO_TOKEN = new OrRequestMatcher(ALWAYS, API_DOCS);

	private PublicPaths() {
	}

}
