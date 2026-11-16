package vn.edu.hust.chungcu.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponseFactory;

/**
 * Chuỗi bảo mật (spec 0004): access token JWT đọc từ cookie, đường công khai ở
 * {@link PublicPaths}, chống CSRF bằng cookie {@code SameSite=Strict} cộng
 * {@link CsrfHeaderFilter}. Mọi lỗi 401 và 403, kể cả token hỏng hay hết hạn, theo vỏ bọc
 * lỗi chung.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	static final String UNAUTHENTICATED_MESSAGE = "Bạn cần đăng nhập để tiếp tục";

	static final String FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện thao tác này";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ErrorResponseFactory errors, JwtDecoder jwtDecoder,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			@Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled) throws Exception {
		AuthenticationEntryPoint entryPoint = (request, response, e) -> errors.write(request, response,
				HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED, UNAUTHENTICATED_MESSAGE);
		AccessDeniedHandler accessDenied = (request, response, e) -> errors.write(request, response,
				HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, FORBIDDEN_MESSAGE);
		http
			// Không dùng token CSRF của Spring; chống CSRF bằng SameSite=Strict và
			// CsrfHeaderFilter
			.csrf(csrf -> csrf.disable())
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> {
				auth.requestMatchers(PublicPaths.ALWAYS).permitAll();
				if (apiDocsEnabled) {
					auth.requestMatchers(PublicPaths.API_DOCS).permitAll();
				}
				auth.anyRequest().authenticated();
			})
			.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDenied))
			// Bộ lọc token có entry point riêng (401 body rỗng). Truyền cùng entry point,
			// để token hỏng hay hết hạn cũng trả UNAUTHENTICATED theo vỏ bọc
			.oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDenied)
				.bearerTokenResolver(new CookieBearerTokenResolver(PublicPaths.NO_TOKEN))
				.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)))
			.addFilterBefore(new CsrfHeaderFilter(errors), BearerTokenAuthenticationFilter.class);
		return http.build();
	}

}
