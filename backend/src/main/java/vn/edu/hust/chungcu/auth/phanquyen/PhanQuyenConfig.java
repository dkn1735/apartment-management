package vn.edu.hust.chungcu.auth.phanquyen;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Đăng ký {@link PhaiDoiMatKhauInterceptor} (spec 0008) rồi {@link ChoPhepInterceptor}
 * (spec 0006) cho mọi API, đúng thứ tự đó.
 */
@Configuration(proxyBeanMethods = false)
public class PhanQuyenConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new PhaiDoiMatKhauInterceptor()).addPathPatterns("/api/**").order(0);
		registry.addInterceptor(new ChoPhepInterceptor()).addPathPatterns("/api/**").order(1);
	}

}
