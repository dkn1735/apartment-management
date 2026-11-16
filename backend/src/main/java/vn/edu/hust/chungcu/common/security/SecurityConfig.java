package vn.edu.hust.chungcu.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponseFactory;

/**
 * Bảo mật TẠM THỜI của bộ khung (spec 0001, mục Nền tảng khác), dùng tới khi spec Đăng
 * nhập (tính năng 5) chốt cách cấp và kiểm tra token, cookie và CSRF. Cho qua health,
 * Swagger (chỉ khi bật ở dev) và {@code /api/v1/auth/**}; mọi đường còn lại trả 401
 * {@code UNAUTHENTICATED} theo vỏ bọc lỗi.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ErrorResponseFactory errors,
			@Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled) throws Exception {
		http
			// CSRF tạm tắt vì chưa có phiên; spec Đăng nhập chốt cách chống CSRF khi dùng
			// cookie
			.csrf(csrf -> csrf.disable())
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> {
				auth.requestMatchers("/error", "/actuator/health", "/actuator/health/**").permitAll();
				auth.requestMatchers("/api/v1/auth/**").permitAll();
				if (apiDocsEnabled) {
					auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
						.permitAll();
				}
				auth.anyRequest().authenticated();
			})
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((request, response, e) -> errors.write(request, response,
						HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED, "Bạn cần đăng nhập để tiếp tục"))
				.accessDeniedHandler((request, response, e) -> errors.write(request, response, HttpStatus.FORBIDDEN,
						ErrorCodes.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này")));
		return http.build();
	}

}
